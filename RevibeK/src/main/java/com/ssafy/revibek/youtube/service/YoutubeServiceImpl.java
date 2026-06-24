// com.ssafy.revibek.youtube.service.YoutubeServiceImpl.java
package com.ssafy.revibek.youtube.service;

import com.ssafy.revibek.youtube.dto.YoutubeChannelDto;
import com.ssafy.revibek.youtube.dto.YoutubeFallbackResponseDto;
import com.ssafy.revibek.youtube.dto.YoutubeVideoDto;
import com.ssafy.revibek.youtube.dto.YoutubeVideoResponseDto;
import com.ssafy.revibek.youtube.dto.YoutubeVideoStatsDto;
import com.ssafy.revibek.youtube.mapper.YoutubeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
@Slf4j
@Service
@RequiredArgsConstructor
public class YoutubeServiceImpl implements YoutubeService {

    @Value("${youtube.enabled:false}")
    private boolean enabled;

    @Value("${youtube.api.key:}")
    private String apiKey;

    private final YoutubeMapper youtubeMapper;
    private final RestTemplate restTemplate;

    private static final String YOUTUBE_API = "https://www.googleapis.com/youtube/v3";

    @Override
    public YoutubeFallbackResponseDto processChannelWithResponse(String channelUrl) {
        if (!enabled || !StringUtils.hasText(apiKey)) {
            log.info("[YouTube] API disabled or key missing. Skip external channel collection: {}", channelUrl);
            return fallbackResponse("YouTube API is disabled or API key is missing.");
        }

        try {
            String handle = parseHandle(channelUrl);
            String rawChannelId = parseChannelId(channelUrl);

            Map<String, Object> channelInfo = handle != null
                ? fetchChannelByHandle(handle)
                : fetchChannelById(rawChannelId);

            if (channelInfo == null) {
                log.warn("[SKIP] 채널 정보 없음: {}", channelUrl);
                return fallbackResponse("YouTube channel metadata was not found. Using fallback video metadata.");
            }

            String channelId = (String) channelInfo.get("channelId");
            String channelName = (String) channelInfo.get("channelName");
            String uploadsPlaylistId = (String) channelInfo.get("uploadsPlaylistId");

            YoutubeChannelDto channelDto = new YoutubeChannelDto();
            channelDto.setChannelId(channelId);
            channelDto.setChannelName(channelName);
            channelDto.setChannelUrl(channelUrl);
            channelDto.setUploadsPlaylist(uploadsPlaylistId);
            youtubeMapper.insertChannel(channelDto);

            log.info("[채널] {} ({})", channelName, channelId);

            List<YoutubeVideoDto> videos = fetchAllVideos(uploadsPlaylistId, channelId);
            for (YoutubeVideoDto video : videos) {
                youtubeMapper.insertVideo(video);
            }
            log.info("  → {}개 영상 저장 완료", videos.size());
            return YoutubeFallbackResponseDto.builder()
                .source("youtube")
                .message("YouTube channel videos collected successfully.")
                .videos(toResponseVideos(videos))
                .build();

        } catch (Exception e) {
            log.error("[SKIP] 채널 처리 실패: {} - {}", channelUrl, e.getMessage());
            return fallbackResponse("YouTube API call failed. Using fallback video metadata.");
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> fetchChannelByHandle(String handle) {
        String url = UriComponentsBuilder.fromUriString(YOUTUBE_API + "/channels")
            .queryParam("part", "snippet,contentDetails")
            .queryParam("forHandle", handle)
            .queryParam("key", apiKey)
            .toUriString();
        return parseChannelResponse(restTemplate.getForObject(url, Map.class));
    }

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

    @SuppressWarnings("unchecked")
    private List<YoutubeVideoDto> fetchAllVideos(String playlistId, String channelId) {
        List<YoutubeVideoDto> videos = new ArrayList<>();
        String nextPageToken = null;

        do {
            String url = YOUTUBE_API + "/playlistItems"
                + "?part=snippet,contentDetails"
                + "&playlistId=" + playlistId
                + "&maxResults=50"
                + "&key=" + apiKey
                + (nextPageToken != null ? "&pageToken=" + nextPageToken : "");

            log.info("[playlistItems 조회] url: {}", url);

            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response == null) break;

            List<Map<String, Object>> items = (List<Map<String, Object>>) response.get("items");
            if (items != null) {
                List<String> videoIds = new ArrayList<>();
                List<YoutubeVideoDto> tempVideos = new ArrayList<>();

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
                    dto.setChannelId(channelId);
                    dto.setVideoId(videoId);
                    dto.setVideoUrl("https://www.youtube.com/watch?v=" + videoId);
                    dto.setTitle(title);
                    dto.setPublishedAt(publishedAt);
                    dto.setCollectStatus("PENDING");
                    tempVideos.add(dto);
                    videoIds.add(videoId);
                }

                log.info("[videoIds 크기] {}", videoIds.size());

                Map<String, Integer> durationMap = fetchDurationSeconds(videoIds);

                log.info("[durationMap 크기] {}", durationMap.size());

                for (YoutubeVideoDto dto : tempVideos) {
                    dto.setDurationSeconds(durationMap.getOrDefault(dto.getVideoId(), null));
                }

                videos.addAll(tempVideos);
            }

            nextPageToken = (String) response.get("nextPageToken");

        } while (nextPageToken != null);

        return videos;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Integer> fetchDurationSeconds(List<String> videoIds) {
        Map<String, Integer> durationMap = new HashMap<>();

        String ids = String.join(",", videoIds);
        String url = YOUTUBE_API + "/videos"
            + "?part=contentDetails"
            + "&id=" + ids
            + "&key=" + apiKey;

        log.info("[Duration 조회] url: {}", url);

        Map<String, Object> response = restTemplate.getForObject(url, Map.class);

        log.info("[Duration 응답] {}", response);

        if (response == null) return durationMap;

        List<Map<String, Object>> items = (List<Map<String, Object>>) response.get("items");
        if (items == null) return durationMap;

        for (Map<String, Object> item : items) {
            String videoId = (String) item.get("id");
            Map<String, Object> contentDetails = (Map<String, Object>) item.get("contentDetails");
            String duration = (String) contentDetails.get("duration");
            log.info("[Duration 파싱] videoId: {}, duration: {}", videoId, duration);
            durationMap.put(videoId, parseDuration(duration));
        }

        return durationMap;
    }

    @Override
    @SuppressWarnings("unchecked")
    public YoutubeVideoStatsDto fetchVideoStats(String videoId) {
        if (!enabled || !StringUtils.hasText(apiKey)) {
            log.info("[YouTube] API disabled or key missing. Skip video stats lookup: {}", videoId);
            return null;
        }

        try {
            String url = YOUTUBE_API + "/videos"
                + "?part=snippet,statistics"
                + "&id=" + videoId
                + "&key=" + apiKey;

            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response == null) return null;

            List<Map<String, Object>> items = (List<Map<String, Object>>) response.get("items");
            if (items == null || items.isEmpty()) return null;

            Map<String, Object> item = items.get(0);
            Map<String, Object> snippet = (Map<String, Object>) item.get("snippet");
            Map<String, Object> statistics = (Map<String, Object>) item.get("statistics");
            Map<String, Object> thumbnails = (Map<String, Object>) snippet.get("thumbnails");

            String thumbnailUrl = null;
            for (String quality : new String[]{"high", "medium", "default"}) {
                Map<String, Object> thumbnail = (Map<String, Object>) thumbnails.get(quality);
                if (thumbnail != null) {
                    thumbnailUrl = (String) thumbnail.get("url");
                    break;
                }
            }

            int viewCount = statistics.get("viewCount") != null
                ? Integer.parseInt((String) statistics.get("viewCount")) : 0;
            int likeCount = statistics.get("likeCount") != null
                ? Integer.parseInt((String) statistics.get("likeCount")) : 0;

            return new YoutubeVideoStatsDto(thumbnailUrl, viewCount, likeCount);
        } catch (Exception e) {
            log.error("[SKIP] 영상 통계 조회 실패: {} - {}", videoId, e.getMessage());
            return null;
        }
    }

    @Override
    public Integer fetchDurationSeconds(String videoId) {
        if (!enabled || !StringUtils.hasText(apiKey)) {
            log.info("[YouTube] API disabled or key missing. Skip duration lookup: {}", videoId);
            return null;
        }
        return fetchDurationSeconds(List.of(videoId)).get(videoId);
    }

    private Integer parseDuration(String duration) {
        try {
            java.time.Duration d = java.time.Duration.parse(duration);
            return (int) d.getSeconds();
        } catch (Exception e) {
            log.error("[Duration 파싱 실패] {}", duration);
            return null;
        }
    }

    private String parseHandle(String url) {
        if (url.contains("/@")) {
            String handle = url.replaceAll(".*/\\@([^/]+).*", "$1");
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

    private YoutubeFallbackResponseDto fallbackResponse(String message) {
        return YoutubeFallbackResponseDto.builder()
            .source("fallback")
            .message(message)
            .videos(List.of(YoutubeVideoResponseDto.builder()
                .title("Sample K-POP video")
                .youtubeId("sample")
                .youtubeUrl("https://www.youtube.com/watch?v=sample")
                .thumbnailUrl(null)
                .build()))
            .build();
    }

    private List<YoutubeVideoResponseDto> toResponseVideos(List<YoutubeVideoDto> videos) {
        return videos.stream()
            .map(video -> YoutubeVideoResponseDto.builder()
                .title(video.getTitle())
                .youtubeId(video.getVideoId())
                .youtubeUrl(video.getVideoUrl())
                .thumbnailUrl(null)
                .build())
            .toList();
    }
}