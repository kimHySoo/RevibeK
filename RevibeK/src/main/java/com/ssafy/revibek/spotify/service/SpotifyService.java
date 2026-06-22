package com.ssafy.revibek.spotify.service;

public interface SpotifyService {

    /**
     * era가 "미분류"인 곡을 Spotify에서 검색해 발매일 기준으로 era, generation, released_at을 채운다.
     * 검색 실패 시 해당 곡은 현상태를 유지한다.
     *
     * @return 갱신된 곡 수
     */
    int fillEraGeneration();
}
