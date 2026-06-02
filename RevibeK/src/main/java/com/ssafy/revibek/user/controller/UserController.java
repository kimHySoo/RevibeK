package com.ssafy.revibek.user.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.revibek.user.dto.UserResponseDto;
import com.ssafy.revibek.user.dto.UserUpdateRequestDto;
import com.ssafy.revibek.user.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // 내 정보 조회
    @GetMapping("/me")
    public ResponseEntity<UserResponseDto> getMyInfo(Authentication authentication) {
        return ResponseEntity.ok(userService.getMyInfo(authentication.getName()));
    }

    // 회원정보 수정
    @PutMapping("/me")
    public ResponseEntity<String> updateMyInfo(Authentication authentication,
                                               @Valid @RequestBody UserUpdateRequestDto dto) {
        userService.updateMyInfo(authentication.getName(), dto);
        return ResponseEntity.ok("수정 완료");
    }

    // 회원 탈퇴
    @DeleteMapping("/me")
    public ResponseEntity<String> deleteMyAccount(Authentication authentication) {
        userService.deleteMyAccount(authentication.getName());
        return ResponseEntity.ok("삭제 완료");
    }
}