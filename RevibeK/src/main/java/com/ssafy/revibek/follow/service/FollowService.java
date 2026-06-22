package com.ssafy.revibek.follow.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ssafy.revibek.follow.dto.FollowDto;
import com.ssafy.revibek.follow.mapper.FollowMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FollowService {

    private final FollowMapper followMapper;

    @Transactional
    public void follow(String followerId, String followeeId) {
        if (followerId.equals(followeeId)) {
            throw new IllegalArgumentException("자기 자신을 팔로우할 수 없습니다.");
        }
        if (followMapper.countFollow(followerId, followeeId) > 0) {
            throw new IllegalStateException("이미 팔로우한 사용자입니다.");
        }
        followMapper.insertFollow(followerId, followeeId);
    }

    @Transactional
    public void unfollow(String followerId, String followeeId) {
        int deleted = followMapper.deleteFollow(followerId, followeeId);
        if (deleted == 0) {
            throw new RuntimeException("팔로우 관계가 존재하지 않습니다.");
        }
    }

    public List<FollowDto> getFollowings(String userId) {
        return followMapper.selectFollowings(userId);
    }

    public List<FollowDto> getFollowers(String userId) {
        return followMapper.selectFollowers(userId);
    }
}
