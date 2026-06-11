package com.ssafy.revibek.analysis.service;

import com.ssafy.revibek.analysis.client.FastApiClient;
import com.ssafy.revibek.analysis.dto.AnalyzeRequestDto;
import com.ssafy.revibek.analysis.dto.AnalyzeResponseDto;
import com.ssafy.revibek.song.dto.SongDto;
import com.ssafy.revibek.song.service.SongService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AnalysisServiceImpl implements AnalysisService {

    private final FastApiClient fastApiClient;
    private final SongService songService;

    @Override
    public AnalyzeResponseDto analyze(SongDto song) {
        AnalyzeRequestDto request = new AnalyzeRequestDto(
                song.getYoutubeId(),
                song.getYoutubeUrl(),
                song.getTitle(),
                song.getDurationSeconds()
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
            case "COMPLETED" -> {
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
            case "MOCK", "SKIPPED" -> {
                System.out.println("[Analysis] 저장 생략: " + song.getTitle()
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
}
