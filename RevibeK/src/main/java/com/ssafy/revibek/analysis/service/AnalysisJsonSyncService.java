package com.ssafy.revibek.analysis.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.revibek.analysis.dto.AnalyzeResponseDto;
import com.ssafy.revibek.youtube.mapper.YoutubeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisJsonSyncService {

    @Value("${fastapi.project.path:../RevibeK_AI}")
    private String fastApiProjectPath;

    private final AnalysisService analysisService;
    private final YoutubeMapper youtubeMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * RevibeK_AI/downloads 폴더의 분석 결과 JSON 파일을 읽어 songs 테이블에 반영하고,
     * youtube_videos_raw.is_analyzed 를 1로 갱신한다.
     */
    public int syncFromDownloads() {
        File downloadsDir = new File(fastApiProjectPath, "downloads");
        File[] files = downloadsDir.listFiles((dir, name) -> name.endsWith(".json"));

        if (files == null || files.length == 0) {
            log.info("[JSON 동기화] 대상 파일 없음: {}", downloadsDir.getAbsolutePath());
            return 0;
        }

        int syncedCount = 0;
        for (File file : files) {
            try {
                AnalyzeResponseDto response = objectMapper.readValue(file, AnalyzeResponseDto.class);

                if (!"COMPLETED".equals(response.getStatus()) && !"MOCK".equals(response.getStatus())) {
                    continue;
                }

                String youtubeId = response.getYoutubeVideoId();
                String youtubeUrl = "https://www.youtube.com/watch?v=" + youtubeId;

                analysisService.upsertSongFromAnalysis(youtubeId, youtubeUrl, response.getTitle(), response);
                youtubeMapper.updateVideoAnalyzed(youtubeId, 1);
                syncedCount++;
            } catch (IOException e) {
                log.error("[JSON 동기화] 파일 읽기 실패: {} - {}", file.getName(), e.getMessage());
            }
        }

        log.info("[JSON 동기화] 완료: {}개 파일 반영", syncedCount);
        return syncedCount;
    }
}
