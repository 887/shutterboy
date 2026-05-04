package com.eight87.shutterboy.domain

import android.net.Uri

data class Folder(
    val id: FolderId,
    val displayName: String,
    val sourceType: SourceType,
    val safTreeUri: Uri? = null,
    val photoCount: Int = 0,
    val coverPhotoId: PhotoId? = null,
)

@JvmInline
value class FolderId(val value: Long)

enum class SourceType { DEVICE, SAF }
