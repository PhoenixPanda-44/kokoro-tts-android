package com.kokoro.tts.engine.audio

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

object WavExporter {

    fun exportToWavFile(
        samples: FloatArray,
        sampleRate: Int = 24000,
        outputFile: File
    ): Boolean {
        outputFile.parentFile?.mkdirs()

        val numSamples = samples.size
        val numChannels = 1
        val bitsPerSample = 16
        val bytesPerSample = bitsPerSample / 8
        val dataSize = numSamples * bytesPerSample
        val totalSize = 36 + dataSize

        FileOutputStream(outputFile).use { fos ->
            val header = ByteBuffer.allocate(44).apply {
                order(ByteOrder.LITTLE_ENDIAN)
                // "RIFF"
                put('R'.code.toByte())
                put('I'.code.toByte())
                put('F'.code.toByte())
                put('F'.code.toByte())
                putInt(totalSize)
                // "WAVE"
                put('W'.code.toByte())
                put('A'.code.toByte())
                put('V'.code.toByte())
                put('E'.code.toByte())
                // "fmt "
                put('f'.code.toByte())
                put('m'.code.toByte())
                put('t'.code.toByte())
                put(' '.code.toByte())
                putInt(16) // Subchunk1Size for PCM
                putShort(1.toShort()) // AudioFormat = PCM
                putShort(numChannels.toShort()) // Mono
                putInt(sampleRate) // SampleRate
                putInt(sampleRate * numChannels * bytesPerSample) // ByteRate
                putShort((numChannels * bytesPerSample).toShort()) // BlockAlign
                putShort(bitsPerSample.toShort()) // BitsPerSample
                // "data"
                put('d'.code.toByte())
                put('a'.code.toByte())
                put('t'.code.toByte())
                put('a'.code.toByte())
                putInt(dataSize) // Subchunk2Size
            }
            fos.write(header.array())

            // Write PCM 16-bit samples
            val sampleBuffer = ByteBuffer.allocate(8192).apply {
                order(ByteOrder.LITTLE_ENDIAN)
            }

            for (sample in samples) {
                // Clamp between -1.0 and 1.0, then scale to 16-bit signed integer
                val clamped = sample.coerceIn(-1.0f, 1.0f)
                val pcmValue = (clamped * 32767.0f).toInt().toShort()

                if (sampleBuffer.remaining() < 2) {
                    fos.write(sampleBuffer.array(), 0, sampleBuffer.position())
                    sampleBuffer.clear()
                }
                sampleBuffer.putShort(pcmValue)
            }

            if (sampleBuffer.position() > 0) {
                fos.write(sampleBuffer.array(), 0, sampleBuffer.position())
            }
        }
        return true
    }

    fun createShareIntent(
        context: Context,
        samples: FloatArray,
        sampleRate: Int = 24000,
        fileNamePrefix: String = "kokoro_speech"
    ): Intent? {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val fileName = "${fileNamePrefix}_${System.currentTimeMillis()}.wav"
        val wavFile = File(exportDir, fileName)

        val success = exportToWavFile(samples, sampleRate, wavFile)
        if (!success || !wavFile.exists()) return null

        val authority = "${context.packageName}.fileprovider"
        val contentUri: Uri = FileProvider.getUriForFile(context, authority, wavFile)

        return Intent(Intent.ACTION_SEND).apply {
            type = "audio/wav"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            putExtra(Intent.EXTRA_SUBJECT, "Kokoro TTS Speech")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
