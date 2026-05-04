package com.eight87.shutterboy.domain

/**
 * Typed snapshot returned by [com.eight87.shutterboy.data.repo.LibraryScanner.runScan].
 * Tonearmboy R.F.6 lesson absorbed: never raw lists, always a typed data class so
 * the contract documents itself and consumers don't drift on field meaning.
 */
data class LibrarySnapshot(
    val photos: List<Photo>,
    val folders: List<Folder>,
    val deltaCount: Int,
)
