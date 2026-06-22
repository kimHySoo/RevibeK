package com.ssafy.revibek.qdrant;

import com.ssafy.revibek.song.dto.SongDto;

import java.util.Map;

public class SongVectorUtil {

    private static final Map<String, Integer> KEY_INDEX = Map.ofEntries(
        Map.entry("C", 0), Map.entry("C#", 1), Map.entry("D", 2),
        Map.entry("D#", 3), Map.entry("E", 4), Map.entry("F", 5),
        Map.entry("F#", 6), Map.entry("G", 7), Map.entry("G#", 8),
        Map.entry("A", 9), Map.entry("A#", 10), Map.entry("B", 11)
    );

    // 9차원 벡터 반환: 분석값이 없으면 null
    public static float[] toVector(SongDto song) {
        if (song.getBpm() == null || song.getEnergy() == null) return null;

        int keyIdx = KEY_INDEX.getOrDefault(song.getMusicalKey(), 0);
        double keyRad = keyIdx * Math.PI / 6.0;

        return new float[]{
            clamp(song.getBpm().floatValue() / 300f),
            clamp(song.getEnergy().floatValue()),
            clamp(song.getDanceability().floatValue()),
            clamp((song.getLoudness().floatValue() + 60f) / 60f),
            (float) Math.sin(keyRad),
            (float) Math.cos(keyRad),
            "minor".equalsIgnoreCase(song.getMusicalScale()) ? 1f : 0f,
            clamp(song.getSpectralCentroid() != null ? song.getSpectralCentroid().floatValue() / 8000f : 0f),
            clamp(song.getZeroCrossingRate() != null ? song.getZeroCrossingRate().floatValue() : 0f)
        };
    }

    private static float clamp(float v) {
        return Math.max(0f, Math.min(1f, v));
    }
}
