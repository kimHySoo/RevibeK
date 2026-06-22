package com.ssafy.revibek.review.controller;

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

import com.ssafy.revibek.review.dto.ReviewRequestDto;
import com.ssafy.revibek.review.dto.ReviewResponseDto;
import com.ssafy.revibek.review.service.ReviewService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    public ResponseEntity<String> createReview(Authentication authentication,
                                                @Valid @RequestBody ReviewRequestDto dto) {
        reviewService.createReview(authentication.getName(), dto);
        return ResponseEntity.ok("리뷰 작성 완료");
    }

    @GetMapping("/songs/{songId}")
    public ResponseEntity<List<ReviewResponseDto>> getReviewsBySong(@PathVariable String songId) {
        return ResponseEntity.ok(reviewService.getReviewsBySong(songId));
    }

    @GetMapping("/me")
    public ResponseEntity<List<ReviewResponseDto>> getMyReviews(Authentication authentication) {
        return ResponseEntity.ok(reviewService.getMyReviews(authentication.getName()));
    }

    @PutMapping("/{reviewId}")
    public ResponseEntity<String> updateReview(Authentication authentication,
                                                @PathVariable String reviewId,
                                                @Valid @RequestBody ReviewRequestDto dto) {
        reviewService.updateReview(reviewId, authentication.getName(), dto);
        return ResponseEntity.ok("리뷰 수정 완료");
    }

    @DeleteMapping("/{reviewId}")
    public ResponseEntity<String> deleteReview(Authentication authentication,
                                                @PathVariable String reviewId) {
        reviewService.deleteReview(reviewId, authentication.getName());
        return ResponseEntity.ok("리뷰 삭제 완료");
    }
}
