package com.eight87.shutterboy.domain

sealed interface ScanProgress {
    data object Idle : ScanProgress
    data class Running(val processed: Int, val total: Int?) : ScanProgress
    data class Done(val deltaCount: Int) : ScanProgress
    data class Failed(val reason: String) : ScanProgress
}
