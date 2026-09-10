package com.kokoro.tts

import com.kokoro.tts.engine.audio.WavExporter
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class WavExporterTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testWavHeaderAndDataGeneration() {
        val outputFile = File(tempFolder.root, "test_output.wav")
        val sampleRate = 24000
        val testSamples = floatArrayOf(0.0f, 0.5f, -0.5f, 1.0f, -1.0f)

        val result = WavExporter.exportToWavFile(testSamples, sampleRate, outputFile)
        assertTrue(result)
        assertTrue(outputFile.exists())

        // 44-byte header + 5 samples * 2 bytes = 54 bytes
        assertEquals(44 + (testSamples.size * 2).toLong(), outputFile.length())

        val bytes = ByteArray(44)
        FileInputStream(outputFile).use { fis ->
            fis.read(bytes)
        }

        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)

        // Magic RIFF
        val riff = String(bytes, 0, 4)
        assertEquals("RIFF", riff)

        val totalSize = buffer.getInt(4)
        assertEquals(36 + testSamples.size * 2, totalSize)

        // Magic WAVE
        val wave = String(bytes, 8, 4)
        assertEquals("WAVE", wave)

        // Magic fmt
        val fmt = String(bytes, 12, 4)
        assertEquals("fmt ", fmt)

        val subchunk1Size = buffer.getInt(16)
        assertEquals(16, subchunk1Size)

        val audioFormat = buffer.getShort(20)
        assertEquals(1.toShort(), audioFormat) // PCM

        val channels = buffer.getShort(22)
        assertEquals(1.toShort(), channels) // Mono

        val rate = buffer.getInt(24)
        assertEquals(sampleRate, rate)

        val bitsPerSample = buffer.getShort(34)
        assertEquals(16.toShort(), bitsPerSample)

        // Magic data
        val dataTag = String(bytes, 36, 4)
        assertEquals("data", dataTag)

        val dataSize = buffer.getInt(40)
        assertEquals(testSamples.size * 2, dataSize)
    }

    @Test
    fun testClampingBehavior() {
        val outputFile = File(tempFolder.root, "clamped_output.wav")
        // Samples exceeding [-1.0, 1.0]
        val extremeSamples = floatArrayOf(2.5f, -3.0f)
        val result = WavExporter.exportToWavFile(extremeSamples, 24000, outputFile)
        assertTrue(result)

        val bytes = ByteArray(48)
        FileInputStream(outputFile).use { fis -> fis.read(bytes) }

        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val firstSample = buffer.getShort(44)
        val secondSample = buffer.getShort(46)

        // Clamped to max positive and negative short
        assertEquals(32767.toShort(), firstSample)
        assertEquals((-32767).toShort(), secondSample)
    }
}
