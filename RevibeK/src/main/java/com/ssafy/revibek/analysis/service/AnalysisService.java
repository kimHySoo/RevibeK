package com.ssafy.revibek.analysis.service;

import com.ssafy.revibek.analysis.dto.AnalyzeResponseDto;
import com.ssafy.revibek.song.dto.SongDto;
import com.ssafy.revibek.youtube.dto.YoutubeVideoDto;

public interface AnalysisService {
    AnalyzeResponseDto analyze(SongDto song);

    /**
     * YouTube URL을 직접 받아 FastAPI로 오디오를 분석하고 9D 임베딩을 반환한다.
     * DB 저장 및 Qdrant upsert는 하지 않는다 (실시간 검색 전용).
     */
    AnalyzeResponseDto analyzeByUrl(String youtubeUrl);

    /**
     * analyzeByUrl과 동일하게 분석하되, 분석 대상 곡이 songs 테이블에 이미 등록되어 있으면
     * analyzed_songs / embedding_songs(AUDIO_9D)에도 결과를 저장한다.
     * 곡이 DB에 없거나 분석 상태가 COMPLETED가 아니면 저장 없이 분석 결과만 반환한다.
     */
    AnalyzeResponseDto analyzeByUrlAndPersist(String youtubeUrl);

    void analyzeAndSave(SongDto song);
    void analyzeAndSave(YoutubeVideoDto video);
    void upsertSongFromAnalysis(String youtubeId, String youtubeUrl, String title, AnalyzeResponseDto response);

    /**
     * songs 테이블에서 thumbnail_url이 비어있는 곡들에 대해 YouTube API로 썸네일/조회수/좋아요수를 채운다.
     * API로 조회되지 않으면 해당 곡은 그대로 둔다.
     */
    int fillMissingYoutubeStats();
}