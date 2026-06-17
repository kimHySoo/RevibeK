package com.ssafy.revibek.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.ssafy.revibek.user.mapper.UserMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.dev.seed-user.email:dev@revibek.local}")
    private String seedEmail;

    @Value("${app.dev.seed-user.password:devpass123}")
    private String seedPassword;

    @Value("${app.dev.seed-user.nickname:개발자}")
    private String seedNickname;

    @Override
    public void run(ApplicationArguments args) {
        if (userMapper.selectUserAuthByEmail(seedEmail) != null) {
            log.info("[DataInitializer] 개발용 계정 이미 존재: {}", seedEmail);
            return;
        }
        String hash = passwordEncoder.encode(seedPassword);
        userMapper.insertLocalUser(seedNickname, seedEmail, hash);
        log.info("[DataInitializer] 개발용 계정 생성 완료 — email: {}, password: {}", seedEmail, seedPassword);
    }
}
