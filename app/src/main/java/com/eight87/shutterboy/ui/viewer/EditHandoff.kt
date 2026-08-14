package com.eight87.shutterboy.ui.viewer

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.widget.Toast
import com.eight87.shutterboy.R

/**
 * Hand a photo off to an external editor via `ACTION_EDIT`.
 *
 * Two things here are load-bearing and were both wrong before:
 *
 * 1. **No `FLAG_GRANT_WRITE_URI_PERMISSION`.** A `content://media/...`
 *    URI belongs to MediaStore, not to us — we merely hold
 *    `READ_MEDIA_IMAGES`. An app can only *grant* URI permissions it
 *    itself received as a grant, so asking the system to pass a write
 *    grant along threw
 *    `SecurityException: UID <n> does not have permission to
 *    content://media/external/images/media/<id>` from
 *    `UriGrantsManagerService` at `startActivity` time, killing the
 *    process the instant the user tapped Edit. Read-grant is fine
 *    (that's why Share always worked). Editors that want to write back
 *    request their own access — that's their job, not ours.
 *
 * 2. **We exclude ourselves from the chooser.** Shutterboy declares an
 *    `ACTION_EDIT` intent-filter (so it shows up when a camera app
 *    offers "edit"), but it ships no editor — leaving it in its own
 *    edit chooser just loops the user back into the viewer.
 *
 * `EXTRA_OUTPUT` + the `mimeType` extra are the de-facto convention
 * editors read to know where to write the result; harmless to editors
 * that ignore them.
 */
fun launchEditChooser(context: Context, uri: Uri, mimeType: String?) {
    val edit = Intent(Intent.ACTION_EDIT).apply {
        setDataAndType(uri, mimeType)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        putExtra(MediaStore.EXTRA_OUTPUT, uri)
        putExtra("mimeType", mimeType)
    }
    val self = ComponentName(context, "com.eight87.shutterboy.MainActivity")
    // An empty chooser opens as a blank sheet rather than throwing, so the
    // "nothing here can edit photos" case has to be detected up front —
    // otherwise tapping Edit looks like it did nothing at all.
    val editors = context.packageManager
        .queryIntentActivities(edit, 0)
        .filterNot { it.activityInfo.packageName == context.packageName }
    if (editors.isEmpty()) {
        toastNoEditor(context)
        return
    }
    val chooser = Intent.createChooser(
        edit,
        context.getString(R.string.viewer_edit_chooser_title),
    ).apply {
        putExtra(Intent.EXTRA_EXCLUDE_COMPONENTS, arrayOf(self))
    }
    try {
        context.startActivity(chooser)
    } catch (_: ActivityNotFoundException) {
        toastNoEditor(context)
    }
}

private fun toastNoEditor(context: Context) {
    Toast.makeText(
        context,
        context.getString(R.string.viewer_edit_no_app),
        Toast.LENGTH_SHORT,
    ).show()
}
