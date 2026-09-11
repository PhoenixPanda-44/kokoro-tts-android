package com.kokoro.tts.data.downloader

import android.content.Context
import com.kokoro.tts.data.model.KokoroModelInfo
import com.kokoro.tts.data.model.ModelType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

class ModelDownloader(
    private val context: Context,
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()
) {
    private val modelsBaseDir: File
        get() = File(context.filesDir, "models").apply { mkdirs() }

    fun getModelInfo(type: ModelType): KokoroModelInfo {
        return KokoroModelInfo.fromStorage(modelsBaseDir, type)
    }

    fun downloadAndExtract(type: ModelType): Flow<DownloadState> = channelFlow {
        send(DownloadState.Idle)

        val modelInfo = getModelInfo(type)
        val tempDownloadFile = File(context.cacheDir, type.archiveName)

        try {
            // Attempt primary URL first, then fallback to mirror
            val urls = listOf(type.primaryDownloadUrl, type.mirrorDownloadUrl)
            var downloadSuccess = false
            var lastException: Exception? = null

            for (url in urls) {
                if (!currentCoroutineContext().isActive) break
                try {
                    downloadFile(url, tempDownloadFile) { downloaded, total, speed ->
                        val progress = if (total > 0) downloaded.toFloat() / total.toFloat() else 0f
                        send(
                            DownloadState.Downloading(
                                progress = progress,
                                downloadedBytes = downloaded,
                                totalBytes = total,
                                bytesPerSecond = speed
                            )
                        )
                    }
                    downloadSuccess = true
                    break
                } catch (e: Exception) {
                    lastException = e
                    tempDownloadFile.delete()
                }
            }

            if (!downloadSuccess) {
                send(DownloadState.Error(
                    message = "Failed to download ${type.displayName}: ${lastException?.localizedMessage ?: "Unknown error"}",
                    throwable = lastException
                ))
                return@channelFlow
            }

            // Extract archive
            send(DownloadState.Extracting("Initializing archive extraction...", 0))
            withContext(Dispatchers.IO) {
                TarBz2Extractor.extract(
                    archiveFile = tempDownloadFile,
                    destinationDir = modelInfo.installDir
                ) { file, count ->
                    trySend(DownloadState.Extracting(file, count))
                }
            }

            // Clean up temporary download archive to save user storage
            tempDownloadFile.delete()

            // Verify installation
            if (modelInfo.isInstalled()) {
                send(DownloadState.Success)
            } else {
                send(DownloadState.Error("Extraction finished, but required model files were missing."))
            }

        } catch (e: Exception) {
            tempDownloadFile.delete()
            send(DownloadState.Error(
                message = "Error during setup: ${e.localizedMessage ?: "Unknown error"}",
                throwable = e
            ))
        }
    }.flowOn(Dispatchers.IO)

    private suspend fun downloadFile(
        url: String,
        destinationFile: File,
        onProgress: suspend (downloaded: Long, total: Long, speed: Long) -> Unit
    ) {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "KokoroTTS-Android/1.0")
            .build()

        val response: Response = okHttpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            throw IOException("HTTP error code: ${response.code} for URL: $url")
        }

        val body = response.body ?: throw IOException("Empty response body from $url")
        val contentLength = body.contentLength()

        destinationFile.parentFile?.mkdirs()
        var downloadedBytes = 0L
        var lastTime = System.currentTimeMillis()
        var bytesSinceLastTime = 0L
        var currentSpeed = 0L

        body.byteStream().use { input ->
            FileOutputStream(destinationFile).use { output ->
                val buffer = ByteArray(32768)
                var bytesRead: Int

                while (input.read(buffer).also { bytesRead = it } != -1) {
                    if (!currentCoroutineContext().isActive) {
                        throw IOException("Download cancelled by user")
                    }
                    output.write(buffer, 0, bytesRead)
                    downloadedBytes += bytesRead
                    bytesSinceLastTime += bytesRead

                    val now = System.currentTimeMillis()
                    val duration = now - lastTime
                    if (duration >= 500) { // Update speed estimate twice a second
                        currentSpeed = (bytesSinceLastTime * 1000) / duration
                        lastTime = now
                        bytesSinceLastTime = 0L
                        onProgress(downloadedBytes, contentLength, currentSpeed)
                    }
                }
                output.flush()
            }
        }
        // Final progress tick
        onProgress(downloadedBytes, contentLength, currentSpeed)
    }

    fun deleteModel(type: ModelType): Boolean {
        val info = getModelInfo(type)
        return info.deleteFiles()
    }
}
