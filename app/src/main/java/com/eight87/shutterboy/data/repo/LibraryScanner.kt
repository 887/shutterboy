package com.eight87.shutterboy.data.repo

import com.eight87.shutterboy.domain.LibrarySnapshot
import com.eight87.shutterboy.domain.ScanProgress
import kotlinx.coroutines.flow.Flow

interface LibraryScanner {
    suspend fun runScan(): LibrarySnapshot
    fun scanProgress(): Flow<ScanProgress>
}
