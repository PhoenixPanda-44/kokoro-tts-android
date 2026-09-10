package com.kokoro.tts.data.model

import java.io.File

data class KokoroModelInfo(
    val type: ModelType,
    val installDir: File
) {
    val modelFile: File get() = File(installDir, "model.onnx")
    val voicesFile: File get() = File(installDir, "voices.bin")
    val tokensFile: File get() = File(installDir, "tokens.txt")
    val espeakDataDir: File get() = File(installDir, "espeak-ng-data")

    fun isInstalled(): Boolean {
        return modelFile.exists() && modelFile.length() > 0 &&
                voicesFile.exists() && voicesFile.length() > 0 &&
                tokensFile.exists() && tokensFile.length() > 0 &&
                espeakDataDir.exists() && espeakDataDir.isDirectory
    }

    fun getTotalDiskSizeBytes(): Long {
        if (!installDir.exists()) return 0L
        return installDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

    fun deleteFiles(): Boolean {
        return if (installDir.exists()) {
            installDir.deleteRecursively()
        } else {
            true
        }
    }

    companion object {
        fun fromStorage(baseModelsDir: File, type: ModelType): KokoroModelInfo {
            val dir = File(baseModelsDir, type.id)
            return KokoroModelInfo(type, dir)
        }
    }
}
