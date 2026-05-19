package com.eight87.shutterboy.ui.multiselect

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.eight87.shutterboy.data.repo.PhotoDeleter
import com.eight87.shutterboy.domain.DeleteRequest
import com.eight87.shutterboy.domain.PhotoId
import kotlinx.coroutines.launch

/**
 * Phase H.3 — composable that wires the typed-confirm dialog + system consent
 * launcher + `PhotoDeleter` dispatch into a single `request()` callback.
 *
 * Shared across the three grid surfaces (Photos / FolderDetail /
 * SmartAlbumDetail) so each screen only needs a single line:
 *
 * ```kotlin
 * val delete = rememberSelectionDeleteHandler(scope.photoDeleter) {
 *   selectionHolder.exit()
 * }
 * delete.Render()   // renders the typed-confirm dialog when active
 * ...
 * onDelete = { delete.request(selectionHolder.selectedIds()) }
 * ```
 */
class SelectionDeleteHandler internal constructor(
    private val request: (Set<PhotoId>) -> Unit,
    private val dialogState: () -> DialogState?,
    private val confirm: () -> Unit,
    private val dismiss: () -> Unit,
) {
    fun request(ids: Set<PhotoId>) = request.invoke(ids)

    @Composable
    fun Render() {
        val state = dialogState() ?: return
        TypedConfirmDeleteDialog(
            count = state.count,
            onConfirm = confirm,
            onDismiss = dismiss,
        )
    }

    internal data class DialogState(val ids: Set<PhotoId>, val count: Int)
}

@Composable
fun rememberSelectionDeleteHandler(
    photoDeleter: PhotoDeleter,
    onComplete: () -> Unit,
): SelectionDeleteHandler {
    val coroutineScope = rememberCoroutineScope()
    var pending by remember { mutableStateOf<SelectionDeleteHandler.DialogState?>(null) }
    // Remember the in-flight ids so the consent-launcher callback can
    // eagerly drop them from the Room cache on success — Aves-style
    // instant removal, no waiting for the MediaStore-observer rescan.
    var inFlightIds by remember { mutableStateOf<List<PhotoId>>(emptyList()) }

    val consentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        // RESULT_OK == -1; on success exit selection. Otherwise leave selection
        // intact so the user can retry / cancel manually.
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val ids = inFlightIds
            inFlightIds = emptyList()
            coroutineScope.launch {
                photoDeleter.eagerlyRemoveFromCache(ids)
            }
            onComplete()
        } else {
            inFlightIds = emptyList()
        }
    }

    return remember(photoDeleter, consentLauncher) {
        SelectionDeleteHandler(
            request = { ids ->
                if (ids.isNotEmpty()) {
                    pending = SelectionDeleteHandler.DialogState(ids, ids.size)
                }
            },
            dialogState = { pending },
            confirm = {
                val state = pending ?: return@SelectionDeleteHandler
                pending = null
                coroutineScope.launch {
                    val idList = state.ids.toList()
                    when (val req = photoDeleter.deletePhotos(idList)) {
                        is DeleteRequest.Immediate -> {
                            photoDeleter.eagerlyRemoveFromCache(idList)
                            onComplete()
                        }
                        is DeleteRequest.Consent -> {
                            inFlightIds = idList
                            val isr = IntentSenderRequest.Builder(req.intentSender.intentSender)
                                .build()
                            consentLauncher.launch(isr)
                        }
                        is DeleteRequest.Failure -> {
                            // Leave selection intact; nothing else to do here in v1.
                        }
                    }
                }
            },
            dismiss = { pending = null },
        )
    }
}
