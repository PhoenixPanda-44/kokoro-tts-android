# Comprehensive Project Documentation & Post-Mortem: Kokoro TTS 82M Android App

**Document Purpose:** This document provides a complete, end-to-end record of the Kokoro TTS Android project—including the original vision, the design interview ("grill-me"), architectural decisions, source code implementation, all issues/bottlenecks encountered during compilation, and actionable insights for a fresh, clean restart. You can share this document directly with Gemini Web or any AI assistant to plan the next iteration properly.

---

## 1. Executive Summary & Original Vision

### Core Objective
Build an ultra-lightweight, high-performance on-device Android Text-to-Speech (TTS) application powered by the **Kokoro TTS 82M** model using an **ONNX runtime**.

### Guiding Principles & Hard Constraints
1. **Super-Lightweight APK Footprint (~10 MB):**
   - The APK must **never bundle model weights** out-of-the-box.
   - Binary packaging must be stripped of unnecessary architecture bloat using Gradle ABI splits (`arm64-v8a`).
2. **On-Demand Model Download & Storage Lifecycle:**
   - The user selects and downloads the model at runtime from within the app.
   - Downloaded model archives are unpacked directly into app-private internal storage (`context.filesDir/models/`).
   - When the user uninstalls the app from Android settings, the OS automatically wipes all downloaded model files with zero residual footprint.
3. **Strict Single-Model RAM Constraint:**
   - The Kokoro-82M model consumes significant RAM (approx. 120 MB to 420 MB depending on quantization/precision).
   - To prevent Out-Of-Memory (OOM) crashes on budget/mid-range Android devices, **only ONE model can be loaded in RAM at any given time**.
   - Switching models must explicitly stop playback, destroy the existing C++ ONNX inference session, clear memory references, and trigger garbage collection before loading the new model.
4. **Low-Latency Streaming Speech:**
   - The engine must stream synthesized audio chunks directly into Android's low-latency native `AudioTrack` (24,000 Hz float/16-bit PCM) as they are generated, so playback begins almost immediately without waiting for long paragraphs to finish processing.
5. **Modular Architecture for Multi-Purpose Reuse:**
   - Built as an MVP with a decoupled TTS engine interface (`KokoroTtsEngine`), allowing it to be easily repurposed later as an Android system `TextToSpeechService`, background audiobook reader, on-device LLM voice response module, or socket receiver.

---

## 2. Design Interview ("Grill-Me"): Questions, Answers & Decisions

During the architectural alignment phase, every branch of the design tree was systematically explored and resolved:

| Decision Area | Question Asked | User's Selected Answer | Technical Rationale |
| :--- | :--- | :--- | :--- |
| **Framework / Platform** | Native Android (Kotlin) vs. Flutter? | **Native Android (Kotlin)** | Lowest possible APK footprint (~10MB base) and minimal runtime RAM overhead, avoiding the ~15–20MB Flutter engine overhead. |
| **TTS Inference Engine** | Sherpa-ONNX vs. Custom ONNX Runtime + Custom JNI phonemizer? | **Sherpa-ONNX (`com.k2fsa.sherpa.onnx`)** | Precompiled C++ ONNX Runtime + native `espeak-ng` phonemizer, battle-tested for Kokoro, optimized for mobile CPUs. |
| **Supported Models** | Which Kokoro model variants should be supported? | **Both Kokoro Multi-lingual v1.0 (int8) AND Kokoro English (fp32)** | Multi-lingual int8 (~110MB download, ~160MB RAM) for international languages; English fp32 (~320MB download, ~420MB RAM) for high fidelity. Only 1 in RAM at a time. |
| **First-Launch UX** | First-launch setup screen vs. silent auto-download? | **First-launch setup modal** | Prompts user to select which model to download first with clear progress bar (% + extraction), with ability to switch/delete models later in Settings. |
| **MVP Features** | Which core controls and features should be included? | **Full suite:** Streaming playback, Play/Pause/Stop, Voice dropdown, Speed slider (0.5x–2.0x), WAV export/share, and Performance stats banner (TTFB, RTF, RAM). | Provides an interactive, feature-complete studio to test and benchmark Kokoro speech generation. |
| **Service Integration** | Standalone engine interface vs. Android `TextToSpeechService` hook now? | **Standalone clean TTS Engine API first** | Keeps the MVP lean and focused, while ensuring clean decoupling for future integration. |
| **Packaging & ABIs** | ABI splits vs. Single Universal APK? | **Enable ABI splits (`arm64-v8a`)** | Eliminates multi-architecture binary overhead, ensuring the APK remains ~10MB. |

---

## 3. What Was Built (Codebase Architecture)

The codebase was created at `/home/phoenix/Desktop/Cline` with the following modular MVVM package structure:

```
app/src/main/java/com/kokoro/tts/
├── MainActivity.kt                      # Single-activity Jetpack Compose entry point
├── data/
│   ├── model/
│   │   ├── ModelType.kt                 # Definitions & mirror URLs for Multi-lingual v1.0 & English fp32/int8
│   │   ├── KokoroModelInfo.kt           # Filesystem verification, model paths, disk usage & deletion
│   │   └── VoiceInfo.kt                 # 37+ Kokoro voice profiles (Heart, Bella, Adam, Emma, etc.)
│   └── downloader/
│       ├── DownloadState.kt             # Reactive download state (Idle, Downloading, Extracting, Success, Error)
│       ├── TarBz2Extractor.kt           # Streaming .tar.bz2 archive extractor with TarSlip security checks
│       └── ModelDownloader.kt           # OkHttp streaming downloader with mirror fallback & disk cleanup
├── engine/
│   ├── tts/
│   │   ├── KokoroTtsEngine.kt           # Decoupled TTS Interface & SynthesisResult contract
│   │   └── SherpaKokoroTtsEngine.kt     # Sherpa-ONNX JNI implementation with strict single-RAM lock
│   └── audio/
│       ├── AudioStreamPlayer.kt         # Low-latency streaming playback via native AudioTrack
│       └── WavExporter.kt               # 24kHz 16-bit PCM WAV encoder & Android Share Sheet intent
└── ui/
    ├── MainScreen.kt                    # Interactive studio: text presets, sliders, actions
    ├── TtsViewModel.kt                  # State coordinator, RAM monitoring, model lifecycle manager
    ├── components/
    │   ├── PerformanceBanner.kt         # Real-time display of TTFB latency, RTF, and active App RAM
    │   ├── PlaybackControls.kt          # Play, Pause, Stop, and WAV Share buttons with status pulses
    │   ├── VoiceSelector.kt             # Voice picker dialog with gender and accent filters
    │   └── ModelManagerSheet.kt         # Model downloader, RAM switcher, and storage cleanup dialog
    └── theme/
        ├── Color.kt, Theme.kt, Type.kt  # Dark/Light slate & indigo Material3 styling
```

### Supporting Project Files Created
- `build.gradle.kts` & `settings.gradle.kts`: Root Gradle build scripts.
- `gradle/libs.versions.toml`: Version catalog managing AGP 8.4.2, Kotlin 2.0.0, Compose BOM, OkHttp, Commons Compress.
- `gradle/wrapper/gradle-wrapper.properties` & `gradlew`: Gradle 8.7 wrapper configuration and execution script.
- `app/build.gradle.kts`: Configured with ABI splits, Java 17 compatibility, ProGuard rules, and local AAR linkage.
- `app/src/test/`: Unit test suite covering `WavExporterTest` (WAV header bytes, sample clamping) and `ModelInfoTest` (file verification).
- `compile_in_colab.ipynb`: Jupyter notebook tailored for building the APK on Google Colab.
- `.github/workflows/build.yml`: Automated CI workflow for building and uploading the APK on GitHub Actions.

---

## 4. Comprehensive Breakdown of Issues Encountered

During the compilation and testing phase, multiple compounding issues occurred across the local machine and Google Colab:

### Issue 1: Missing Gradle Wrapper Jar
- **Symptom:** Running `./gradlew assembleDebug` immediately threw:
  `ClassNotFoundException: org.gradle.wrapper.GradleWrapperMain`.
- **Cause:** When initializing the repository, only `gradle-wrapper.properties` was generated, but the binary `gradle-wrapper.jar` was missing.
- **Resolution:** Downloaded the official Gradle 8.7 wrapper jar into `gradle/wrapper/` and added a self-downloading fallback in `gradlew`.

### Issue 2: Conflicting ABI Configurations
- **Symptom:** Gradle configuration failed with:
  `Conflicting configuration : 'armeabi-v7a,arm64-v8a,x86_64' in ndk abiFilters cannot be present when splits abi filters are set`.
- **Cause:** In Android Gradle Plugin, you cannot have both `ndk.abiFilters` and `splits.abi` enabled simultaneously.
- **Resolution:** Removed the redundant `ndk { ... }` block from `defaultConfig` in `app/build.gradle.kts`.

### Issue 3: Missing Android SDK (`sdk.dir`)
- **Symptom:** Gradle reported `SDK location not found. Define a valid SDK location with an ANDROID_HOME environment variable or by setting sdk.dir in local.properties`.
- **Cause:** The local machine was a fresh Linux installation without the Android SDK installed.
- **Resolution:** Provided commands to download Google's commandline-tools and install `platforms;android-34` and `build-tools;34.0.0`, and created `local.properties` with `sdk.dir=/home/phoenix/Android/Sdk`.

### Issue 4: Sherpa-ONNX Dependency Not on Maven Central
- **Symptom:** Gradle reported `Could not find com.k2fsa.sherpa.onnx:sherpa-onnx:1.10.30`.
- **Cause:** The official Sherpa-ONNX project does not publish its Android SDK to Maven Central; it distributes precompiled `.aar` files via GitHub Releases.
- **Resolution:** Downloaded `sherpa-onnx-1.13.7.aar` (47 MB, containing native C++ binaries for `arm64-v8a`, `x86_64`, `armeabi-v7a`, `x86`) into `app/libs/` and updated `app/build.gradle.kts` to `implementation(files("libs/sherpa-onnx-1.13.7.aar"))`.

### Issue 5: Local Machine Freeze & 2+ Hour Hang (Crucial Issue)
- **Symptom:** Running `./gradlew assembleDebug` locally took more than 2 hours and caused the laptop to freeze completely, requiring a hard reboot.
- **Root Causes:**
  1. **RAM Starvation & Swap Thrashing:** Modern Android builds using Kotlin 2.0, Compose Compiler, and AGP require at least 8–12 GB of free physical RAM. On machines with 4–8 GB of RAM, the Gradle daemon and Kotlin daemon exceeded physical memory, causing Linux to aggressively thrash the disk swap space and freeze the OS.
  2. **Java 24 vs. Java 21 Interference:** The machine's default Java was Java 24 (non-LTS/early-access), which produced native-access warnings and JVM compatibility mismatches with Gradle 8.7.

### Issue 6: Google Colab Hang at ~59% (Crucial Issue)
- **Symptom:** Running the build in Google Colab also hung for a long time at ~59% until the session timed out and disconnected.
- **Root Cause:**
  - **`stripDebugDebugSymbols` Task Stalling on ONNX:** The task at ~59% in Android builds is `stripDebugDebugSymbols`. Because `sherpa-onnx-1.13.7.aar` contains a massive 25 MB native C++ library (`libonnxruntime.so`), AGP called `llvm-strip` without debug symbol configurations. The tool entered an intensive CPU/memory loop trying to analyze and strip symbols across all ABIs.
- **Fix Implemented in Code:**
  1. Added `packaging { jniLibs { keepDebugSymbols += "**/*.so" } }` to `app/build.gradle.kts` to completely bypass the native stripping bottleneck.
  2. Restricted ABI splits to `arm64-v8a` only, reducing the work by 66%.
  3. Tuned `gradle.properties` with `-Xmx4096m` and `-XX:+UseParallelGC`.

### Issue 7: Gradle Daemon Registry Corruption
- **Symptom:** `Could not read cache value from '/home/phoenix/.gradle/daemon/8.7/registry.bin'`.
- **Cause:** The hard reboot during the local freeze left Gradle's binary daemon registry file corrupted on disk.
- **Resolution:** Deleted `~/.gradle/daemon/8.7/registry.bin` and associated lock files.

### Issue 8: Colab Task Name Mismatch
- **Symptom:** Colab failed with `Task 'assembleArm64-v8aDebug' not found`.
- **Cause:** In Android Gradle with ABI splits, the task name is `assembleDebug` (which internally produces `app-arm64-v8a-debug.apk`), not `assembleArm64-v8aDebug`.

---

## 5. Architectural Lessons & Takeaways for the Clean Restart

If you are restarting the project to build cleanly and reliably:

1. **Avoid Heavy Local Compilation on Resource-Constrained Machines:**
   - Compiling Jetpack Compose + C++ native JNI libraries locally is extremely heavy on CPU and RAM.
   - **Recommended Approach:** Use **GitHub Actions** or a remote cloud builder for compiling APKs. GitHub Actions provides free 4-core runners with 16 GB of RAM, pre-installed Android SDK/NDK, and takes ~2 minutes to output a downloadable `.apk`.
2. **Always Bypass Native Stripping on Pre-Compiled ML Libraries:**
   - Any project using precompiled ONNX, TensorFlow Lite, or Sherpa-ONNX `.so` files must include:
     ```kotlin
     android {
         packaging {
             jniLibs {
                 keepDebugSymbols += "**/*.so"
             }
         }
     }
     ```
   - Failing to include this will cause Gradle to stall on `stripDebugDebugSymbols`.
3. **Target Single Architecture (`arm64-v8a`) During Development:**
   - Do not compile multi-ABI splits (`x86`, `x86_64`, `armeabi-v7a`) during development. All modern physical Android phones use `arm64-v8a`.
4. **Alternative UI Consideration (Compose vs. Classic Views):**
   - Jetpack Compose adds noticeable compilation overhead (Compose Compiler plugin, Compose BOM resolution, dexing).
   - For an "ultra-lightweight interface where the model takes most of the RAM", using **standard Android XML Views** or **minimal Material3 components** reduces both compilation memory and runtime APK size compared to full Compose.
5. **Alternative Framework Consideration (Flutter with Pre-built Plugin):**
   - The Flutter community has a pre-packaged plugin: `flutter_kokoro_tts`.
   - While Flutter adds a ~15 MB base engine overhead to the APK, its build toolchain is often smoother for developers who do not want to manage NDK and raw Gradle configurations directly.

---

## 6. Checklist & Action Plan for Gemini Web / Next Agent

When presenting this to Gemini Web or planning the next clean attempt, here is the recommended roadmap:

- [ ] **Step 1: Choose Build Strategy**
  - *Recommendation:* Commit the project to a GitHub repository and let **GitHub Actions** build the APK. This completely eliminates laptop freezes and Colab timeouts.
- [ ] **Step 2: Clean Gradle Setup**
  - Keep the proven `SherpaKokoroTtsEngine.kt`, `ModelDownloader.kt`, and `TarBz2Extractor.kt` logic.
  - Retain `keepDebugSymbols += "**/*.so"` and `arm64-v8a` targeting in `app/build.gradle.kts`.
- [ ] **Step 3: Model Storage & Downloading**
  - Verify download URLs:
    - Multi-lingual v1.0: `https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/kokoro-multi-lang-v1_0.tar.bz2`
    - English fp32: `https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/kokoro-en-v0_19.tar.bz2`
  - Internal destination: `File(context.filesDir, "models")`.
- [ ] **Step 4: Memory Verification**
  - Ensure `offlineTts.release()` and `System.gc()` remain strictly executed whenever models switch.
