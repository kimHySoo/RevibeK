package com.ssafy.revibek.analysis.service;

import com.ssafy.revibek.analysis.client.FastApiClient;
import com.ssafy.revibek.analysis.dto.AnalyzedSongDto;
import com.ssafy.revibek.analysis.dto.AnalyzeRequestDto;
import com.ssafy.revibek.analysis.dto.AnalyzeResponseDto;
import com.ssafy.revibek.analysis.mapper.AnalyzedSongDao;
import com.ssafy.revibek.embedding.dto.EmbeddingSongDto;
import com.ssafy.revibek.embedding.mapper.EmbeddingSongDao;
import com.ssafy.revibek.song.dto.SongDto;
import com.ssafy.revibek.song.service.SongService;
import com.ssafy.revibek.youtube.dto.YoutubeVideoDto;
import com.ssafy.revibek.youtube.dto.YoutubeVideoStatsDto;
import com.ssafy.revibek.youtube.mapper.YoutubeMapper;
import com.ssafy.revibek.youtube.service.YoutubeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisServiceImpl implements AnalysisService {

    private final FastApiClient fastApiClient;
    private final SongService songService;
    private final YoutubeMapper youtubeMapper;
    private final YoutubeService youtubeService;
    private final AnalyzedSongDao analyzedSongDao;
    private final EmbeddingSongDao embeddingSongDao;

    @Value("${qdrant.collection:revibek_songs}")
    private String audioCollection;

    private static final String AUDIO_EMBEDDING_TYPE = "AUDIO_9D";

    @Override
    public AnalyzeResponseDto analyzeByUrl(String youtubeUrl) {
        String ytId = extractYoutubeId(youtubeUrl);
        if (ytId == null) {
            throw new IllegalArgumentException("유효하지 않은 YouTube URL: " + youtubeUrl);
        }
        AnalyzeRequestDto request = new AnalyzeRequestDto(ytId, youtubeUrl, "", 0);
        return fastApiClient.analyze(request);
    }

    @Override
    public AnalyzeResponseDto analyzeByUrlAndPersist(String youtubeUrl) {
        AnalyzeResponseDto response = analyzeByUrl(youtubeUrl);
        if (response == null || !"COMPLETED".equals(response.getStatus())) {
            // MOCK/FAILED/SKIPPED 결과는 저장하지 않는다 (가짜·미완 분석값이 DB에 남는 것을 방지).
            return response;
        }

        String ytId = extractYoutubeId(youtubeUrl);
        SongDto song = songService.getSongByYoutubeId(ytId);
        if (song == null) {
            log.info("[Analysis] songs에 없는 URL이라 analyzed_songs/embedding_songs 저장을 건너뜀: {}", ytId);
            return response;
        }

        persistAnalyzedSong(song, response);
        if (response.getEmbedding() != null && !response.getEmbedding().isEmpty()) {
            persistEmbeddingSong(song, response.getEmbedding());
        }
        return response;
    }

    private void persistAnalyzedSong(SongDto song, AnalyzeResponseDto response) {
        var rawVideo = youtubeMapper.findVideoByVideoId(song.getYoutubeId());
        if (rawVideo == null) {
            // youtube_videos_raw.channel_id가 NOT NULL FK라 채널 수집 이력이 없는 곡은
            // analyzed_songs를 저장할 수 없다 (docs/answer/answer13.md 2.4-1 참고).
            log.info("[Analysis] youtube_videos_raw 이력이 없어 analyzed_songs 저장을 건너뜀: songId={}", song.getId());
            return;
        }

        analyzedSongDao.upsert(AnalyzedSongDto.builder()
            .youtubeVideoRawId(rawVideo.getId())
            .youtubeVideoId(song.getYoutubeId())
            .songId(song.getId())
            .status(response.getStatus())
            .message(response.getMessage())
            .durationSeconds((double) response.getDurationSeconds())
            .bpm(response.getBpm())
            .energy(response.getEnergy())
            .danceability(response.getDanceability())
            .loudness(response.getLoudness())
            .musicalKey(response.getMusicalKey())
            .musicalScale(response.getMusicalScale())
            .spectralCentroid(response.getSpectralCentroid())
            .zeroCrossingRate(response.getZeroCrossingRate())
            .analyzedAt(LocalDateTime.now())
            .build());
    }

    private void persistEmbeddingSong(SongDto song, List<Float> embedding) {
        embeddingSongDao.upsert(EmbeddingSongDto.builder()
            .songId(song.getId())
            .embeddingType(AUDIO_EMBEDDING_TYPE)
            .modelName("SongVectorUtil/FastAPI embedding_service")
            .dimension(embedding.size())
            .sourceText(null)
            .vector(embedding)
            .qdrantCollection(audioCollection)
            .qdrantPointId(song.getId())
            .isIndexed(false)
            .indexedAt(null)
            .build());
    }

    private String extractYoutubeId(String url) {
        if (url == null || url.isBlank()) return null;
        java.util.regex.Matcher m = java.util.regex.Pattern
            .compile("(?:youtu\\.be/|youtube\\.com/(?:watch\\?v=|embed/|shorts/))([a-zA-Z0-9_-]{11})")
            .matcher(url);
        if (m.find()) return m.group(1);
        if (url.matches("[a-zA-Z0-9_-]{11}")) return url;
        return null;
    }

    @Override
    public AnalyzeResponseDto analyze(SongDto song) {
        if (song.getYoutubeId() != null && song.getYoutubeId().startsWith("dummy")) {
            AnalyzeResponseDto response = new AnalyzeResponseDto();
            response.setYoutubeVideoId(song.getYoutubeId());
            response.setTitle(song.getTitle());
            response.setStatus("SKIPPED");
            response.setMessage("더미 데이터(youtube_id=" + song.getYoutubeId() + ")이므로 분석을 건너뜁니다.");
            return response;
        }

        AnalyzeRequestDto request = new AnalyzeRequestDto(
            song.getYoutubeId(),
            song.getYoutubeUrl(),
            song.getTitle(),
            song.getDurationSeconds() != null ? song.getDurationSeconds() : 0
        );
        return fastApiClient.analyze(request);
    }

    @Override
    public void analyzeAndSave(SongDto song) {
        AnalyzeResponseDto response = analyze(song);

        if (response == null) {
            System.out.println("[Analysis] FastAPI 응답 없음: " + song.getTitle());
            return;
        }

        switch (response.getStatus()) {
            case "COMPLETED", "MOCK" -> {
                // 분석 결과를 songs 테이블에 반영
                song.setBpm(response.getBpm());
                song.setEnergy(response.getEnergy());
                song.setDanceability(response.getDanceability());
                song.setLoudness(response.getLoudness());
                song.setMusicalKey(response.getMusicalKey());
                song.setMusicalScale(response.getMusicalScale());
                song.setSpectralCentroid(response.getSpectralCentroid());
                song.setZeroCrossingRate(response.getZeroCrossingRate());
                songService.modifySong(song);
                System.out.println("[Analysis] 완료: " + song.getTitle()
                    + " (source=" + response.getSource() + ")");
            }
            case "SKIPPED" -> {
                System.out.println("[Analysis] 스킵: " + song.getTitle()
                    + " (" + response.getMessage() + ")");
            }
            case "FAILED" -> {
                System.out.println("[Analysis] 실패: " + song.getTitle()
                    + " (" + response.getMessage() + ")");
            }
            default -> {
                System.out.println("[Analysis] 알 수 없는 상태: " + response.getStatus());
            }
        }
    }

    @Override
    public void analyzeAndSave(YoutubeVideoDto video) {
        AnalyzeRequestDto request = new AnalyzeRequestDto(
            video.getVideoId(),
            video.getVideoUrl(),
            video.getTitle(),
            video.getDurationSeconds() != null ? video.getDurationSeconds() : 0
        );
        AnalyzeResponseDto response = fastApiClient.analyze(request);

        if (response == null) {
            System.out.println("[Analysis] FastAPI 응답 없음: " + video.getTitle());
            return;
        }

        switch (response.getStatus()) {
            case "COMPLETED", "MOCK" -> {
                upsertSongFromAnalysis(video.getVideoId(), video.getVideoUrl(), video.getTitle(), response);
                youtubeMapper.updateVideoAnalyzed(video.getVideoId(), 1);
                System.out.println("[Analysis] 완료: " + video.getTitle()
                    + " (source=" + response.getSource() + ")");
            }
            case "SKIPPED" -> {
                System.out.println("[Analysis] 스킵: " + video.getTitle()
                    + " (" + response.getMessage() + ")");
            }
            case "FAILED" -> {
                System.out.println("[Analysis] 실패: " + video.getTitle()
                    + " (" + response.getMessage() + ")");
            }
            default -> {
                System.out.println("[Analysis] 알 수 없는 상태: " + response.getStatus());
            }
        }
    }

    @Override
    public void upsertSongFromAnalysis(String youtubeId, String youtubeUrl, String title, AnalyzeResponseDto response) {
        SongDto song = songService.getSongByYoutubeId(youtubeId);
        YoutubeVideoStatsDto stats = youtubeService.fetchVideoStats(youtubeId);

        if (song == null) {
            SongDto.SongDtoBuilder builder = SongDto.builder()
                .youtubeId(youtubeId)
                .youtubeUrl(youtubeUrl)
                .title(title)
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
                .spectralCentroid(response.getSpectralCentroid())
                .zeroCrossingRate(response.getZeroCrossingRate())
                .isAnalyzed(1);

            if (stats != null) {
                builder.thumbnailUrl(stats.getThumbnailUrl())
                    .viewCount(stats.getViewCount())
                    .likeCount(stats.getLikeCount());
            }

            songService.registerSong(builder.build());
        } else {
            song.setDurationSeconds(response.getDurationSeconds());
            song.setBpm(response.getBpm());
            song.setEnergy(response.getEnergy());
            song.setDanceability(response.getDanceability());
            song.setLoudness(response.getLoudness());
            song.setMusicalKey(response.getMusicalKey());
            song.setMusicalScale(response.getMusicalScale());
            song.setSpectralCentroid(response.getSpectralCentroid());
            song.setZeroCrossingRate(response.getZeroCrossingRate());
            song.setIsAnalyzed(1);

            if (stats != null) {
                song.setThumbnailUrl(stats.getThumbnailUrl());
                song.setViewCount(stats.getViewCount());
                song.setLikeCount(stats.getLikeCount());
            }

            songService.modifySong(song);
        }
    }

    @Override
    public int fillMissingYoutubeStats() {
        int updatedCount = 0;

        for (SongDto song : songService.getAllSongs()) {
            if (song.getThumbnailUrl() != null && !song.getThumbnailUrl().isEmpty()) {
                continue;
            }
            if (song.getYoutubeId() == null || song.getYoutubeId().startsWith("dummy")) {
                continue;
            }

            YoutubeVideoStatsDto stats = youtubeService.fetchVideoStats(song.getYoutubeId());
            if (stats == null) {
                continue;
            }

            song.setThumbnailUrl(stats.getThumbnailUrl());
            song.setViewCount(stats.getViewCount());
            song.setLikeCount(stats.getLikeCount());
            songService.modifySong(song);
            updatedCount++;
        }

        return updatedCount;
    }
}