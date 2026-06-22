package com.ssafy.revibek.follow.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.revibek.follow.dto.FollowDto;
import com.ssafy.revibek.follow.service.FollowService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/follows")
@RequiredArgsConstructor
public class FollowController {

    private final FollowService followService;

    @PostMapping("/{followeeId}")
    public ResponseEntity<String> follow(Authentication authentication,
                                          @PathVariable String followeeId) {
        followService.follow(authentication.getName(), followeeId);
        return ResponseEntity.ok("팔로우 완료");
    }

    @DeleteMapping("/{followeeId}")
    public ResponseEntity<String> unfollow(Authentication authentication,
                                            @PathVariable String followeeId) {
        followService.unfollow(authentication.getName(), followeeId);
        return ResponseEntity.ok("언팔로우 완료");
    }

    @GetMapping("/followings")
    public ResponseEntity<List<FollowDto>> getFollowings(Authentication authentication) {
        return ResponseEntity.ok(followService.getFollowings(authentication.getName()));
    }

    @GetMapping("/followers")
    public ResponseEntity<List<FollowDto>> getFollowers(Authentication authentication) {
        return ResponseEntity.ok(followService.getFollowers(authentication.getName()));
    }
}
