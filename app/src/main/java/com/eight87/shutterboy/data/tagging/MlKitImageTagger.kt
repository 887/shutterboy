package com.eight87.shutterboy.data.tagging

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.tasks.Task
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.genai.common.DownloadCallback
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.common.GenAiException
import com.google.mlkit.genai.imagedescription.ImageDescriber
import com.google.mlkit.genai.imagedescription.ImageDescriberOptions
import com.google.mlkit.genai.imagedescription.ImageDescription
import com.google.mlkit.genai.imagedescription.ImageDescriptionRequest
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Production [ImageTagger] over ML Kit.
 *
 * - **Labels** come from Image Labeling (`play-services-mlkit-image-labeling`).
 *   The model lives in Play Services and downloads on demand — labeling is
 *   "available" iff Google Play Services is present.
 * - **Captions** come from GenAI Image Description (AICore / Gemini Nano).
 *   Available only on AICore-capable devices; a DOWNLOADABLE model is fetched
 *   in the background and captioning is skipped (not blocked) until it's ready.
 *
 * Everything is wrapped so a missing dependency, absent Play Services / AICore,
 * or a model rejection degrades to "unavailable" / empty rather than crashing.
 */
class MlKitImageTagger(
    private val context: Context,
) : ImageTagger {

    private val labeler by lazy {
        ImageLabeling.getClient(
            ImageLabelerOptions.Builder()
                .setConfidenceThreshold(LABEL_CONFIDENCE_THRESHOLD)
                .build(),
        )
    }

    @Volatile private var describer: ImageDescriber? = null

    private fun describerOrNull(): ImageDescriber? {
        describer?.let { return it }
        return runCatching {
            ImageDescription.getClient(ImageDescriberOptions.builder(context).build())
        }.getOrNull()?.also { describer = it }
    }

    override suspend fun availability(): TaggerAvailability {
        val labels = runCatching {
            GoogleApiAvailability.getInstance()
                .isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS
        }.getOrDefault(false)

        val captions = runCatching {
            val d = describerOrNull() ?: return@runCatching false
            val status = d.checkFeatureStatus().awaitFuture()
            status != FeatureStatus.UNAVAILABLE
        }.getOrDefault(false)

        return TaggerAvailability(labelsAvailable = labels, captionsAvailable = captions)
    }

    override suspend fun analyze(bitmap: Bitmap, wantCaption: Boolean): List<TagCandidate> {
        val out = ArrayList<TagCandidate>()

        runCatching {
            val labels = labeler.process(InputImage.fromBitmap(bitmap, 0)).await()
            for (label in labels) {
                out.add(TagCandidate(label.text, label.confidence, TagSource.LABEL))
            }
        }.onFailure { Log.d(TAG, "labeling failed: ${it.message}") }

        if (wantCaption) {
            runCatching { caption(bitmap)?.let(out::add) }
                .onFailure { Log.d(TAG, "captioning failed: ${it.message}") }
        }

        return out
    }

    private suspend fun caption(bitmap: Bitmap): TagCandidate? {
        val d = describerOrNull() ?: return null
        when (d.checkFeatureStatus().awaitFuture()) {
            FeatureStatus.AVAILABLE -> {
                val result = d.runInference(ImageDescriptionRequest.builder(bitmap).build()).awaitFuture()
                val text = result.description.trim()
                return if (text.isEmpty()) null else TagCandidate(text, 1f, TagSource.CAPTION)
            }
            FeatureStatus.DOWNLOADABLE -> {
                // Kick the download but don't block this pass on it; the next
                // pass picks captioning up once the model is resident.
                runCatching { d.downloadFeature(noOpDownloadCallback()) }
                return null
            }
            else -> return null // DOWNLOADING or UNAVAILABLE
        }
    }

    override fun close() {
        runCatching { describer?.close() }
        describer = null
    }

    private fun noOpDownloadCallback() = object : DownloadCallback {
        override fun onDownloadStarted(bytesToDownload: Long) = Unit
        override fun onDownloadFailed(e: GenAiException) = Unit
        override fun onDownloadProgress(totalBytesDownloaded: Long) = Unit
        override fun onDownloadCompleted() = Unit
    }

    private companion object {
        const val TAG = "shutterboy.tagging"
        const val LABEL_CONFIDENCE_THRESHOLD = 0.6f
    }
}

/** Bridge a GMS [Task] (image labeling) into a suspend call without the extra coroutines-play-services dep. */
private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { cont.resume(it) }
    addOnFailureListener { cont.resumeWithException(it) }
    addOnCanceledListener { cont.cancel() }
}

/** Bridge a Guava [ListenableFuture] (GenAI image description) into a suspend call. */
private suspend fun <T> ListenableFuture<T>.awaitFuture(): T = suspendCancellableCoroutine { cont ->
    val direct = Executor { it.run() }
    addListener(
        {
            try {
                cont.resume(get())
            } catch (e: Throwable) {
                cont.resumeWithException(e)
            }
        },
        direct,
    )
    cont.invokeOnCancellation { cancel(true) }
}
