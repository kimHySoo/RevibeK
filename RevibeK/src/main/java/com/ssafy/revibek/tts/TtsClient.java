package com.ssafy.revibek.tts;

public interface TtsClient {

    TtsResponseDto synthesize(String text);
}
