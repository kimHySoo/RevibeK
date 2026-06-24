package com.ssafy.revibek.explore.service;

import com.ssafy.revibek.analysis.client.FastApiClient;
import com.ssafy.revibek.analysis.dto.AnalyzeRequestDto;
import com.ssafy.revibek.analysis.dto.AnalyzeResponseDto;
import com.ssafy.revibek.explore.dto.ExploreResponseDto;
import com.ssafy.revibek.qdrant.QdrantService;
import com.ssafy.revibek.song.dto.SongDto;
import com.ssafy.revibek.song.service.SongService;
import com.ssafy.revibek.youtube.service.YoutubeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExploreService {

    private final SongService songService;
    private final FastApiClient fastApiClient;
    private final QdrantService qdrantService;
    private final YoutubeService youtubeService;

    private static final Pattern YOUTUBE_ID_PATTERN = Pattern.compile(
        "(?:v=|youtu\\.be/|/shorts/)([a-zA-Z0-9_-]{11})"
    );

    // 주거용 프록시 트래픽 비용 폭증을 막기 위해, 이 길이 이상인 영상은 다운로드를 시도하지 않음 (docs/answer/block.md)
    private static final int MAX_DURATION_SECONDS = 300;

    public ExploreResponseDto explore(String youtubeUrl, int limit) {
        String youtubeId = extractYoutubeId(youtubeUrl);

        SongDto song = songService.getSongByYoutubeId(youtubeId);
        boolean isNew = false;

        if (song == null) {
            Integer durationSeconds = youtubeService.fetchDurationSeconds(youtubeId);
            if (durationSeconds != null && durationSeconds >= MAX_DURATION_SECONDS) {
                throw new IllegalArgumentException(
                    "재생시간이 " + MAX_DURATION_SECONDS + "초 이상인 영상은 분석할 수 없습니다: " + youtubeUrl);
            }
            song = analyzeAndInsert(youtubeId, youtubeUrl);
            isNew = true;
        }

        // Qdrant에 없을 수 있으므로 upsert 후 검색
        qdrantService.createCollectionIfNotExists();
        qdrantService.upsertSong(song);

        List<String> similarIds = qdrantService.searchSimilar(song.getId(), limit);
        List<SongDto> similar = similarIds.stream()
            .map(songService::getSongById)
            .filter(s -> s != null)
            .toList();

        if (similar.isEmpty()) {
            String currentSongId = song.getId();
            similar = songService.getRecommendSongs().stream()
                .filter(s -> s != null && !s.getId().equals(currentSongId))
                .limit(limit)
                .toList();
        }

        return new ExploreResponseDto(song, similar, isNew);
    }

    private SongDto analyzeAndInsert(String youtubeId, String youtubeUrl) {
        AnalyzeRequestDto request = new AnalyzeRequestDto(youtubeId, youtubeUrl, "", 0);
        AnalyzeResponseDto response = fastApiClient.analyze(request);

        if (response == null || !isUsableAnalysis(response.getStatus())) {
            String msg = response != null ? response.getMessage() : "FastAPI 응답 없음";
            throw new RuntimeException("분석 실패: " + msg);
        }

        SongDto song = SongDto.builder()
            .youtubeId(youtubeId)
            .youtubeUrl(youtubeUrl)
            .title(response.getTitle() != null ? response.getTitle() : youtubeId)
            .artist("미분류")
            .genre("미분류")
            .era("미분류")
            .type("song")
            .durationSeconds(response.getDurationSeconds())
            .bpm(response.getBpm())
            .energy(response.getEnergy())
            .danceability(response.getDanceability())
            .loudness(response.getLoudness())
            .musicalKey(response.getMusicalKey())
            .musicalScale(response.getMusicalScale())
            .isAnalyzed(1)
            .build();

        songService.registerSong(song);
        log.info("새 곡 분석 및 저장: {}", song.getTitle());

        return songService.getSongByYoutubeId(youtubeId);
    }

    private String extractYoutubeId(String url) {
        Matcher matcher = YOUTUBE_ID_PATTERN.matcher(url);
        if (matcher.find()) return matcher.group(1);
        throw new IllegalArgumentException("유효하지 않은 YouTube URL: " + url);
    }

    private boolean isUsableAnalysis(String status) {
        return "COMPLETED".equals(status) || "MOCK".equals(status) || "FALLBACK".equals(status);
    }
}
