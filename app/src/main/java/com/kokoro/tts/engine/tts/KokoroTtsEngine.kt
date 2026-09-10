package com.kokoro.tts.engine.tts

import com.kokoro.tts.data.model.KokoroModelInfo
import com.kokoro.tts.data.model.ModelType

data class AudioChunk(
    val samples: FloatArray,
    val sampleRate: Int,
    val isFinal: Boolean
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AudioChunk) return false
        return samples.contentEquals(other.samples) &&
                sampleRate == other.sampleRate &&
                isFinal == other.isFinal
    }

    override fun hashCode(): Int {
        var result = samples.contentHashCode()
        result = 31 * result + sampleRate
        result = 31 * result + isFinal.hashCode()
        return result
    }
}

data class SynthesisResult(
    val samples: FloatArray,
    val sampleRate: Int,
    val latencyMs: Long,
    val synthesisDurationMs: Long,
    val audioDurationSeconds: Float,
    val rtf: Float // Real Time Factor = synthesisDuration / audioDuration
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SynthesisResult) return false
        return samples.contentEquals(other.samples) &&
                sampleRate == other.sampleRate &&
                latencyMs == other.latencyMs &&
                synthesisDurationMs == other.synthesisDurationMs &&
                audioDurationSeconds == other.audioDurationSeconds &&
                rtf == other.rtf
    }

    override fun hashCode(): Int {
        var result = samples.contentHashCode()
        result = 31 * result + sampleRate
        result = 31 * result + latencyMs.hashCode()
        result = 31 * result + synthesisDurationMs.hashCode()
        result = 31 * result + audioDurationSeconds.hashCode()
        result = 31 * result + rtf.hashCode()
        return result
    }
}

interface KokoroTtsEngine {
    val currentModelType: ModelType?
    fun isLoaded(): Boolean
    fun loadModel(modelInfo: KokoroModelInfo)
    fun unloadModel()
    fun synthesize(text: String, speakerId: Int, speed: Float): SynthesisResult
    fun synthesizeStreaming(
        text: String,
        speakerId: Int,
        speed: Float,
        onChunk: (AudioChunk) -> Boolean
    ): SynthesisResult
}
