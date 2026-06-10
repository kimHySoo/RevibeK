package com.ssafy.revibek.radio.ai;

import com.ssafy.revibek.radio.dto.RadioRequestDto;
import com.ssafy.revibek.radio.dto.RadioCreateRequestDto;
import com.ssafy.revibek.radio.dto.RecommendedSongResponseDto;
import com.ssafy.revibek.song.dto.SongDto;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Component
public class AiDjPromptBuilder {

    public String build(RadioCreateRequestDto request, List<RecommendedSongResponseDto> songs) {
        String songList = IntStream.range(0, songs.size())
                .mapToObj(i -> {
                    RecommendedSongResponseDto song = songs.get(i);
                    return (i + 1) + ". " + song.getArtist() + " - " + song.getTitle()
                        + " (" + safe(song.getEra()) + ", " + safe(song.getGenre()) + ")";
                })
                .collect(Collectors.joining("\n"));

        return buildPrompt(
            safe(request.getMood()),
            safe(request.getStory()),
            safe(request.getEra()),
            safe(request.getGenre()),
            songList.isBlank() ? "추천곡 없음" : songList
        );
    }

    public String build(RadioRequestDto request, List<SongDto> songs) {
        String songList = IntStream.range(0, songs.size())
                .mapToObj(i -> {
                    SongDto song = songs.get(i);
                    return (i + 1) + ". " + song.getArtist() + " - " + song.getTitle();
                })
                .collect(Collectors.joining("\n"));

        return buildPrompt(
            safe(request.getMood()),
            safe(request.getStory()),
            firstNonBlank(request.getEra(), request.getGeneration()),
            safe(request.getGenre()),
            songList.isBlank() ? "추천곡 없음" : songList
        );
    }

    private String buildPrompt(String mood, String story, String era, String genre, String songList) {
        return """
            너는 2·3세대 K-POP 전문 라디오 DJ다.
            사용자의 감정과 사연, 선호 시대/장르, 추천곡 목록을 바탕으로 따뜻하고 자연스러운 라디오 오프닝 멘트를 작성해라.

            시대별 톤:
            - 2세대 또는 00s: 2004년~2011년 전후 K-POP. 동방신기, 빅뱅, 소녀시대, 원더걸스, 카라, 2NE1, 샤이니, 2PM이 떠오르는 선명한 후렴, 무대 감성, 향수, 에너지 키워드를 자연스럽게 사용
            - 3세대 또는 10s: 2012년~2017년 전후 K-POP. EXO, BTS, TWICE, BLACKPINK, Red Velvet, SEVENTEEN, 여자친구가 떠오르는 감정선, 서사, 청춘, 학창시절 키워드를 자연스럽게 사용

            조건:
            - 한국어로 작성
            - 3~5문장
            - 너무 과장하지 않기
            - 추천곡 제목과 아티스트를 자연스럽게 1~2개 언급
            - 사용자의 감정을 공감하는 문장 포함
            - 라디오 DJ처럼 자연스럽게 말하기
            - 저작권 문제가 생기지 않도록 가사 직접 인용 금지

            사용자 감정:
            %s

            사용자 사연:
            %s

            선호 시대:
            %s

            선호 장르:
            %s

            추천곡 목록:
            %s

            출력 형식:
            라디오 DJ 멘트만 출력해라.
            """.formatted(mood, story, era, genre, songList);
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "미지정" : value.trim();
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first.trim();
        }
        if (second != null && !second.isBlank()) {
            return second.trim();
        }
        return "미지정";
    }
}