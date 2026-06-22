package com.ssafy.revibek.follow.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ssafy.revibek.follow.dto.FollowDto;

@Mapper
public interface FollowMapper {

    int countFollow(@Param("followerId") String followerId,
                    @Param("followeeId") String followeeId);

    void insertFollow(@Param("followerId") String followerId,
                      @Param("followeeId") String followeeId);

    int deleteFollow(@Param("followerId") String followerId,
                     @Param("followeeId") String followeeId);

    List<FollowDto> selectFollowings(@Param("userId") String userId);

    List<FollowDto> selectFollowers(@Param("userId") String userId);
}
