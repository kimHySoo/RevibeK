package com.ssafy.revibek.plan.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ssafy.revibek.plan.dto.PlanResponseDto;

@Mapper
public interface PlanMapper {

    void insertPlan(@Param("id") String id,
                    @Param("userId") String userId,
                    @Param("title") String title,
                    @Param("description") String description,
                    @Param("planDate") String planDate,
                    @Param("planType") String planType);

    List<PlanResponseDto> selectPlansByUserId(@Param("userId") String userId);

    PlanResponseDto selectPlanById(@Param("id") String id,
                                    @Param("userId") String userId);

    int updatePlan(@Param("id") String id,
                   @Param("userId") String userId,
                   @Param("title") String title,
                   @Param("description") String description,
                   @Param("planDate") String planDate,
                   @Param("planType") String planType);

    int deletePlan(@Param("id") String id,
                   @Param("userId") String userId);
}
