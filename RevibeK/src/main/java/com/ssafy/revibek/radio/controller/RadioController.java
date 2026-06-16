package  com.ssafy.revibek.radio.controller;

import com.ssafy.revibek.radio.dto.PublicRadioFeedDto;
import com.ssafy.revibek.radio.dto.RadioCreateRequestDto;
import com.ssafy.revibek.radio.dto.RadioCreateResponseDto;
import com.ssafy.revibek.radio.dto.RadioLikeCountResponseDto;
import com.ssafy.revibek.radio.dto.RadioLikeResponseDto;
import com.ssafy.revibek.radio.dto.RadioPublicToggleRequestDto;
import com.ssafy.revibek.radio.dto.RadioPublicToggleResponseDto;
import com.ssafy.revibek.radio.dto.RadioResponseDto;
import com.ssafy.revibek.radio.service.RadioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import java.util.List;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/radio")
@RequiredArgsConstructor
public class RadioController {

    private final RadioService radioService;

    // 라디오 세션 생성
    @PostMapping
    public ResponseEntity<RadioCreateResponseDto> createRadio(Authentication authentication,
                                                              @RequestHeader(value = "X-USER-ID", required = false) String headerUserId,
                                                              @RequestParam(value = "userId", required = false) String requestUserId,
                                                              @Valid @RequestBody RadioCreateRequestDto dto) {
        return ResponseEntity.ok(radioService.createRadio(resolveUserId(authentication, headerUserId, requestUserId), dto));
    }

    // 세션 단건 조회
    @GetMapping("/{id}")
    public ResponseEntity<RadioResponseDto> getSession(Authentication authentication,
                                                       @RequestHeader(value = "X-USER-ID", required = false) String headerUserId,
                                                       @RequestParam(value = "userId", required = false) String requestUserId,
                                                       @PathVariable String id) {
        return ResponseEntity.ok(radioService.getSession(id, resolveUserId(authentication, headerUserId, requestUserId)));
    }

    // 내 세션 목록 조회
    @GetMapping("/me")
    public ResponseEntity<List<RadioResponseDto>> getSessionByUser(Authentication authentication,
                                                                   @RequestHeader(value = "X-USER-ID", required = false) String headerUserId,
                                                                   @RequestParam(value = "userId", required = false) String requestUserId) {
        return ResponseEntity.ok(radioService.getSessionByUser(resolveUserId(authentication, headerUserId, requestUserId)));
    }

    // 라디오 공개/비공개 전환
    @PutMapping("/{radioSessionId}/public")
    public ResponseEntity<RadioPublicToggleResponseDto> togglePublic(Authentication authentication,
                                                                     @PathVariable String radioSessionId,
                                                                     @RequestBody RadioPublicToggleRequestDto request) {
        return ResponseEntity.ok(radioService.togglePublic(authentication.getName(), radioSessionId, request));
    }

    // 공개 라디오 사연 목록 (비로그인 가능)
    @GetMapping("/public")
    public ResponseEntity<List<PublicRadioFeedDto>> getPublicSessions(Authentication authentication,
                                                                       @RequestParam(value = "sort", defaultValue = "latest") String sort) {
        String currentUserId = authentication != null ? authentication.getName() : null;
        return ResponseEntity.ok(radioService.getPublicSessions(sort, currentUserId));
    }

    // 공개 라디오 사연 상세 (비로그인 가능)
    @GetMapping("/public/{radioSessionId}")
    public ResponseEntity<PublicRadioFeedDto> getPublicSession(Authentication authentication,
                                                                @PathVariable String radioSessionId) {
        String currentUserId = authentication != null ? authentication.getName() : null;
        return ResponseEntity.ok(radioService.getPublicSession(radioSessionId, currentUserId));
    }

    // 라디오 사연 좋아요
    @PostMapping("/{radioSessionId}/likes")
    public ResponseEntity<RadioLikeResponseDto> addRadioLike(Authentication authentication,
                                                              @PathVariable String radioSessionId) {
        return ResponseEntity.ok(radioService.addRadioLike(authentication.getName(), radioSessionId));
    }

    // 라디오 사연 좋아요 취소
    @DeleteMapping("/{radioSessionId}/likes")
    public ResponseEntity<RadioLikeResponseDto> removeRadioLike(Authentication authentication,
                                                                 @PathVariable String radioSessionId) {
        return ResponseEntity.ok(radioService.removeRadioLike(authentication.getName(), radioSessionId));
    }

    // 라디오 사연 좋아요 상태
    @GetMapping("/{radioSessionId}/likes/status")
    public ResponseEntity<RadioLikeResponseDto> getRadioLikeStatus(Authentication authentication,
                                                                    @PathVariable String radioSessionId) {
        return ResponseEntity.ok(radioService.getRadioLikeStatus(authentication.getName(), radioSessionId));
    }

    // 라디오 사연 좋아요 수 (비로그인 가능)
    @GetMapping("/{radioSessionId}/likes/count")
    public ResponseEntity<RadioLikeCountResponseDto> getRadioLikeCount(@PathVariable String radioSessionId) {
        return ResponseEntity.ok(radioService.getRadioLikeCount(radioSessionId));
    }

    private String resolveUserId(Authentication authentication, String headerUserId, String requestUserId) {
        if (authentication != null && StringUtils.hasText(authentication.getName())) {
            return authentication.getName();
        }
        throw new IllegalArgumentException("로그인 사용자 정보가 필요합니다.");
    }
}
