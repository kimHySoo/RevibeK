package com.ssafy.revibek.analysis.controller;

import com.ssafy.revibek.analysis.dto.AnalyzeResponseDto;
import com.ssafy.revibek.analysis.service.AnalysisJsonSyncService;
import com.ssafy.revibek.analysis.service.AnalysisService;
import com.ssafy.revibek.song.dto.SongDto;
import com.ssafy.revibek.song.service.SongService;
import com.ssafy.revibek.youtube.mapper.YoutubeMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/analysis")
@Tag(name = "Analysis", description = "FastAPI 음악 분석 API")
@RequiredArgsConstructor
public class AnalysisController {

    private final AnalysisService analysisService;
    private final SongService songService;
    private final YoutubeMapper youtubeMapper;
    private final AnalysisJsonSyncService analysisJsonSyncService;

    // 단건 분석
    @PostMapping("/{songId}")
    @Operation(summary = "노래 단건 분석", description = "FastAPI에 분석 요청 후 결과 저장")
    public ResponseEntity<?> analyzeSong(@PathVariable String songId) {
        try {
            SongDto song = songService.getSongById(songId);
            if (song == null)
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                     .body("노래를 찾을 수 없습니다.");
            AnalyzeResponseDto response = analysisService.analyze(song);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body(e.getMessage());
        }
    }

    // downloads 폴더의 분석 결과 JSON을 DB에 동기화
    @PostMapping("/sync-from-json")
    @Operation(summary = "분석 결과 JSON DB 동기화", description = "RevibeK_AI/downloads의 분석 결과 JSON을 songs/youtube_videos_raw에 반영")
    public ResponseEntity<?> syncFromJson() {
        try {
            int count = analysisJsonSyncService.syncFromDownloads();
            return ResponseEntity.ok(count + "개 파일을 DB에 동기화했습니다.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body(e.getMessage());
        }
    }

    // 썸네일/조회수/좋아요수 미보유 곡 채우기
    @PostMapping("/fill-youtube-stats")
    @Operation(summary = "유튜브 통계 채우기", description = "thumbnail_url이 없는 곡에 대해 YouTube API로 썸네일/조회수/좋아요수를 채움 (조회 실패 시 현상태 유지)")
    public ResponseEntity<?> fillYoutubeStats() {
        try {
            int count = analysisService.fillMissingYoutubeStats();
            return ResponseEntity.ok(count + "개 곡의 YouTube 통계를 채웠습니다.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body(e.getMessage());
        }
    }

    // 미분석 영상 일괄 분석
    @PostMapping("/batch")
    @Operation(summary = "미분석 영상 일괄 분석", description = "youtube_videos_raw에서 is_analyzed=0인 영상을 FastAPI에 분석 요청")
    public ResponseEntity<?> analyzeAll() {
        try {
            youtubeMapper.findPendingVideos().forEach(analysisService::analyzeAndSave);
            return ResponseEntity.ok("일괄 분석 완료");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body(e.getMessage());
        }
    }
}