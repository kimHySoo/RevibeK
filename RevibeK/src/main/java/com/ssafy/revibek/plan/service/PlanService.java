package com.ssafy.revibek.plan.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ssafy.revibek.plan.dto.PlanRequestDto;
import com.ssafy.revibek.plan.dto.PlanResponseDto;
import com.ssafy.revibek.plan.mapper.PlanMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PlanService {

    private final PlanMapper planMapper;

    @Transactional
    public void createPlan(String userId, PlanRequestDto dto) {
        planMapper.insertPlan(
                UUID.randomUUID().toString(),
                userId,
                dto.getTitle(),
                dto.getDescription(),
                dto.getPlanDate(),
                dto.getPlanType()
        );
    }

    public List<PlanResponseDto> getMyPlans(String userId) {
        return planMapper.selectPlansByUserId(userId);
    }

    public PlanResponseDto getPlan(String planId, String userId) {
        PlanResponseDto plan = planMapper.selectPlanById(planId, userId);
        if (plan == null) {
            throw new RuntimeException("계획을 찾을 수 없거나 접근 권한이 없습니다.");
        }
        return plan;
    }

    @Transactional
    public void updatePlan(String planId, String userId, PlanRequestDto dto) {
        int updated = planMapper.updatePlan(planId, userId, dto.getTitle(), dto.getDescription(),
                dto.getPlanDate(), dto.getPlanType());
        if (updated == 0) {
            throw new RuntimeException("계획을 찾을 수 없거나 수정 권한이 없습니다.");
        }
    }

    @Transactional
    public void deletePlan(String planId, String userId) {
        int deleted = planMapper.deletePlan(planId, userId);
        if (deleted == 0) {
            throw new RuntimeException("계획을 찾을 수 없거나 삭제 권한이 없습니다.");
        }
    }
}
