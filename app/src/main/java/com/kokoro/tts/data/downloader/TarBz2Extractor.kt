package com.kokoro.tts.data.downloader

import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object TarBz2Extractor {

    fun extract(
        archiveFile: File,
        destinationDir: File,
        onProgress: (fileName: String, count: Int) -> Unit = { _, _ -> }
    ): Boolean {
        if (!archiveFile.exists()) return false

        destinationDir.mkdirs()
        val destCanonicalPath = destinationDir.canonicalPath

        var count = 0
        val buffer = ByteArray(16384)

        // First pass or streaming pass:
        // We will strip common top-level directory prefix if present
        FileInputStream(archiveFile).use { fis ->
            BufferedInputStream(fis, 32768).use { bis ->
                BZip2CompressorInputStream(bis).use { bzIn ->
                    TarArchiveInputStream(bzIn).use { tarIn ->
                        var entry: TarArchiveEntry? = tarIn.nextTarEntry
                        while (entry != null) {
                            val rawName = entry.name
                            // Normalize name and remove potential top-level directory folder if archive wraps it
                            val cleanName = stripTopLevelFolder(rawName)

                            if (cleanName.isNotBlank() && cleanName != ".") {
                                val outputFile = File(destinationDir, cleanName)
                                val outputCanonicalPath = outputFile.canonicalPath

                                // Security check against path traversal (TarSlip)
                                if (!outputCanonicalPath.startsWith(destCanonicalPath)) {
                                    throw SecurityException("Malicious archive entry detected: ${entry.name}")
                                }

                                if (entry.isDirectory) {
                                    outputFile.mkdirs()
                                } else {
                                    outputFile.parentFile?.mkdirs()
                                    FileOutputStream(outputFile).use { fos ->
                                        var len: Int
                                        while (tarIn.read(buffer).also { len = it } != -1) {
                                            fos.write(buffer, 0, len)
                                        }
                                    }
                                    count++
                                    onProgress(outputFile.name, count)
                                }
                            }
                            entry = tarIn.nextTarEntry
                        }
                    }
                }
            }
        }
        return true
    }

    private fun stripTopLevelFolder(entryPath: String): String {
        val parts = entryPath.split('/').filter { it.isNotBlank() && it != "." }
        return if (parts.size > 1 && (parts[0].startsWith("kokoro") || parts[0].contains("sherpa"))) {
            // e.g. kokoro-int8-en-v0_19/model.int8.onnx -> model.int8.onnx
            parts.drop(1).joinToString("/")
        } else if (parts.size == 1 && (parts[0].startsWith("kokoro") || parts[0].contains("sherpa"))) {
            "" // Skip root directory entry itself
        } else {
            parts.joinToString("/")
        }
    }
}
