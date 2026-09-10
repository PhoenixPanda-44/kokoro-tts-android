package com.kokoro.tts.data.model

enum class ModelType(
    val id: String,
    val displayName: String,
    val description: String,
    val estimatedDownloadSizeMb: Int,
    val estimatedRamUsageMb: Int,
    val archiveName: String,
    val primaryDownloadUrl: String,
    val mirrorDownloadUrl: String
) {
    MULTI_LINGUAL_V1(
        id = "kokoro-multi-lang-v1_0",
        displayName = "Kokoro Multi-lingual v1.0 (int8)",
        description = "Supports English, Japanese, Mandarin, French, Spanish, Hindi, etc.",
        estimatedDownloadSizeMb = 110,
        estimatedRamUsageMb = 160,
        archiveName = "kokoro-multi-lang-v1_0.tar.bz2",
        primaryDownloadUrl = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/kokoro-multi-lang-v1_0.tar.bz2",
        mirrorDownloadUrl = "https://huggingface.co/csukuangfj/sherpa-onnx-tts-kokoro-multi-lang-v1_0/resolve/main/kokoro-multi-lang-v1_0.tar.bz2"
    ),
    ENGLISH_FP32(
        id = "kokoro-en-v0_19",
        displayName = "Kokoro English v0.19 (fp32)",
        description = "Maximum audio precision and fidelity for English speech",
        estimatedDownloadSizeMb = 320,
        estimatedRamUsageMb = 420,
        archiveName = "kokoro-en-v0_19.tar.bz2",
        primaryDownloadUrl = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/kokoro-en-v0_19.tar.bz2",
        mirrorDownloadUrl = "https://huggingface.co/csukuangfj/sherpa-onnx-tts-kokoro-en-v0_19/resolve/main/kokoro-en-v0_19.tar.bz2"
    ),
    ENGLISH_INT8(
        id = "kokoro-en-v0_19-int8",
        displayName = "Kokoro English v0.19 (int8)",
        description = "Ultra-lean English model with lowest RAM and fastest download",
        estimatedDownloadSizeMb = 85,
        estimatedRamUsageMb = 120,
        archiveName = "kokoro-en-v0_19-int8.tar.bz2",
        primaryDownloadUrl = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/kokoro-en-v0_19-int8.tar.bz2",
        mirrorDownloadUrl = "https://huggingface.co/csukuangfj/sherpa-onnx-tts-kokoro-en-v0_19/resolve/main/kokoro-en-v0_19-int8.tar.bz2"
    );

    companion object {
        fun fromId(id: String): ModelType? = entries.find { it.id == id }
    }
}
