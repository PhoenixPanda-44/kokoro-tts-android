package com.kokoro.tts.data.model

import java.io.File

data class KokoroModelInfo(
    val type: ModelType,
    val installDir: File
) {
    val effectiveDir: File get() {
        if (File(installDir, "voices.bin").exists()) return installDir
        val subdirs = installDir.listFiles()?.filter { it.isDirectory } ?: emptyList()
        for (sub in subdirs) {
            if (File(sub, "voices.bin").exists()) return sub
        }
        return installDir
    }

    val modelFile: File get() {
        val dir = effectiveDir
        val int8File = File(dir, "model.int8.onnx")
        if (int8File.exists()) return int8File
        val defaultFile = File(dir, "model.onnx")
        if (defaultFile.exists()) return defaultFile
        return if (type.id.contains("int8")) int8File else defaultFile
    }

    val voicesFile: File get() = File(effectiveDir, "voices.bin")
    val tokensFile: File get() = File(effectiveDir, "tokens.txt")
    val espeakDataDir: File get() = File(effectiveDir, "espeak-ng-data")

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
