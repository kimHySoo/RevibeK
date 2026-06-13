package com.ssafy.revibek.analysis.service;

import com.ssafy.revibek.analysis.dto.AnalyzeResponseDto;
import com.ssafy.revibek.song.dto.SongDto;
import com.ssafy.revibek.youtube.dto.YoutubeVideoDto;

public interface AnalysisService {
    AnalyzeResponseDto analyze(SongDto song);
    void analyzeAndSave(SongDto song);
    void analyzeAndSave(YoutubeVideoDto video);
    void upsertSongFromAnalysis(String youtubeId, String youtubeUrl, String title, AnalyzeResponseDto response);

    /**
     * songs 테이블에서 thumbnail_url이 비어있는 곡들에 대해 YouTube API로 썸네일/조회수/좋아요수를 채운다.
     * API로 조회되지 않으면 해당 곡은 그대로 둔다.
     */
    int fillMissingYoutubeStats();
}