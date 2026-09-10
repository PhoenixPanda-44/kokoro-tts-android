package com.kokoro.tts.engine.tts

import android.util.Log
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsKokoroModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.kokoro.tts.data.model.KokoroModelInfo
import com.kokoro.tts.data.model.ModelType
import java.io.FileNotFoundException
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class SherpaKokoroTtsEngine : KokoroTtsEngine {

    companion object {
        private const val TAG = "SherpaKokoroTts"
        const val DEFAULT_SAMPLE_RATE = 24000
    }

    private val lock = ReentrantLock()
    private var offlineTts: OfflineTts? = null
    private var activeModelType: ModelType? = null

    override val currentModelType: ModelType?
        get() = activeModelType

    override fun isLoaded(): Boolean = lock.withLock {
        offlineTts != null
    }

    override fun loadModel(modelInfo: KokoroModelInfo) {
        lock.withLock {
            if (!modelInfo.isInstalled()) {
                throw FileNotFoundException("Model files are incomplete in ${modelInfo.installDir.absolutePath}")
            }

            // STRICT SINGLE-MODEL RAM RULE:
            // Always release and unload any previously active model first
            unloadModelInternal()

            Log.i(TAG, "Loading Kokoro model: ${modelInfo.type.displayName} from ${modelInfo.installDir.absolutePath}")

            val numCores = Runtime.getRuntime().availableProcessors()
            val numThreads = numCores.coerceIn(2, 4)

            val kokoroConfig = OfflineTtsKokoroModelConfig(
                model = modelInfo.modelFile.absolutePath,
                voices = modelInfo.voicesFile.absolutePath,
                tokens = modelInfo.tokensFile.absolutePath,
                dataDir = modelInfo.espeakDataDir.absolutePath,
                lengthScale = 1.0f
            )

            val modelConfig = OfflineTtsModelConfig(
                kokoro = kokoroConfig,
                numThreads = numThreads,
                debug = false,
                provider = "cpu"
            )

            val config = OfflineTtsConfig(
                model = modelConfig,
                ruleFsts = "",
                maxNumSentences = 1
            )

            offlineTts = OfflineTts(assetManager = null, config = config)
            activeModelType = modelInfo.type
            Log.i(TAG, "Successfully loaded model: ${modelInfo.type.displayName}. Sample rate: ${offlineTts?.sampleRate()}")
        }
    }

    override fun unloadModel() {
        lock.withLock {
            unloadModelInternal()
        }
    }

    private fun unloadModelInternal() {
        offlineTts?.let { tts ->
            Log.i(TAG, "Unloading active Kokoro model: $activeModelType")
            try {
                tts.release()
            } catch (e: Exception) {
                Log.w(TAG, "Exception releasing OfflineTts session", e)
            }
        }
        offlineTts = null
        activeModelType = null
        // Request immediate GC to reclaim native C++ tensor buffers
        System.gc()
    }

    override fun synthesize(text: String, speakerId: Int, speed: Float): SynthesisResult = lock.withLock {
        val tts = offlineTts ?: throw IllegalStateException("No Kokoro model loaded in RAM. Please load a model first.")

        val startTime = System.currentTimeMillis()
        val audio = tts.generate(text = text, sid = speakerId, speed = speed)
        val synthesisDuration = System.currentTimeMillis() - startTime

        val sampleRate = if (audio.sampleRate > 0) audio.sampleRate else DEFAULT_SAMPLE_RATE
        val audioDurationSec = if (sampleRate > 0) audio.samples.size.toFloat() / sampleRate else 0f
        val rtf = if (audioDurationSec > 0) (synthesisDuration / 1000f) / audioDurationSec else 0f

        return SynthesisResult(
            samples = audio.samples,
            sampleRate = sampleRate,
            latencyMs = synthesisDuration,
            synthesisDurationMs = synthesisDuration,
            audioDurationSeconds = audioDurationSec,
            rtf = rtf
        )
    }

    override fun synthesizeStreaming(
        text: String,
        speakerId: Int,
        speed: Float,
        onChunk: (AudioChunk) -> Boolean
    ): SynthesisResult = lock.withLock {
        val tts = offlineTts ?: throw IllegalStateException("No Kokoro model loaded in RAM. Please load a model first.")

        val startTime = System.currentTimeMillis()
        var timeToFirstChunk = 0L
        var chunkCount = 0
        val accumulatedSamples = mutableListOf<Float>()
        val sampleRate = if (tts.sampleRate() > 0) tts.sampleRate() else DEFAULT_SAMPLE_RATE

        val audio = tts.generateWithCallback(
            text = text,
            sid = speakerId,
            speed = speed
        ) { chunkSamples ->
            if (chunkCount == 0) {
                timeToFirstChunk = System.currentTimeMillis() - startTime
            }
            chunkCount++

            for (sample in chunkSamples) {
                accumulatedSamples.add(sample)
            }

            val continueSynthesis = onChunk(
                AudioChunk(
                    samples = chunkSamples,
                    sampleRate = sampleRate,
                    isFinal = false
                )
            )

            // Return 1 to continue, 0 to abort
            if (continueSynthesis) 1 else 0
        }

        val totalDuration = System.currentTimeMillis() - startTime
        val finalSamples = if (accumulatedSamples.isNotEmpty()) {
            accumulatedSamples.toFloatArray()
        } else {
            audio.samples
        }

        val audioDurationSec = if (sampleRate > 0) finalSamples.size.toFloat() / sampleRate else 0f
        val rtf = if (audioDurationSec > 0) (totalDuration / 1000f) / audioDurationSec else 0f

        // Emit final chunk indicator
        onChunk(
            AudioChunk(
                samples = floatArrayOf(),
                sampleRate = sampleRate,
                isFinal = true
            )
        )

        return SynthesisResult(
            samples = finalSamples,
            sampleRate = sampleRate,
            latencyMs = if (timeToFirstChunk > 0) timeToFirstChunk else totalDuration,
            synthesisDurationMs = totalDuration,
            audioDurationSeconds = audioDurationSec,
            rtf = rtf
        )
    }
}
