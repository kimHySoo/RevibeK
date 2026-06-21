package com.ssafy.revibek.radio.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

class GmsClientTest {

    private RestTemplate restTemplate;
    private GmsClient gmsClient;

    @BeforeEach
    void setUp() {
        restTemplate = mock(RestTemplate.class);
        gmsClient = new GmsClient(restTemplate);
        ReflectionTestUtils.setField(gmsClient, "baseUrl", "https://gms.ssafy.io/gmsapi/api.anthropic.com/v1/messages");
        ReflectionTestUtils.setField(gmsClient, "apiKey", "dummy-key");
        ReflectionTestUtils.setField(gmsClient, "model", "claude-sonnet-4-5-20250929");
        ReflectionTestUtils.setField(gmsClient, "anthropicVersion", "2023-06-01");
        ReflectionTestUtils.setField(gmsClient, "maxTokens", 300);
    }

    @Test
    void gms비활성화면_외부API를_호출하지않고_fallback을_반환한다() {
        ReflectionTestUtils.setField(gmsClient, "enabled", false);

        Optional<String> result = gmsClient.generate("프롬프트");

        assertThat(result).isEmpty();
        verify(restTemplate, never()).postForObject(any(String.class), any(), any(Class.class));
    }

    @Test
    void apiKey가없으면_외부API를_호출하지않고_fallback을_반환한다() {
        ReflectionTestUtils.setField(gmsClient, "enabled", true);
        ReflectionTestUtils.setField(gmsClient, "apiKey", "");

        Optional<String> result = gmsClient.generate("프롬프트");

        assertThat(result).isEmpty();
        verify(restTemplate, never()).postForObject(any(String.class), any(), any(Class.class));
    }

    @Test
    void baseUrl이없으면_외부API를_호출하지않고_fallback을_반환한다() {
        ReflectionTestUtils.setField(gmsClient, "enabled", true);
        ReflectionTestUtils.setField(gmsClient, "baseUrl", "");

        Optional<String> result = gmsClient.generate("프롬프트");

        assertThat(result).isEmpty();
        verify(restTemplate, never()).postForObject(any(String.class), any(), any(Class.class));
    }

    @Test
    void 정상응답이면_Claude텍스트를_반환한다() {
        ReflectionTestUtils.setField(gmsClient, "enabled", true);
        Map<String, Object> response = Map.of(
                "content", List.of(Map.of("type", "text", "text", "안녕하세요, DJ 리아예요."))
        );
        when(restTemplate.postForObject(any(String.class), any(), any(Class.class)))
                .thenReturn(response);

        Optional<String> result = gmsClient.generate("프롬프트");

        assertThat(result).contains("안녕하세요, DJ 리아예요.");
    }

    @Test
    void 응답에content가없으면_fallback을_반환한다() {
        ReflectionTestUtils.setField(gmsClient, "enabled", true);
        when(restTemplate.postForObject(any(String.class), any(), any(Class.class)))
                .thenReturn(Map.of());

        Optional<String> result = gmsClient.generate("프롬프트");

        assertThat(result).isEmpty();
    }

    @Test
    void 외부API호출이실패하면_fallback을_반환한다() {
        ReflectionTestUtils.setField(gmsClient, "enabled", true);
        when(restTemplate.postForObject(any(String.class), any(), any(Class.class)))
                .thenThrow(new RuntimeException("connection refused"));

        Optional<String> result = gmsClient.generate("프롬프트");

        assertThat(result).isEmpty();
    }
}
