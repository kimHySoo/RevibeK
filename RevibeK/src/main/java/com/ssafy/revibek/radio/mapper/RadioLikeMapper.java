package com.ssafy.revibek.radio.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RadioLikeMapper {

    int insertRadioLike(@Param("radioSessionId") String radioSessionId,
                        @Param("userId") String userId);

    int deleteRadioLike(@Param("radioSessionId") String radioSessionId,
                        @Param("userId") String userId);

    int existsRadioLike(@Param("radioSessionId") String radioSessionId,
                        @Param("userId") String userId);

    int countRadioLikes(@Param("radioSessionId") String radioSessionId);
}
