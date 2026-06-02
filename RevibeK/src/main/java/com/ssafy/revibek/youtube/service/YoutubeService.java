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

   
}