# Keep Sherpa-ONNX JNI classes
-keep class com.k2fsa.sherpa.onnx.** { *; }
-keepclassmembers class * {
    native <methods>;
}

# Keep OkHttp & Commons Compress
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.apache.commons.compress.**
