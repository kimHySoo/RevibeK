package com.ssafy.revibek.challenge.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.revibek.challenge.dto.ChallengeParticipantDto;
import com.ssafy.revibek.challenge.dto.ChallengeRequestDto;
import com.ssafy.revibek.challenge.dto.ChallengeResponseDto;
import com.ssafy.revibek.challenge.dto.ChallengeTemplateDto;
import com.ssafy.revibek.challenge.service.ChallengeService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/challenges")
@RequiredArgsConstructor
public class ChallengeController {

    private final ChallengeService challengeService;

    @PostMapping
    public ResponseEntity<String> createChallenge(Authentication authentication,
                                                    @Valid @RequestBody ChallengeRequestDto dto) {
        challengeService.createChallenge(authentication.getName(), dto);
        return ResponseEntity.ok("챌린지 생성 완료");
    }

    @GetMapping
    public ResponseEntity<List<ChallengeResponseDto>> getAllChallenges() {
        return ResponseEntity.ok(challengeService.getAllChallenges());
    }

    // /templates 는 /{challengeId} 보다 반드시 먼저 선언
    @GetMapping("/templates")
    public ResponseEntity<List<ChallengeTemplateDto>> getTemplates() {
        return ResponseEntity.ok(challengeService.getTemplates());
    }

    @GetMapping("/{challengeId}")
    public ResponseEntity<ChallengeResponseDto> getChallenge(@PathVariable String challengeId) {
        return ResponseEntity.ok(challengeService.getChallenge(challengeId));
    }

    @PostMapping("/{challengeId}/join")
    public ResponseEntity<String> joinChallenge(Authentication authentication,
                                                 @PathVariable String challengeId) {
        challengeService.joinChallenge(challengeId, authentication.getName());
        return ResponseEntity.ok("챌린지 참여 완료");
    }

    @GetMapping("/me")
    public ResponseEntity<List<ChallengeParticipantDto>> getMyChallenges(Authentication authentication) {
        return ResponseEntity.ok(challengeService.getMyChallenges(authentication.getName()));
    }

    @PutMapping("/{challengeId}/progress")
    public ResponseEntity<String> updateProgress(Authentication authentication,
                                                  @PathVariable String challengeId,
                                                  @RequestParam(defaultValue = "1") int increment) {
        challengeService.updateProgress(challengeId, authentication.getName(), increment);
        return ResponseEntity.ok("진행도 업데이트 완료");
    }

    @DeleteMapping("/{challengeId}/leave")
    public ResponseEntity<String> leaveChallenge(Authentication authentication,
                                                  @PathVariable String challengeId) {
        challengeService.leaveChallenge(challengeId, authentication.getName());
        return ResponseEntity.ok("챌린지 나가기 완료");
    }

    @DeleteMapping("/{challengeId}")
    public ResponseEntity<String> deleteChallenge(Authentication authentication,
                                                   @PathVariable String challengeId) {
        challengeService.deleteChallenge(challengeId, authentication.getName());
        return ResponseEntity.ok("챌린지 삭제 완료");
    }
}
