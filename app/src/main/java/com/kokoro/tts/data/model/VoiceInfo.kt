package com.kokoro.tts.data.model

enum class VoiceGender {
    FEMALE, MALE
}

enum class VoiceAccent {
    AMERICAN, BRITISH, INTERNATIONAL
}

data class VoiceInfo(
    val speakerId: Int,
    val name: String,
    val code: String,
    val gender: VoiceGender,
    val accent: VoiceAccent,
    val description: String
)

object KokoroVoices {

    val multiLingualVoices: List<VoiceInfo> = listOf(
        // American Female
        VoiceInfo(0, "Heart", "af_heart", VoiceGender.FEMALE, VoiceAccent.AMERICAN, "Warm & emotive (flagship)"),
        VoiceInfo(1, "Bella", "af_bella", VoiceGender.FEMALE, VoiceAccent.AMERICAN, "Lively & bright"),
        VoiceInfo(2, "Nicole", "af_nicole", VoiceGender.FEMALE, VoiceAccent.AMERICAN, "Crisp & professional"),
        VoiceInfo(3, "Aoede", "af_aoede", VoiceGender.FEMALE, VoiceAccent.AMERICAN, "Soft & melodic"),
        VoiceInfo(4, "Kore", "af_kore", VoiceGender.FEMALE, VoiceAccent.AMERICAN, "Calm & steady"),
        VoiceInfo(5, "Sarah", "af_sarah", VoiceGender.FEMALE, VoiceAccent.AMERICAN, "Clear narrator"),
        VoiceInfo(6, "Nova", "af_nova", VoiceGender.FEMALE, VoiceAccent.AMERICAN, "Modern & cheerful"),
        VoiceInfo(7, "Sky", "af_sky", VoiceGender.FEMALE, VoiceAccent.AMERICAN, "Gentle & airy"),
        VoiceInfo(8, "Alloy", "af_alloy", VoiceGender.FEMALE, VoiceAccent.AMERICAN, "Neutral & balanced"),
        VoiceInfo(9, "Jessica", "af_jessica", VoiceGender.FEMALE, VoiceAccent.AMERICAN, "Expressive"),
        VoiceInfo(10, "River", "af_river", VoiceGender.FEMALE, VoiceAccent.AMERICAN, "Smooth & deep"),

        // American Male
        VoiceInfo(11, "Michael", "am_michael", VoiceGender.MALE, VoiceAccent.AMERICAN, "Natural & friendly"),
        VoiceInfo(12, "Adam", "am_adam", VoiceGender.MALE, VoiceAccent.AMERICAN, "Deep & authoritative"),
        VoiceInfo(13, "Echo", "am_echo", VoiceGender.MALE, VoiceAccent.AMERICAN, "Crisp presenter"),
        VoiceInfo(14, "Eric", "am_eric", VoiceGender.MALE, VoiceAccent.AMERICAN, "Conversational"),
        VoiceInfo(15, "Fenrir", "am_fenrir", VoiceGender.MALE, VoiceAccent.AMERICAN, "Intense & dramatic"),
        VoiceInfo(16, "Liam", "am_liam", VoiceGender.MALE, VoiceAccent.AMERICAN, "Youthful & energetic"),
        VoiceInfo(17, "Onyx", "am_onyx", VoiceGender.MALE, VoiceAccent.AMERICAN, "Rich baritone"),
        VoiceInfo(18, "Puck", "am_puck", VoiceGender.MALE, VoiceAccent.AMERICAN, "Playful"),
        VoiceInfo(19, "Santa", "am_santa", VoiceGender.MALE, VoiceAccent.AMERICAN, "Jovial & deep"),

        // British Female
        VoiceInfo(20, "Alice", "bf_alice", VoiceGender.FEMALE, VoiceAccent.BRITISH, "Refined British RP"),
        VoiceInfo(21, "Emma", "bf_emma", VoiceGender.FEMALE, VoiceAccent.BRITISH, "Eloquent & clear"),
        VoiceInfo(22, "Isabella", "bf_isabella", VoiceGender.FEMALE, VoiceAccent.BRITISH, "Classic literary"),
        VoiceInfo(23, "Lily", "bf_lily", VoiceGender.FEMALE, VoiceAccent.BRITISH, "Bright & cheerful"),

        // British Male
        VoiceInfo(24, "George", "bm_george", VoiceGender.MALE, VoiceAccent.BRITISH, "Distinguished narrator"),
        VoiceInfo(25, "Fable", "bm_fable", VoiceGender.MALE, VoiceAccent.BRITISH, "Storyteller"),
        VoiceInfo(26, "Lewis", "bm_lewis", VoiceGender.MALE, VoiceAccent.BRITISH, "Calm & educated"),
        VoiceInfo(27, "Daniel", "bm_daniel", VoiceGender.MALE, VoiceAccent.BRITISH, "Contemporary British"),

        // International
        VoiceInfo(28, "Xiaobei", "zf_xiaobei", VoiceGender.FEMALE, VoiceAccent.INTERNATIONAL, "Mandarin female"),
        VoiceInfo(29, "Yunjian", "zm_yunjian", VoiceGender.MALE, VoiceAccent.INTERNATIONAL, "Mandarin male"),
        VoiceInfo(30, "Alpha", "jf_alpha", VoiceGender.FEMALE, VoiceAccent.INTERNATIONAL, "Japanese female"),
        VoiceInfo(31, "Kento", "jm_kento", VoiceGender.MALE, VoiceAccent.INTERNATIONAL, "Japanese male"),
        VoiceInfo(32, "Dora", "ef_dora", VoiceGender.FEMALE, VoiceAccent.INTERNATIONAL, "Spanish female"),
        VoiceInfo(33, "Alex", "em_alex", VoiceGender.MALE, VoiceAccent.INTERNATIONAL, "Spanish male"),
        VoiceInfo(34, "Siwis", "ff_siwis", VoiceGender.FEMALE, VoiceAccent.INTERNATIONAL, "French female"),
        VoiceInfo(35, "Alpha Hindi", "hf_alpha", VoiceGender.FEMALE, VoiceAccent.INTERNATIONAL, "Hindi female"),
        VoiceInfo(36, "Omega Hindi", "hm_omega", VoiceGender.MALE, VoiceAccent.INTERNATIONAL, "Hindi male")
    )

    val englishOnlyVoices: List<VoiceInfo> = multiLingualVoices.filter { 
        it.accent != VoiceAccent.INTERNATIONAL 
    }

    fun getVoicesForModel(modelType: ModelType): List<VoiceInfo> {
        return when (modelType) {
            ModelType.MULTI_LINGUAL_V1 -> multiLingualVoices
            ModelType.ENGLISH_FP32, ModelType.ENGLISH_INT8 -> englishOnlyVoices
        }
    }
}
