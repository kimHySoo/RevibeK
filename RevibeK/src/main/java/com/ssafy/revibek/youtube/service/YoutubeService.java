// com.ssafy.revibek.youtube.service.YoutubeService.java
package com.ssafy.revibek.youtube.service;

import com.ssafy.revibek.youtube.dto.YoutubeChannelDto;
import com.ssafy.revibek.youtube.dto.YoutubeVideoDto;
import com.ssafy.revibek.youtube.mapper.YoutubeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class YoutubeService {

    @Value("${youtube.api.key}")
    private String apiKey;

    private final YoutubeMapper youtubeMapper;
    private final RestTemplate restTemplate;

    private static final String YOUTUBE_API = "https://www.googleapis.com/youtube/v3";

    private static final List<String> CHANNEL_URLS = List.of(
    	"https://www.youtube.com/channel/UCh8AnUKJ2E4JDh4kI61UCHQ",  // 변경
        "https://www.youtube.com/@chenzie1004",
        "https://www.youtube.com/@THE_aIDOL",
        "https://www.youtube.com/@EQUINOX.entertainment",
        "https://www.youtube.com/@Dir.Fevernova",
        "https://www.youtube.com/channel/UCkVBepcU7jd1LjgKKfjz83Q",
        "https://www.youtube.com/@A_I_Go",
        "https://www.youtube.com/@DALLASTUDIOS",
        "https://www.youtube.com/channel/UC4woL-HW4aiHLp4AjyxS7Og",
        "https://www.youtube.com/@TREE-WAVE",
        "https://www.youtube.com/@i-playlist-hr",
        "https://www.youtube.com/@뮤잇-Music_it"
    );

    // 전체 실행 진입점
    public void collectAll() {
        for (String url : CHANNEL_URLS) {
            try {
                processChannel(url);
            } catch (Exception e) {
                log.error("[SKIP] 채널 처리 실패: {} - {}", url, e.getMessage());
            }
        }
        log.info("전체 채널 수집 완료");
    }

    private void processChannel(String channelUrl) {
        // 1. URL에서 handle 또는 channelId 파싱
        String handle = parseHandle(channelUrl);
        String rawChannelId = parseChannelId(channelUrl);

        // 2. YouTube API로 채널 정보 조회
        Map<String, Object> channelInfo = handle != null
            ? fetchChannelByHandle(handle)
            : fetchChannelById(rawChannelId);

        if (channelInfo == null) {
            log.warn("[SKIP] 채널 정보 없음: {}", channelUrl);
            return;
        }

        String channelId = (String) channelInfo.get("channelId");
        String channelName = (String) channelInfo.get("channelName");
        String uploadsPlaylistId = (String) channelInfo.get("uploadsPlaylistId");

        // 3. DB에 채널 저장
        YoutubeChannelDto channelDto = new YoutubeChannelDto();
        channelDto.setChannelId(channelId);
        channelDto.setChannelName(channelName);
        channelDto.setChannelUrl(channelUrl);
        youtubeMapper.insertChannel(channelDto);

        Long dbChannelId = youtubeMapper.findChannelIdByChannelId(channelId);
        log.info("[채널] {} ({})", channelName, channelId);

        // 4. 영상 목록 수집
        List<YoutubeVideoDto> videos = fetchAllVideos(uploadsPlaylistId, dbChannelId);
        for (YoutubeVideoDto video : videos) {
            youtubeMapper.insertVideo(video);
        }
        log.info("  → {}개 영상 저장 완료", videos.size());
    }

    // handle로 채널 조회 (@HarmoVerse 형식)
    @SuppressWarnings("unchecked")
    private Map<String, Object> fetchChannelByHandle(String handle) {
    	String url = UriComponentsBuilder.fromUriString(YOUTUBE_API + "/channels")
    	    .queryParam("part", "snippet,contentDetails")
    	    .queryParam("forHandle", handle)
    	    .queryParam("key", apiKey)
    	    .toUriString();

        return parseChannelResponse(restTemplate.getForObject(url, Map.class));
    }

    // channelId로 채널 조회 (/channel/UC... 형식)
    @SuppressWarnings("unchecked")
    private Map<String, Object> fetchChannelById(String channelId) {
        String url = UriComponentsBuilder.fromUriString(YOUTUBE_API + "/channels")
            .queryParam("part", "snippet,contentDetails")
            .queryParam("id", channelId)
            .queryParam("key", apiKey)
            .toUriString();

        return parseChannelResponse(restTemplate.getForObject(url, Map.class));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseChannelResponse(Map<String, Object> response) {
        if (response == null) return null;
        List<Map<String, Object>> items = (List<Map<String, Object>>) response.get("items");
        if (items == null || items.isEmpty()) return null;

        Map<String, Object> item = items.get(0);
        Map<String, Object> snippet = (Map<String, Object>) item.get("snippet");
        Map<String, Object> contentDetails = (Map<String, Object>) item.get("contentDetails");
        Map<String, Object> relatedPlaylists = (Map<String, Object>) contentDetails.get("relatedPlaylists");

        return Map.of(
            "channelId", item.get("id"),
            "channelName", snippet.get("title"),
            "uploadsPlaylistId", relatedPlaylists.get("uploads")
        );
    }

    // 전체 영상 목록 수집 (페이지네이션)
    @SuppressWarnings("unchecked")
    private List<YoutubeVideoDto> fetchAllVideos(String playlistId, Long dbChannelId) {
        List<YoutubeVideoDto> videos = new ArrayList<>();
        String nextPageToken = null;

        do {
            UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString(YOUTUBE_API + "/playlistItems")
                .queryParam("part", "snippet,contentDetails")
                .queryParam("playlistId", playlistId)
                .queryParam("maxResults", 50)
                .queryParam("key", apiKey);

            if (nextPageToken != null) {
                builder.queryParam("pageToken", nextPageToken);
            }

            Map<String, Object> response = restTemplate.getForObject(
                builder.toUriString(), Map.class
            );

            if (response == null) break;

            List<Map<String, Object>> items = (List<Map<String, Object>>) response.get("items");
            if (items != null) {
                for (Map<String, Object> item : items) {
                    Map<String, Object> snippet = (Map<String, Object>) item.get("snippet");
                    Map<String, Object> contentDetails = (Map<String, Object>) item.get("contentDetails");

                    String videoId = (String) contentDetails.get("videoId");
                    String title = (String) snippet.get("title");
                    String publishedAt = (String) contentDetails.get("videoPublishedAt");

                    if (publishedAt != null) {
                        publishedAt = publishedAt.replace("T", " ").substring(0, 19);
                    }

                    YoutubeVideoDto dto = new YoutubeVideoDto();
                    dto.setYoutubeChannelId(dbChannelId);
                    dto.setVideoId(videoId);
                    dto.setVideoUrl("https://www.youtube.com/watch?v=" + videoId);
                    dto.setVideoTitle(title);
                    dto.setPublishedAt(publishedAt);
                    videos.add(dto);
                }
            }

            nextPageToken = (String) response.get("nextPageToken");

        } while (nextPageToken != null);

        return videos;
    }

    // URL 파싱 유틸
    private String parseHandle(String url) {
        if (url.contains("/@")) {
            // /@HarmoVerse/videos 같은 경우도 처리
            String handle = url.replaceAll(".*/\\@([^/]+).*", "$1");
            // URL 디코딩 (한글 등)
            try {
                return java.net.URLDecoder.decode(handle, "UTF-8");
            } catch (Exception e) {
                return handle;
            }
        }
        return null;
    }

    private String parseChannelId(String url) {
        if (url.contains("/channel/")) {
            return url.replaceAll(".*/channel/([^/]+).*", "$1");
        }
        return null;
    }
}