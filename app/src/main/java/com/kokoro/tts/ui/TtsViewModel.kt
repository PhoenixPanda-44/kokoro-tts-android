package com.kokoro.tts.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kokoro.tts.data.downloader.DownloadState
import com.kokoro.tts.data.downloader.ModelDownloader
import com.kokoro.tts.data.model.KokoroModelInfo
import com.kokoro.tts.data.model.KokoroVoices
import com.kokoro.tts.data.model.ModelType
import com.kokoro.tts.data.model.VoiceInfo
import com.kokoro.tts.engine.audio.AudioStreamPlayer
import com.kokoro.tts.engine.audio.WavExporter
import com.kokoro.tts.engine.tts.KokoroTtsEngine
import com.kokoro.tts.engine.tts.SherpaKokoroTtsEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class PerformanceMetrics(
    val latencyMs: Long = 0L,
    val rtf: Float = 0.0f,
    val audioDurationSeconds: Float = 0.0f,
    val appRamUsedMb: Long = 0L,
    val appRamMaxMb: Long = 0L
)

data class TtsUiState(
    val activeModel: ModelType? = null,
    val isModelLoaded: Boolean = false,
    val installedModels: Set<ModelType> = emptySet(),
    val availableVoices: List<VoiceInfo> = KokoroVoices.multiLingualVoices,
    val selectedVoice: VoiceInfo = KokoroVoices.multiLingualVoices[0], // Heart
    val playbackSpeed: Float = 1.0f,
    val inputText: String = "Hello! Kokoro TTS is running on-device with ONNX Runtime.",
    val isSynthesizing: Boolean = false,
    val isPlaying: Boolean = false,
    val isPaused: Boolean = false,
    val downloadState: DownloadState = DownloadState.Idle,
    val downloadingModelType: ModelType? = null,
    val performanceMetrics: PerformanceMetrics = PerformanceMetrics(),
    val isModelManagerOpen: Boolean = false,
    val statusMessage: String? = null,
    val hasGeneratedAudio: Boolean = false
)

class TtsViewModel(
    application: Application,
    private val ttsEngine: KokoroTtsEngine = SherpaKokoroTtsEngine(),
    private val modelDownloader: ModelDownloader = ModelDownloader(application)
) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "TtsViewModel"
    }

    private val _uiState = MutableStateFlow(TtsUiState())
    val uiState: StateFlow<TtsUiState> = _uiState.asStateFlow()

    private val audioPlayer = AudioStreamPlayer(sampleRate = SherpaKokoroTtsEngine.DEFAULT_SAMPLE_RATE)
    private var synthesisJob: Job? = null
    private var downloadJob: Job? = null
    private var lastGeneratedSamples: FloatArray? = null

    init {
        audioPlayer.onPlaybackFinished = {
            _uiState.update { it.copy(isPlaying = false, isPaused = false) }
        }
        refreshInstalledModels()
        startRamMonitoring()
    }

    fun refreshInstalledModels() {
        val installed = ModelType.entries.filter { type ->
            modelDownloader.getModelInfo(type).isInstalled()
        }.toSet()

        _uiState.update { current ->
            val updatedActive = if (installed.contains(current.activeModel)) current.activeModel else installed.firstOrNull()
            current.copy(
                installedModels = installed,
                activeModel = updatedActive,
                isModelManagerOpen = installed.isEmpty() // Automatically open on first run if nothing is downloaded!
            )
        }

        // Auto-load first available model if none is currently loaded
        val currentActive = _uiState.value.activeModel
        if (currentActive != null && !ttsEngine.isLoaded()) {
            loadModel(currentActive)
        }
    }

    fun setInputText(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun setSelectedVoice(voice: VoiceInfo) {
        _uiState.update { it.copy(selectedVoice = voice) }
    }

    fun setPlaybackSpeed(speed: Float) {
        _uiState.update { it.copy(playbackSpeed = speed) }
    }

    fun openModelManager() {
        _uiState.update { it.copy(isModelManagerOpen = true) }
    }

    fun closeModelManager() {
        _uiState.update { it.copy(isModelManagerOpen = false) }
    }

    fun loadModel(modelType: ModelType) {
        viewModelScope.launch(Dispatchers.IO) {
            val modelInfo = modelDownloader.getModelInfo(modelType)
            if (!modelInfo.isInstalled()) {
                _uiState.update { it.copy(statusMessage = "Model not downloaded: ${modelType.displayName}") }
                return@launch
            }

            stopPlayback()

            try {
                // Strict single-model RAM: ttsEngine.loadModel internally unloads previous model first
                ttsEngine.loadModel(modelInfo)
                val voices = KokoroVoices.getVoicesForModel(modelType)
                val newSelectedVoice = voices.find { it.speakerId == _uiState.value.selectedVoice.speakerId } ?: voices[0]

                _uiState.update {
                    it.copy(
                        activeModel = modelType,
                        isModelLoaded = true,
                        availableVoices = voices,
                        selectedVoice = newSelectedVoice,
                        statusMessage = "Loaded ${modelType.displayName}"
                    )
                }
                updateRamMetrics()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load model $modelType", e)
                _uiState.update {
                    it.copy(
                        isModelLoaded = false,
                        statusMessage = "Error loading model: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun unloadModel() {
        viewModelScope.launch(Dispatchers.IO) {
            stopPlayback()
            ttsEngine.unloadModel()
            _uiState.update {
                it.copy(
                    isModelLoaded = false,
                    statusMessage = "Model unloaded from RAM"
                )
            }
            updateRamMetrics()
        }
    }

    fun downloadModel(modelType: ModelType) {
        if (downloadJob?.isActive == true) return

        downloadJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    downloadingModelType = modelType,
                    downloadState = DownloadState.Idle
                )
            }

            modelDownloader.downloadAndExtract(modelType).collect { state ->
                _uiState.update { it.copy(downloadState = state) }
                if (state is DownloadState.Success) {
                    refreshInstalledModels()
                    loadModel(modelType)
                    _uiState.update {
                        it.copy(
                            downloadingModelType = null,
                            isModelManagerOpen = false,
                            statusMessage = "Ready: ${modelType.displayName}"
                        )
                    }
                } else if (state is DownloadState.Error) {
                    _uiState.update {
                        it.copy(
                            downloadingModelType = null,
                            statusMessage = "Download failed: ${state.message}"
                        )
                    }
                }
            }
        }
    }

    fun cancelDownload() {
        downloadJob?.cancel()
        downloadJob = null
        _uiState.update {
            it.copy(
                downloadingModelType = null,
                downloadState = DownloadState.Idle,
                statusMessage = "Download cancelled"
            )
        }
    }

    fun deleteModel(modelType: ModelType) {
        viewModelScope.launch(Dispatchers.IO) {
            if (_uiState.value.activeModel == modelType) {
                ttsEngine.unloadModel()
                _uiState.update { it.copy(isModelLoaded = false, activeModel = null) }
            }
            modelDownloader.deleteModel(modelType)
            refreshInstalledModels()
            _uiState.update {
                it.copy(statusMessage = "Deleted ${modelType.displayName}")
            }
            updateRamMetrics()
        }
    }

    fun playOrSynthesize() {
        val state = _uiState.value

        // Resume if paused
        if (state.isPaused) {
            audioPlayer.resume()
            _uiState.update { it.copy(isPaused = false, isPlaying = true) }
            return
        }

        // Already synthesizing/playing
        if (state.isSynthesizing || state.isPlaying) {
            return
        }

        if (!state.isModelLoaded) {
            _uiState.update { it.copy(statusMessage = "No model loaded. Please select and download a model.") }
            openModelManager()
            return
        }

        val text = state.inputText.trim()
        if (text.isEmpty()) {
            _uiState.update { it.copy(statusMessage = "Please enter text to speak") }
            return
        }

        synthesisJob = viewModelScope.launch(Dispatchers.Default) {
            _uiState.update {
                it.copy(
                    isSynthesizing = true,
                    isPlaying = true,
                    isPaused = false,
                    statusMessage = "Synthesizing audio..."
                )
            }

            audioPlayer.startStream(viewModelScope)

            try {
                val voiceId = _uiState.value.selectedVoice.speakerId
                val speed = _uiState.value.playbackSpeed

                val result = ttsEngine.synthesizeStreaming(
                    text = text,
                    speakerId = voiceId,
                    speed = speed
                ) { chunk ->
                    if (!isActive) return@synthesizeStreaming false
                    if (!chunk.isFinal && chunk.samples.isNotEmpty()) {
                        audioPlayer.enqueueSamples(chunk.samples)
                    }
                    true
                }

                audioPlayer.finishStream()
                lastGeneratedSamples = result.samples

                _uiState.update {
                    it.copy(
                        isSynthesizing = false,
                        hasGeneratedAudio = result.samples.isNotEmpty(),
                        performanceMetrics = it.performanceMetrics.copy(
                            latencyMs = result.latencyMs,
                            rtf = result.rtf,
                            audioDurationSeconds = result.audioDurationSeconds
                        ),
                        statusMessage = "Done: %.1fs audio (RTF: %.2f)".format(result.audioDurationSeconds, result.rtf)
                    )
                }
                updateRamMetrics()

            } catch (e: Exception) {
                Log.e(TAG, "Error synthesizing speech", e)
                audioPlayer.stop()
                _uiState.update {
                    it.copy(
                        isSynthesizing = false,
                        isPlaying = false,
                        statusMessage = "Speech error: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun pausePlayback() {
        if (_uiState.value.isPlaying && !_uiState.value.isPaused) {
            audioPlayer.pause()
            _uiState.update { it.copy(isPaused = true) }
        }
    }

    fun stopPlayback() {
        synthesisJob?.cancel()
        synthesisJob = null
        audioPlayer.stop()
        _uiState.update {
            it.copy(
                isSynthesizing = false,
                isPlaying = false,
                isPaused = false
            )
        }
    }

    fun getShareWavIntent(context: Context): Intent? {
        val samples = lastGeneratedSamples ?: return null
        return WavExporter.createShareIntent(
            context = context,
            samples = samples,
            sampleRate = SherpaKokoroTtsEngine.DEFAULT_SAMPLE_RATE
        )
    }

    private fun startRamMonitoring() {
        viewModelScope.launch(Dispatchers.Default) {
            while (isActive) {
                updateRamMetrics()
                delay(2000)
            }
        }
    }

    private fun updateRamMetrics() {
        val runtime = Runtime.getRuntime()
        val usedMem = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
        val maxMem = runtime.maxMemory() / (1024 * 1024)

        _uiState.update {
            it.copy(
                performanceMetrics = it.performanceMetrics.copy(
                    appRamUsedMb = usedMem,
                    appRamMaxMb = maxMem
                )
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayer.release()
        ttsEngine.unloadModel()
    }
}
