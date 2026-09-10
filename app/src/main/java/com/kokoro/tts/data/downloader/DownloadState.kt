package com.kokoro.tts.data.downloader

sealed interface DownloadState {
    object Idle : DownloadState

    data class Downloading(
        val progress: Float, // 0.0 to 1.0
        val downloadedBytes: Long,
        val totalBytes: Long,
        val bytesPerSecond: Long
    ) : DownloadState {
        val progressPercent: Int get() = (progress * 100).toInt().coerceIn(0, 100)
        val downloadedMb: String get() = "%.1f MB".format(downloadedBytes / (1024f * 1024f))
        val totalMb: String get() = if (totalBytes > 0) "%.1f MB".format(totalBytes / (1024f * 1024f)) else "Unknown"
        val speedFormatted: String get() = "%.2f MB/s".format(bytesPerSecond / (1024f * 1024f))
    }

    data class Extracting(
        val currentFileName: String,
        val entriesExtracted: Int
    ) : DownloadState

    object Success : DownloadState

    data class Error(
        val message: String,
        val throwable: Throwable? = null
    ) : DownloadState
}
