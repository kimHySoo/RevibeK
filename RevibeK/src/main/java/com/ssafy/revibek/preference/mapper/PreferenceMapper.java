package com.ssafy.revibek.preference.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ssafy.revibek.preference.dto.UserPreferenceDto;

@Mapper
public interface PreferenceMapper {

    int upsertUserPreference(UserPreferenceDto preference);

    UserPreferenceDto selectByUserId(@Param("userId") String userId);

    int deleteByUserId(@Param("userId") String userId);
}
