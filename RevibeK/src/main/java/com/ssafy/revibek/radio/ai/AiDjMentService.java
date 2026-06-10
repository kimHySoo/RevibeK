package com.ssafy.revibek.radio.ai;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.ssafy.revibek.radio.dto.RadioCreateRequestDto;
import com.ssafy.revibek.radio.dto.RecommendedSongResponseDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AiDjMentService {

    private final AiDjPromptBuilder promptBuilder;
    private final GmsClient gmsClient;

    public String createDjMent(RadioCreateRequestDto request, List<RecommendedSongResponseDto> songs) {
        String prompt = promptBuilder.build(request, songs);
        return gmsClient.generate(prompt)
            .filter(StringUtils::hasText)
            .orElseGet(() -> fallbackMent(request, songs));
    }

    private String fallbackMent(RadioCreateRequestDto request, List<RecommendedSongResponseDto> songs) {
        String era = safe(request.getEra());
        String genre = safe(request.getGenre());
        String mood = safe(request.getMood());
        String firstSong = songs.isEmpty()
            ? null
            : songs.get(0).getArtist() + "의 " + songs.get(0).getTitle();

        String eraPhrase = switch (normalizeEraLabel(era)) {
            case "2세대" -> "2004년부터 2011년 전후를 채운 2세대 K-POP 특유의 선명한 후렴과 무대 에너지";
            case "3세대" -> "2012년부터 2017년 전후를 지나온 3세대 K-POP의 청춘 감정선과 서사";
            default -> era + " K-POP 감성";
        };
        String artistPhrase = switch (normalizeEraLabel(era)) {
            case "2세대" -> "동방신기, 빅뱅, 소녀시대, 원더걸스, 카라, 2NE1, 샤이니, 2PM이 떠오르는";
            case "3세대" -> "EXO, BTS, TWICE, BLACKPINK, Red Velvet, SEVENTEEN, 여자친구가 떠오르는";
            default -> "그 시절 아티스트들이 떠오르는";
        };

        StringBuilder ment = new StringBuilder();
        ment.append("오늘은 ").append(mood).append(" 마음을 위해 ")
            .append(eraPhrase).append("가 느껴지는 ")
            .append(genre).append(" 곡들을 준비했습니다. ")
            .append(artistPhrase).append(" 장면들을 라디오처럼 천천히 꺼내볼게요. ");

        if (firstSong != null) {
            ment.append("먼저 ").append(firstSong)
                .append("처럼 익숙하면서도 다시 힘을 주는 노래로 시작해볼게요. ");
        } else {
            ment.append("아직 추천곡이 충분하지 않아도, 지금의 기분을 천천히 풀어낼 수 있는 라디오로 이어가볼게요. ");
        }

        ment.append("잠시 라디오에 기대어 쉬어가도 괜찮습니다.");
        return ment.toString();
    }

    private String normalizeEraLabel(String era) {
        if ("2".equals(era) || "2세대".equals(era) || "00s".equalsIgnoreCase(era)
            || "2000년대".equals(era) || era.contains("2000")) {
            return "2세대";
        }
        if ("3".equals(era) || "3세대".equals(era) || "10s".equalsIgnoreCase(era)
            || "2010년대".equals(era) || era.contains("2010")) {
            return "3세대";
        }
        return era;
    }

    private String safe(String value) {
        return StringUtils.hasText(value) ? value.trim() : "미지정";
    }
}
