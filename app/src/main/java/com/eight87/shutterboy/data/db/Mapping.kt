package com.eight87.shutterboy.data.db

import android.net.Uri
import com.eight87.shutterboy.data.scan.ScannedPhoto
import com.eight87.shutterboy.domain.Folder
import com.eight87.shutterboy.domain.FolderId
import com.eight87.shutterboy.domain.Photo
import com.eight87.shutterboy.domain.PhotoId
import com.eight87.shutterboy.domain.SourceType

/**
 * The data-layer-boundary mapping. UI consumers receive [Photo] / [Folder] from
 * the repository; they never see Room entities. The scanner produces
 * [ScannedPhoto] which maps to [PhotoEntity] for persistence.
 */
internal fun PhotoEntity.toDomain(): Photo = Photo(
    id = PhotoId(id),
    contentUri = Uri.parse(contentUri),
    displayName = displayName,
    dateTakenMs = dateTakenMs,
    dateAddedMs = dateAddedMs,
    width = width,
    height = height,
    sizeBytes = sizeBytes,
    mimeType = mimeType,
    folderId = FolderId(folderId),
    exifLensModel = exifLensModel,
    exifFocalLength = exifFocalLength,
    exifIso = exifIso,
    exifAperture = exifAperture,
    exifShutterSpeedSec = exifShutterSpeedSec,
    latitude = latitude,
    longitude = longitude,
)

internal fun FolderEntity.toDomain(): Folder = Folder(
    id = FolderId(id),
    displayName = displayName,
    sourceType = runCatching { SourceType.valueOf(sourceType) }.getOrDefault(SourceType.DEVICE),
    safTreeUri = safTreeUri?.let(Uri::parse),
    photoCount = photoCount,
    coverPhotoId = coverPhotoId?.let(::PhotoId),
)

internal fun ScannedPhoto.toEntity(): PhotoEntity = PhotoEntity(
    id = id,
    contentUri = contentUri.toString(),
    displayName = displayName,
    dateTakenMs = dateTakenMs,
    dateAddedMs = dateAddedMs,
    width = width,
    height = height,
    sizeBytes = sizeBytes,
    mimeType = mimeType,
    folderId = folderId,
    exifLensModel = exifLensModel,
    exifFocalLength = exifFocalLength,
    exifIso = exifIso,
    exifAperture = exifAperture,
    exifShutterSpeedSec = exifShutterSpeedSec,
    latitude = latitude,
    longitude = longitude,
)
