package com.kokoro.tts.engine.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

class AudioStreamPlayer(
    private val sampleRate: Int = 24000
) {
    companion object {
        private const val TAG = "AudioStreamPlayer"
    }

    private var audioTrack: AudioTrack? = null
    private val isPlaying = AtomicBoolean(false)
    private val isPaused = AtomicBoolean(false)
    private var playbackJob: Job? = null
    private var chunkChannel = Channel<FloatArray>(Channel.UNLIMITED)

    var onPlaybackFinished: (() -> Unit)? = null

    init {
        initAudioTrack()
    }

    private fun initAudioTrack() {
        try {
            val minBufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_FLOAT
            )

            val bufferSize = (minBufferSize * 4).coerceAtLeast(32768)

            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()

            val format = AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                .setSampleRate(sampleRate)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build()

            audioTrack = AudioTrack(
                attributes,
                format,
                bufferSize,
                AudioTrack.MODE_STREAM,
                AudioManager.AUDIO_SESSION_ID_GENERATE
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing AudioTrack", e)
        }
    }

    fun startStream(scope: CoroutineScope) {
        stop()

        chunkChannel = Channel(Channel.UNLIMITED)
        isPlaying.set(true)
        isPaused.set(false)

        val track = audioTrack ?: run {
            initAudioTrack()
            audioTrack
        }

        try {
            track?.play()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start AudioTrack playback", e)
        }

        playbackJob = scope.launch(Dispatchers.Default) {
            try {
                for (samples in chunkChannel) {
                    if (!isActive || !isPlaying.get()) break
                    while (isPaused.get() && isActive) {
                        kotlinx.coroutines.delay(50)
                    }

                    if (samples.isNotEmpty()) {
                        track?.write(
                            samples,
                            0,
                            samples.size,
                            AudioTrack.WRITE_BLOCKING
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in playback loop", e)
            } finally {
                isPlaying.set(false)
                onPlaybackFinished?.invoke()
            }
        }
    }

    fun enqueueSamples(samples: FloatArray) {
        if (isPlaying.get() && samples.isNotEmpty()) {
            chunkChannel.trySend(samples)
        }
    }

    fun finishStream() {
        chunkChannel.close()
    }

    fun pause() {
        if (isPlaying.get() && !isPaused.get()) {
            isPaused.set(true)
            audioTrack?.pause()
        }
    }

    fun resume() {
        if (isPlaying.get() && isPaused.get()) {
            isPaused.set(false)
            audioTrack?.play()
        }
    }

    fun stop() {
        isPlaying.set(false)
        isPaused.set(false)
        chunkChannel.close()
        playbackJob?.cancel()
        playbackJob = null

        try {
            audioTrack?.pause()
            audioTrack?.flush()
        } catch (e: Exception) {
            Log.w(TAG, "Exception stopping AudioTrack", e)
        }
    }

    fun release() {
        stop()
        try {
            audioTrack?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Exception releasing AudioTrack", e)
        }
        audioTrack = null
    }

    fun isCurrentlyPlaying(): Boolean = isPlaying.get() && !isPaused.get()
    fun isCurrentlyPaused(): Boolean = isPlaying.get() && isPaused.get()
}
