package com.ssafy.revibek.review.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ssafy.revibek.review.dto.ReviewRequestDto;
import com.ssafy.revibek.review.dto.ReviewResponseDto;
import com.ssafy.revibek.review.mapper.ReviewMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewMapper reviewMapper;

    @Transactional
    public void createReview(String userId, ReviewRequestDto dto) {
        reviewMapper.insertReview(
                UUID.randomUUID().toString(),
                userId,
                dto.getSongId(),
                dto.getContent(),
                dto.getRating()
        );
    }

    public List<ReviewResponseDto> getReviewsBySong(String songId) {
        return reviewMapper.selectReviewsBySongId(songId);
    }

    public List<ReviewResponseDto> getMyReviews(String userId) {
        return reviewMapper.selectReviewsByUserId(userId);
    }

    @Transactional
    public void updateReview(String reviewId, String userId, ReviewRequestDto dto) {
        int updated = reviewMapper.updateReview(reviewId, userId, dto.getContent(), dto.getRating());
        if (updated == 0) {
            throw new RuntimeException("리뷰를 찾을 수 없거나 수정 권한이 없습니다.");
        }
    }

    @Transactional
    public void deleteReview(String reviewId, String userId) {
        int deleted = reviewMapper.deleteReview(reviewId, userId);
        if (deleted == 0) {
            throw new RuntimeException("리뷰를 찾을 수 없거나 삭제 권한이 없습니다.");
        }
    }
}
