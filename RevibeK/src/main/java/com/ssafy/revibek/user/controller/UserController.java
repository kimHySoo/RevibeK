package com.ssafy.revibek.user.controller;

import com.ssafy.revibek.user.dto.UserLoginRequestDto;
import com.ssafy.revibek.user.dto.UserRegisterRequestDto;
import com.ssafy.revibek.user.dto.UserResponseDto;
import com.ssafy.revibek.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // 회원가입
    @PostMapping("/signup")
    public ResponseEntity<String> signUp(@RequestBody UserRegisterRequestDto dto) {
        userService.signUp(dto);
        return ResponseEntity.ok("회원가입 완료");
    }

    // 로그인
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody UserLoginRequestDto dto) {
        return ResponseEntity.ok(userService.login(dto));
    }

    // 내 정보 조회
    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDto> getUser(@PathVariable String id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    // 회원정보 수정
    @PutMapping("/{id}")
    public ResponseEntity<String> updateUser(@PathVariable String id,
                                             @RequestBody UserResponseDto dto) {
        dto.setId(id);
        userService.updateUser(dto);
        return ResponseEntity.ok("수정 완료");
    }

    // 회원 탈퇴
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteUser(@PathVariable String id) {
        userService.deleteUser(id);
        return ResponseEntity.ok("삭제 완료");
    }
}