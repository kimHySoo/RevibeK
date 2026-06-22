package com.ssafy.revibek.plan.controller;

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
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.revibek.plan.dto.PlanRequestDto;
import com.ssafy.revibek.plan.dto.PlanResponseDto;
import com.ssafy.revibek.plan.service.PlanService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/plans")
@RequiredArgsConstructor
public class PlanController {

    private final PlanService planService;

    @PostMapping
    public ResponseEntity<String> createPlan(Authentication authentication,
                                              @Valid @RequestBody PlanRequestDto dto) {
        planService.createPlan(authentication.getName(), dto);
        return ResponseEntity.ok("계획 생성 완료");
    }

    @GetMapping
    public ResponseEntity<List<PlanResponseDto>> getMyPlans(Authentication authentication) {
        return ResponseEntity.ok(planService.getMyPlans(authentication.getName()));
    }

    @GetMapping("/{planId}")
    public ResponseEntity<PlanResponseDto> getPlan(Authentication authentication,
                                                    @PathVariable String planId) {
        return ResponseEntity.ok(planService.getPlan(planId, authentication.getName()));
    }

    @PutMapping("/{planId}")
    public ResponseEntity<String> updatePlan(Authentication authentication,
                                              @PathVariable String planId,
                                              @Valid @RequestBody PlanRequestDto dto) {
        planService.updatePlan(planId, authentication.getName(), dto);
        return ResponseEntity.ok("계획 수정 완료");
    }

    @DeleteMapping("/{planId}")
    public ResponseEntity<String> deletePlan(Authentication authentication,
                                              @PathVariable String planId) {
        planService.deletePlan(planId, authentication.getName());
        return ResponseEntity.ok("계획 삭제 완료");
    }
}
