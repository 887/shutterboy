package com.eight87.shutterboy.ui.multiselect

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eight87.shutterboy.R
import com.eight87.shutterboy.data.repo.FolderSource
import com.eight87.shutterboy.data.repo.PhotoMover
import com.eight87.shutterboy.domain.Folder
import com.eight87.shutterboy.domain.FolderId
import com.eight87.shutterboy.domain.MoveRequest
import com.eight87.shutterboy.domain.PhotoId
import kotlinx.coroutines.launch

/**
 * Phase H.4 — composable that wires the folder-picker dialog + system
 * consent launcher + [PhotoMover] dispatch into a single `request()`
 * callback. Mirrors [SelectionDeleteHandler]'s shape.
 *
 * Usage:
 * ```kotlin
 * val move = rememberSelectionMoveHandler(
 *     photoMover = scope.photoMover,
 *     folderSource = scope.folderSource,
 *     snackbar = scope.snackbar,
 *     sourceFolderId = currentFolderId,   // null for Photos / SmartAlbum
 * ) { selectionHolder.exit() }
 * move.Render()
 * // ...
 * onMove = { move.request(selectionHolder.selectedIds()) }
 * ```
 */
class SelectionMoveHandler internal constructor(
    private val request: (Set<PhotoId>) -> Unit,
    private val renderContent: @Composable () -> Unit,
) {
    fun request(ids: Set<PhotoId>) = request.invoke(ids)

    @Composable
    fun Render() {
        renderContent()
    }
}

/**
 * Derive the MediaStore RELATIVE_PATH for a destination folder. We don't
 * have the original relative path cached in the [Folder] row, but for
 * device-sourced (MediaStore) folders the convention is
 * `Pictures/<displayName>/`. SAF-sourced folders are out of scope for v1.
 */
internal fun relativePathFor(folder: Folder): String =
    "Pictures/${folder.displayName.trim('/')}/"

@Composable
fun rememberSelectionMoveHandler(
    photoMover: PhotoMover,
    folderSource: FolderSource,
    snackbar: SnackbarHostState,
    sourceFolderId: FolderId? = null,
    onComplete: () -> Unit,
): SelectionMoveHandler {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var pendingIds by remember { mutableStateOf<Set<PhotoId>?>(null) }
    var inFlightTarget by remember { mutableStateOf<Folder?>(null) }

    fun successMsg(count: Int, folder: String) =
        context.getString(R.string.multiselect_move_success, count, folder)
    val cancelledMsg = context.getString(R.string.multiselect_move_cancelled)
    fun failedMsg(reason: String) =
        context.getString(R.string.multiselect_move_failed, reason)

    val allFolders by folderSource.observeFolders()
        .collectAsStateWithLifecycle(initialValue = emptyList<Folder>())

    val consentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        val target = inFlightTarget
        inFlightTarget = null
        if (result.resultCode == android.app.Activity.RESULT_OK && target != null) {
            coroutineScope.launch {
                val moved = runCatching { photoMover.applyMoveAfterConsent() }
                    .getOrElse { 0 }
                if (moved > 0) {
                    snackbar.showSnackbar(successMsg(moved, target.displayName))
                    onComplete()
                } else {
                    snackbar.showSnackbar(cancelledMsg)
                }
            }
        } else {
            coroutineScope.launch {
                snackbar.showSnackbar(cancelledMsg)
            }
        }
    }

    val renderContent: @Composable () -> Unit = remember(allFolders, sourceFolderId) {
        @Composable {
            val ids = pendingIds
            if (ids != null) {
                val targets = remember(allFolders, sourceFolderId) {
                    filterMoveTargets(allFolders, sourceFolderId)
                }
                FolderPickerDialog(
                    folders = targets,
                    onDismiss = { pendingIds = null },
                    onPick = { folder ->
                        val capturedIds = ids
                        pendingIds = null
                        inFlightTarget = folder
                        coroutineScope.launch {
                            val target = relativePathFor(folder)
                            when (val req = photoMover.moveToFolder(
                                capturedIds.toList(),
                                target,
                            )) {
                                is MoveRequest.Direct -> {
                                    inFlightTarget = null
                                    if (req.movedCount > 0) {
                                        snackbar.showSnackbar(
                                            successMsg(req.movedCount, folder.displayName),
                                        )
                                        onComplete()
                                    } else {
                                        snackbar.showSnackbar(cancelledMsg)
                                    }
                                }
                                is MoveRequest.Consent -> {
                                    val isr = IntentSenderRequest.Builder(
                                        req.intentSender.intentSender,
                                    ).build()
                                    consentLauncher.launch(isr)
                                }
                                is MoveRequest.Failure -> {
                                    inFlightTarget = null
                                    snackbar.showSnackbar(failedMsg(req.reason))
                                }
                            }
                        }
                    },
                )
            }
        }
    }

    return remember(photoMover) {
        SelectionMoveHandler(
            request = { ids -> if (ids.isNotEmpty()) pendingIds = ids },
            renderContent = renderContent,
        )
    }
}

