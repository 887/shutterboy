package com.eight87.shutterboy.data.saf

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.eight87.shutterboy.data.scan.MediaStoreScanner
import com.eight87.shutterboy.data.scan.ScannedPhoto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.absoluteValue

/**
 * Phase B.4 — multi-source library via Storage Access Framework.
 *
 * Companion to [MediaStoreScanner]: this surface walks user-picked SAF tree
 * URIs and returns image-typed [ScannedPhoto]s with `source = SAF_TREE`. The
 * persisted-tree-URIs list comes from
 * [com.eight87.shutterboy.data.settings.ScanConfigSource]; the actual
 * ACTION_OPEN_DOCUMENT_TREE intent + per-tree `takePersistableUriPermission`
 * call live in the UI layer (Phase I.3 settings → Manage sources).
 *
 * The repo composes [scanSafTrees] with [MediaStoreScanner.scanDeviceMediaStore]
 * to produce the unified library.
 */
class SafSourceManager(
    private val context: Context,
) {
    /**
     * Walk every supplied SAF tree URI and return image-typed scanned photos.
     * Trees that are no longer permission-held (revoked) silently produce 0
     * photos for that tree; the repo surfaces revocation via the
     * `Folder.sourceType=SAF` row's count going to 0.
     */
    suspend fun scanSafTrees(treeUris: Set<String>): SafScanResult = withContext(Dispatchers.IO) {
        if (treeUris.isEmpty()) return@withContext SafScanResult.Empty
        val photos = mutableListOf<ScannedPhoto>()
        val foldersById = LinkedHashMap<Long, MediaStoreScanner.ScannedFolder>()
        // R.F.26 — collect URIs whose `DocumentFile.fromTreeUri(...)` returns
        // null, or whose tree can't be read (`canRead() == false`). The
        // caller prunes these from the persisted set so the next observation
        // surfaces a clean inventory.
        val revoked = mutableSetOf<String>()
        for (treeUriStr in treeUris) {
            val treeUri = runCatching { Uri.parse(treeUriStr) }.getOrNull()
            if (treeUri == null) {
                revoked += treeUriStr
                continue
            }
            val tree = DocumentFile.fromTreeUri(context, treeUri)
            if (tree == null || !tree.canRead()) {
                revoked += treeUriStr
                continue
            }
            val rootName = tree.name ?: "SAF source"
            val rootId = stableFolderId(treeUriStr)
            val rooted = walkTreeForImages(tree, rootId, rootName, treeUri, photos)
            if (rooted) {
                foldersById.getOrPut(rootId) {
                    MediaStoreScanner.ScannedFolder(
                        id = rootId,
                        displayName = rootName,
                        source = ScannedPhoto.ScanSource.SAF_TREE,
                        safTreeUri = treeUri,
                    )
                }
            }
        }
        SafScanResult(
            photos = photos,
            folders = foldersById.values.toList(),
            revokedUris = revoked,
        )
    }

    /**
     * Phase incremental-scan A.3 — cheap fingerprint per tree. Recursive
     * count of image-typed leaves, no dimension reads, no EXIF, no
     * `ScannedPhoto` allocation. Persisted alongside the MediaStore
     * generation token so the cold-start gate can ask "did any SAF tree
     * change?" without doing a full scan. Trees that are no longer
     * permission-held count as 0.
     */
    suspend fun fingerprint(treeUris: Set<String>): Map<String, Int> = withContext(Dispatchers.IO) {
        if (treeUris.isEmpty()) return@withContext emptyMap<String, Int>()
        val out = LinkedHashMap<String, Int>(treeUris.size)
        for (treeUriStr in treeUris) {
            val treeUri = runCatching { Uri.parse(treeUriStr) }.getOrNull()
            val tree = treeUri?.let { DocumentFile.fromTreeUri(context, it) }
            out[treeUriStr] = if (tree == null) 0 else countImageLeaves(tree)
        }
        out
    }

    private fun countImageLeaves(node: DocumentFile): Int {
        var n = 0
        for (child in node.listFiles()) {
            n += when {
                child.isDirectory -> countImageLeaves(child)
                child.isFile && (child.type ?: "").startsWith("image/") -> 1
                else -> 0
            }
        }
        return n
    }

    /**
     * Walk a [DocumentFile] subtree recursively, appending image-typed leaves
     * to [out]. Returns true if at least one image was found (so the caller
     * knows whether to register a folder row for this tree).
     */
    private fun walkTreeForImages(
        node: DocumentFile,
        folderId: Long,
        folderName: String,
        treeUri: Uri,
        out: MutableList<ScannedPhoto>,
    ): Boolean {
        var found = false
        for (child in node.listFiles()) {
            when {
                child.isDirectory ->
                    if (walkTreeForImages(child, folderId, folderName, treeUri, out)) found = true

                child.isFile && (child.type ?: "").startsWith("image/") -> {
                    out += scannedPhotoFromDocumentFile(
                        doc = child,
                        folderId = folderId,
                        folderName = folderName,
                        treeUri = treeUri,
                    )
                    found = true
                }
            }
        }
        return found
    }

    private fun scannedPhotoFromDocumentFile(
        doc: DocumentFile,
        folderId: Long,
        folderName: String,
        treeUri: Uri,
    ): ScannedPhoto {
        val uri = doc.uri
        val (w, h) = readImageDimensions(context.contentResolver, uri)
        // No DATE_TAKEN from SAF; fall back to lastModified for both fields. EXIF
        // pass (B.3) overrides exif* fields if present in the file.
        val mtime = doc.lastModified()
        return ScannedPhoto(
            id = uri.hashCode().toLong().absoluteValue,
            contentUri = uri,
            displayName = doc.name ?: "saf_${doc.uri.lastPathSegment ?: ""}",
            dateTakenMs = mtime,
            dateAddedMs = mtime,
            width = w,
            height = h,
            sizeBytes = doc.length(),
            mimeType = doc.type ?: "image/*",
            folderId = folderId,
            folderDisplayName = folderName,
            source = ScannedPhoto.ScanSource.SAF_TREE,
        )
    }

    /**
     * Open the file just enough to read intrinsic dimensions without decoding
     * the full bitmap. Falls back to (0, 0) on failure; the EXIF pass may
     * override these if the EXIF block carries `PixelXDimension` /
     * `PixelYDimension`.
     */
    private fun readImageDimensions(resolver: ContentResolver, uri: Uri): Pair<Int, Int> {
        return runCatching {
            resolver.openInputStream(uri)?.use { stream ->
                val opts = android.graphics.BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                android.graphics.BitmapFactory.decodeStream(stream, null, opts)
                opts.outWidth to opts.outHeight
            }
        }.getOrNull() ?: (0 to 0)
    }

    /**
     * Stable, deterministic id for a SAF tree URI so the same source always
     * maps to the same `FolderEntity.id` across rescans. Uses a hash; we avoid
     * collisions with MediaStore bucket ids (which are positive `Long`s much
     * smaller than [Long.MAX_VALUE]) by setting the high bit.
     */
    private fun stableFolderId(treeUriStr: String): Long =
        Long.MIN_VALUE or treeUriStr.hashCode().toLong().absoluteValue

    data class SafScanResult(
        val photos: List<ScannedPhoto>,
        val folders: List<MediaStoreScanner.ScannedFolder>,
        val revokedUris: Set<String> = emptySet(),
    ) {
        companion object {
            val Empty = SafScanResult(emptyList(), emptyList())
        }
    }
}
