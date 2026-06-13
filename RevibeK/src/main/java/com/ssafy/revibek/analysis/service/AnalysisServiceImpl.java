package com.ssafy.revibek.analysis.service;

import com.ssafy.revibek.analysis.client.FastApiClient;
import com.ssafy.revibek.analysis.dto.AnalyzeRequestDto;
import com.ssafy.revibek.analysis.dto.AnalyzeResponseDto;
import com.ssafy.revibek.song.dto.SongDto;
import com.ssafy.revibek.song.service.SongService;
import com.ssafy.revibek.youtube.dto.YoutubeVideoDto;
import com.ssafy.revibek.youtube.dto.YoutubeVideoStatsDto;
import com.ssafy.revibek.youtube.mapper.YoutubeMapper;
import com.ssafy.revibek.youtube.service.YoutubeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AnalysisServiceImpl implements AnalysisService {

    private final FastApiClient fastApiClient;
    private final SongService songService;
    private final YoutubeMapper youtubeMapper;
    private final YoutubeService youtubeService;

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