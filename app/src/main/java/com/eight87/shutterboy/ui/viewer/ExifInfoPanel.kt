package com.eight87.shutterboy.ui.viewer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.eight87.shutterboy.R
import com.eight87.shutterboy.domain.Photo
import com.eight87.shutterboy.domain.PhotoTag
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Phase F.3 — EXIF info bottom sheet. Reads cached EXIF values from the
 * [Photo] domain row (already enriched at scan time, B.3); no fresh file
 * I/O on the viewer hot path.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ExifInfoPanel(
    photo: Photo,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    tags: List<PhotoTag> = emptyList(),
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.viewer_info_title),
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(Modifier.width(0.dp))
            InfoRow(stringResource(R.string.viewer_info_filename), photo.displayName)
            formatCaptureDate(photo.dateTakenMs)?.let {
                InfoRow(stringResource(R.string.viewer_info_capture_date), it)
            }
            ExifPanelFormatters.formatDimensions(photo.width, photo.height)?.let {
                val mp = ExifPanelFormatters.formatMegapixels(photo.width, photo.height)
                val value = if (mp != null) "$it ($mp)" else it
                InfoRow(stringResource(R.string.viewer_info_dimensions), value)
            }
            ExifPanelFormatters.formatFileSize(photo.sizeBytes)?.let {
                InfoRow(stringResource(R.string.viewer_info_file_size), it)
            }
            photo.exifLensModel?.takeIf { it.isNotBlank() }?.let {
                InfoRow(stringResource(R.string.viewer_info_lens), it)
            }
            ExifPanelFormatters.formatFocalLength(photo.exifFocalLength)?.let {
                InfoRow(stringResource(R.string.viewer_info_focal_length), it)
            }
            ExifPanelFormatters.formatIso(photo.exifIso)?.let {
                InfoRow(stringResource(R.string.viewer_info_iso), it)
            }
            ExifPanelFormatters.formatAperture(photo.exifAperture)?.let {
                InfoRow(stringResource(R.string.viewer_info_aperture), it)
            }
            ExifPanelFormatters.formatShutterSpeed(photo.exifShutterSpeedSec)?.let {
                InfoRow(stringResource(R.string.viewer_info_shutter_speed), it)
            }

            // Smart Tags — GenAI caption (if any) as a sentence, keyword
            // labels as chips below it.
            val caption = tags.firstOrNull { it.isCaption }?.label?.takeIf { it.isNotBlank() }
            val labels = tags.filter { !it.isCaption && it.label.isNotBlank() }
            if (caption != null) {
                InfoRow(stringResource(R.string.viewer_info_description), caption)
            }
            if (labels.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.viewer_info_tags),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 4.dp),
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    labels.forEach { tag ->
                        SuggestionChip(
                            onClick = {},
                            label = { Text(tag.label) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = androidx.compose.ui.Alignment.Top,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(132.dp),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private fun formatCaptureDate(epochMs: Long): String? {
    if (epochMs <= 0L) return null
    return runCatching {
        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
            .withZone(ZoneId.systemDefault())
            .format(Instant.ofEpochMilli(epochMs))
    }.getOrNull()
}
