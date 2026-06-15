package  com.ssafy.revibek.radio.controller;

import com.ssafy.revibek.radio.dto.RadioCreateRequestDto;
import com.ssafy.revibek.radio.dto.RadioCreateResponseDto;
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

    private String resolveUserId(Authentication authentication, String headerUserId, String requestUserId) {
        if (StringUtils.hasText(headerUserId)) {
            return headerUserId.trim();
        }
        if (StringUtils.hasText(requestUserId)) {
            return requestUserId.trim();
        }
        if (authentication != null && StringUtils.hasText(authentication.getName())) {
            return authentication.getName();
        }
        throw new IllegalArgumentException("사용자 ID가 필요합니다. Authorization 또는 X-USER-ID를 전달해주세요.");
    }
}
