package com.ssafy.revibek.spotify.service;

import com.ssafy.revibek.song.dto.SongDto;
import com.ssafy.revibek.song.service.SongService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SpotifyServiceImpl implements SpotifyService {

    private static final String TOKEN_URL = "https://accounts.spotify.com/api/token";
    private static final String SEARCH_URL = "https://api.spotify.com/v1/search";

    private final SongService songService;
    private final RestTemplate restTemplate;

    @Value("${spotify.enabled:false}")
    private boolean enabled;

    @Value("${spotify.client-id:}")
    private String clientId;

    @Value("${spotify.client-secret:}")
    private String clientSecret;

    private String accessToken;
    private Instant tokenExpiresAt = Instant.MIN;

    @Override
    public int fillEraGeneration() {
        if (!enabled || !StringUtils.hasText(clientId) || !StringUtils.hasText(clientSecret)) {
            log.info("[Spotify] API disabled or credentials missing. Skip.");
            return 0;
        }

        int updatedCount = 0;
        for (SongDto song : songService.getAllSongs()) {
            if (!"미분류".equals(song.getEra())) {
                continue;
            }

            LocalDate releaseDate = searchReleaseDate(song.getTitle(), song.getArtist());
            if (releaseDate == null) {
                song.setEra("");
                songService.modifySong(song);
                continue;
            }

            int year = releaseDate.getYear();
            song.setEra(toEra(year));
            song.setGeneration(toGeneration(year));
            song.setReleasedAt(releaseDate);
            songService.modifySong(song);
            updatedCount++;
        }

        return updatedCount;
    }

    private LocalDate searchReleaseDate(String title, String artist) {
        try {
            String token = getAccessToken();
            if (token == null) {
                return null;
            }

            String query = StringUtils.hasText(artist) && !"미분류".equals(artist)
                ? title + " " + artist
                : title;

            String url = UriComponentsBuilder.fromUriString(SEARCH_URL)
                .queryParam("q", query)
                .queryParam("type", "track")
                .queryParam("limit", 1)
                .queryParam("market", "KR")
                .build()
                .encode()
                .toUriString();

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + token);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.exchange(
                url, HttpMethod.GET, new HttpEntity<>(headers), Map.class
            ).getBody();

            if (response == null) {
                return null;
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> tracks = (Map<String, Object>) response.get("tracks");
            if (tracks == null) {
                return null;
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> items = (List<Map<String, Object>>) tracks.get("items");
            if (items == null || items.isEmpty()) {
                return null;
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> album = (Map<String, Object>) items.get(0).get("album");
            if (album == null) {
                return null;
            }

            String releaseDate = (String) album.get("release_date");
            String precision = (String) album.get("release_date_precision");
            return parseReleaseDate(releaseDate, precision);
        } catch (Exception e) {
            log.warn("[Spotify] 검색 실패: {} - {}", title, e.getMessage());
            return null;
        }
    }

    private LocalDate parseReleaseDate(String releaseDate, String precision) {
        if (releaseDate == null || precision == null) {
            return null;
        }
        return switch (precision) {
            case "day" -> LocalDate.parse(releaseDate);
            case "month" -> LocalDate.parse(releaseDate + "-01");
            case "year" -> LocalDate.of(Integer.parseInt(releaseDate), 1, 1);
            default -> null;
        };
    }

    private String toEra(int year) {
        if (year < 2000) return "90s";
        if (year < 2010) return "00s";
        if (year < 2020) return "10s";
        return "20s";
    }

    private String toGeneration(int year) {
        if (year < 2004) return "1세대";
        if (year < 2012) return "2세대";
        if (year < 2018) return "3세대";
        if (year < 2024) return "4세대";
        return "5세대";
    }

    private synchronized String getAccessToken() {
        if (accessToken != null && Instant.now().isBefore(tokenExpiresAt)) {
            return accessToken;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        String credentials = Base64.getEncoder()
            .encodeToString((clientId + ":" + clientSecret).getBytes(StandardCharsets.UTF_8));
        headers.set("Authorization", "Basic " + credentials);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "client_credentials");

        @SuppressWarnings("unchecked")
        Map<String, Object> response = restTemplate.postForObject(
            TOKEN_URL, new HttpEntity<>(body, headers), Map.class
        );

        if (response == null || response.get("access_token") == null) {
            log.warn("[Spotify] 토큰 발급 실패");
            return null;
        }

        accessToken = (String) response.get("access_token");
        int expiresIn = (int) response.get("expires_in");
        tokenExpiresAt = Instant.now().plusSeconds(expiresIn - 60);
        return accessToken;
    }
}
