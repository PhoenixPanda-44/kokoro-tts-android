# Kokoro TTS (82M) Android App

An ultra-lightweight, modular Native Android application running the **Kokoro TTS 82M** speech synthesis model entirely on-device via **Sherpa-ONNX** (precompiled C++ ONNX Runtime + phonemizer).

---

## Key Highlights

- 🪶 **Super Lightweight APK (~10MB)**: Zero bundled models in the APK. Packaging configured with Gradle ABI splits (`arm64-v8a` and `x86_64`) to eliminate multi-architecture bloat.
- ⚡ **Strict Single-Model RAM Footprint**: To protect mobile RAM, the engine guarantees **only one model is loaded into memory at any given time**. Switching models automatically releases native C++ tensors and reclaims memory before loading the new one.
- 📥 **On-Demand Dynamic Download**: Models are downloaded upon user selection with real-time speed/progress monitoring and automatically extracted to internal app-private storage (`context.filesDir/models/`). When the application is uninstalled, all model files are deleted by the OS.
- 🔊 **Instant Streaming Synthesis**: Synthesizes and streams speech chunks directly into Android's low-latency native `AudioTrack` without waiting for the full sentence to finish generating.
- 📊 **Real-Time Performance Telemetry**: Real-time display for Time-to-First-Byte (TTFB) latency, Real-Time Factor (RTF), and active process RAM consumption.
- 💾 **Audio Export**: Export or share any synthesized speech as a clean, standard 24kHz 16-bit PCM `.wav` file.
- 🧩 **Modular Clean Architecture**: Fully decoupled `KokoroTtsEngine` interface, making it trivial to extend into a background service, audiobook reader, LLM voice output, or Android `TextToSpeechService`.

---

## Architecture Overview

```
app/src/main/java/com/kokoro/tts/
├── MainActivity.kt                      # Single-activity Compose entry point
├── data/
│   ├── model/
│   │   ├── ModelType.kt                 # Model definitions (Multi-lingual v1.0, English fp32, English int8)
│   │   ├── KokoroModelInfo.kt           # File verification, paths, and disk management
│   │   └── VoiceInfo.kt                 # Voice definitions (Heart, Bella, Adam, Emma, etc.)
│   └── downloader/
│       ├── DownloadState.kt             # Reactive download state (Progress, Extracting, Success, Error)
│       ├── TarBz2Extractor.kt           # Fast streaming .tar.bz2 extraction with TarSlip protection
│       └── ModelDownloader.kt           # OkHttp streaming downloader with mirror fallback
├── engine/
│   ├── tts/
│   │   ├── KokoroTtsEngine.kt           # Decoupled TTS Interface & SynthesisResult
│   │   └── SherpaKokoroTtsEngine.kt     # Sherpa-ONNX JNI implementation with single-RAM lock
│   └── audio/
│       ├── AudioStreamPlayer.kt         # Low-latency streaming playback via AudioTrack
│       └── WavExporter.kt               # 24kHz 16-bit PCM WAV encoder & Share intent generator
└── ui/
    ├── MainScreen.kt                    # Interactive TTS studio screen
    ├── TtsViewModel.kt                  # State management, RAM tracker, model lifecycle coordinator
    ├── components/
    │   ├── PerformanceBanner.kt         # Latency, RTF, RAM usage display
    │   ├── PlaybackControls.kt          # Play, Pause, Stop, Share WAV buttons
    │   ├── VoiceSelector.kt             # Voice picker dialog with gender/accent filters
    │   └── ModelManagerSheet.kt         # Model download manager & storage manager
    └── theme/
        ├── Color.kt, Theme.kt, Type.kt  # Clean slate & indigo theme
```

---

## Supported Kokoro Models

| Model Variant | Archive Size | RAM Footprint | Languages | Description |
| :--- | :---: | :---: | :---: | :--- |
| **Kokoro Multi-lingual v1.0 (int8)** | ~110 MB | ~160 MB | Multilingual | English, Japanese, Mandarin, French, Spanish, Hindi, etc. |
| **Kokoro English v0.19 (fp32)** | ~320 MB | ~420 MB | English | Maximum floating-point precision and acoustic fidelity. |
| **Kokoro English v0.19 (int8)** | ~85 MB | ~120 MB | English | Ultra-lean English model with lowest RAM consumption. |

---

## Memory Management & Lifecycle

```
User selects "Switch to English fp32"
          │
          ▼
SherpaKokoroTtsEngine.loadModel()
          │
          ├─► 1. Stop active AudioTrack playback
          ├─► 2. Call offlineTts.release() [Destroys native C++ ONNX session]
          ├─► 3. Nullify references & trigger System.gc()
          ├─► 4. Verify target model files in context.filesDir/models/
          ├─► 5. Initialize new OfflineTts instance with target model
          └─► 6. Update UI State & active voices
```

---

## Building the Application

### Requirements
- JDK 17+
- Android SDK (API 34)

### Build Debug APK (ABI Splits)
```bash
./gradlew assembleDebug
```
The output APKs will be generated in `app/build/outputs/apk/debug/`:
- `app-arm64-v8a-debug.apk` (~10MB) — for physical Android devices
- `app-x86_64-debug.apk` — for Android emulators

### Running Unit Tests
```bash
./gradlew test
```

---

## Extending for Multiple Purposes

The core engine is completely independent of the UI:

```kotlin
// Initialize the engine
val engine: KokoroTtsEngine = SherpaKokoroTtsEngine()

// Load a downloaded model
val modelInfo = KokoroModelInfo.fromStorage(context.filesDir, ModelType.MULTI_LINGUAL_V1)
engine.loadModel(modelInfo)

// Stream chunks directly to any audio sink
engine.synthesizeStreaming(
    text = "Hello from Kokoro TTS!",
    speakerId = 0, // 0 = af_heart
    speed = 1.0f
) { chunk ->
    // Handle chunk.samples (FloatArray at 24000 Hz)
    true // return true to continue, false to stop
}
```

This can be directly connected to:
- Android system `TextToSpeechService`
- An eBook reader or article reader
- An on-device LLM agent / voice assistant
- A WebSocket or local HTTP server
