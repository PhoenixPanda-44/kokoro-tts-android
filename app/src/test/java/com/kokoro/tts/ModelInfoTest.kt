package com.kokoro.tts

import com.kokoro.tts.data.model.KokoroModelInfo
import com.kokoro.tts.data.model.KokoroVoices
import com.kokoro.tts.data.model.ModelType
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ModelInfoTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testModelInfoInstallationCheck() {
        val baseDir = tempFolder.root
        val modelInfo = KokoroModelInfo.fromStorage(baseDir, ModelType.MULTI_LINGUAL_V1)

        // Not yet installed
        assertFalse(modelInfo.isInstalled())

        // Create partial files
        modelInfo.installDir.mkdirs()
        modelInfo.modelFile.writeText("dummy onnx content")
        assertFalse(modelInfo.isInstalled())

        // Create remaining files
        modelInfo.voicesFile.writeText("dummy voices")
        modelInfo.tokensFile.writeText("dummy tokens")
        modelInfo.espeakDataDir.mkdirs()

        // Now fully installed
        assertTrue(modelInfo.isInstalled())
        assertTrue(modelInfo.getTotalDiskSizeBytes() > 0)

        // Delete test
        assertTrue(modelInfo.deleteFiles())
        assertFalse(modelInfo.isInstalled())
    }

    @Test
    fun testVoiceListsForModels() {
        val multiVoices = KokoroVoices.getVoicesForModel(ModelType.MULTI_LINGUAL_V1)
        val englishVoices = KokoroVoices.getVoicesForModel(ModelType.ENGLISH_FP32)

        assertTrue(multiVoices.isNotEmpty())
        assertTrue(englishVoices.isNotEmpty())

        // Multi-lingual should have more voices than English-only
        assertTrue(multiVoices.size >= englishVoices.size)

        // First voice should be Heart
        assertEquals("Heart", multiVoices[0].name)
        assertEquals("af_heart", multiVoices[0].code)

        // All speaker IDs must be unique
        val ids = multiVoices.map { it.speakerId }
        assertEquals(ids.size, ids.toSet().size)
    }
}
