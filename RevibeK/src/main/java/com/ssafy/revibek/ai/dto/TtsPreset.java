package com.ssafy.revibek.ai.dto;

public enum TtsPreset {
    KOREAN_FEMALE_SOFT("ko-KR", "ko-KR-Chirp3-HD-Vindemiatrix", 0.92, null, "MP3"),
    KOREAN_FEMALE_NORMAL("ko-KR", "ko-KR-Chirp3-HD-Vindemiatrix", 1.00, null, "MP3"),
    KOREAN_FEMALE_FAST("ko-KR", "ko-KR-Chirp3-HD-Vindemiatrix", 1.08, null, "MP3");

    public final String languageCode;
    public final String voiceName;
    public final Double speakingRate;
    public final Double pitch;
    public final String audioEncoding;

    TtsPreset(String languageCode, String voiceName, Double speakingRate, Double pitch, String audioEncoding) {
        this.languageCode = languageCode;
        this.voiceName = voiceName;
        this.speakingRate = speakingRate;
        this.pitch = pitch;
        this.audioEncoding = audioEncoding;
    }

    public static TtsPreset from(String value) {
        if (value == null || value.isBlank()) {
            return KOREAN_FEMALE_NORMAL;
        }
        return TtsPreset.valueOf(value.trim().toUpperCase());
    }
}
