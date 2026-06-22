package com.ssafy.revibek.explore.service;

import com.ssafy.revibek.analysis.client.FastApiClient;
import com.ssafy.revibek.analysis.dto.AnalyzeRequestDto;
import com.ssafy.revibek.analysis.dto.AnalyzeResponseDto;
import com.ssafy.revibek.explore.dto.ExploreResponseDto;
import com.ssafy.revibek.qdrant.QdrantService;
import com.ssafy.revibek.song.dto.SongDto;
import com.ssafy.revibek.song.service.SongService;
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

    private static final Pattern YOUTUBE_ID_PATTERN = Pattern.compile(
        "(?:v=|youtu\\.be/|/shorts/)([a-zA-Z0-9_-]{11})"
    );

    public ExploreResponseDto explore(String youtubeUrl, int limit) {
        String youtubeId = extractYoutubeId(youtubeUrl);

        SongDto song = songService.getSongByYoutubeId(youtubeId);
        boolean isNew = false;

        if (song == null) {
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
