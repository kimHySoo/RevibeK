package com.ssafy.revibek.review.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ssafy.revibek.review.dto.ReviewResponseDto;

@Mapper
public interface ReviewMapper {

    void insertReview(@Param("id") String id,
                      @Param("userId") String userId,
                      @Param("songId") String songId,
                      @Param("content") String content,
                      @Param("rating") Integer rating);

    List<ReviewResponseDto> selectReviewsBySongId(@Param("songId") String songId);

    List<ReviewResponseDto> selectReviewsByUserId(@Param("userId") String userId);

    ReviewResponseDto selectReviewById(@Param("id") String id);

    int updateReview(@Param("id") String id,
                     @Param("userId") String userId,
                     @Param("content") String content,
                     @Param("rating") Integer rating);

    int deleteReview(@Param("id") String id,
                     @Param("userId") String userId);
}
