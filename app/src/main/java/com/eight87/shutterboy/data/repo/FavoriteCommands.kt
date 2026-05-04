package com.eight87.shutterboy.data.repo

import com.eight87.shutterboy.domain.PhotoId

interface FavoriteCommands {
    suspend fun toggleFavorite(photoId: PhotoId): Boolean
    suspend fun isFavorite(photoId: PhotoId): Boolean
}
