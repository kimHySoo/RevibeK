package com.ssafy.revibek.user.service;

import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.ssafy.revibek.common.exception.EmailSendFailedException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private static final long CODE_TTL_SECONDS = 5 * 60;      // 5분
    private static final long VERIFIED_TTL_SECONDS = 30 * 60; // 30분

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String senderEmail;

    @Value("${app.email.verification.mode:mock}")
    private String verificationMode;

    @Value("${app.email.verification.mock-code:123456}")
    private String mockCode;

    // SMTP 인증 실패 시에도 회원가입/로그인 흐름이 깨지지 않도록 하는 발표/개발용 옵션.
    // true: 발송 실패를 콘솔 로그로 대체하고 정상 흐름을 유지한다.
    // false: 발송 실패를 EmailSendFailedException(503)으로 클라이언트에 알린다.
    @Value("${app.email.verification.fail-open:true}")
    private boolean failOpen;

    private final Map<String, VerificationCodeEntry> codeStore = new ConcurrentHashMap<>();
    private final Map<String, Instant> verifiedEmailStore = new ConcurrentHashMap<>();

    public void sendVerificationCode(String email) {
        String normalizedEmail = normalizeEmail(email);
        cleanupExpiredEntries();
        String code = isMockMode() ? mockCode : generateCode();
        Instant expiresAt = Instant.now().plusSeconds(CODE_TTL_SECONDS);
        codeStore.put(normalizedEmail, new VerificationCodeEntry(code, expiresAt));

        if (isMockMode()) {
            log.info("[MOCK] 이메일 인증코드 발송 - email={}, code={}", normalizedEmail, code);
            return;
        }

        if (senderEmail == null || senderEmail.isBlank()) {
            handleSendFailure(normalizedEmail, code, "SMTP 발송 계정(spring.mail.username)이 설정되지 않았습니다.", null);
            return;
        }

        try {
            mailSender.send(buildMessage(normalizedEmail, code));
        } catch (MailException ex) {
            // Gmail SMTP 인증 실패(MailAuthenticationException 등)를 포함한 모든 발송 실패를 여기서 처리한다.
            handleSendFailure(normalizedEmail, code, "이메일 발송에 실패했습니다.", ex);
        }
    }

    private SimpleMailMessage buildMessage(String to, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(senderEmail);
        message.setTo(to);
        message.setSubject("[RevibeK] 이메일 인증코드");
        message.setText(
            "아래 인증코드를 입력해주세요.\n\n" +
            "인증코드: " + code + "\n" +
            "만료시간: 5분"
        );
        return message;
    }

    private void handleSendFailure(String email, String code, String reason, Exception cause) {
        log.error("이메일 발송 실패 - email={}, reason={}", email, reason, cause);
        if (!failOpen) {
            throw new EmailSendFailedException("이메일 발송에 실패했습니다. 잠시 후 다시 시도해주세요.");
        }
        // fail-open: 인증코드는 이미 codeStore에 저장되어 있으므로 콘솔 출력으로 대체하고 흐름은 계속 진행한다.
        log.warn("[FAIL-OPEN] 메일 발송 실패로 콘솔 출력으로 대체합니다. email={}, code={}", email, code);
    }

    public void verifyCode(String email, String code) {
        String normalizedEmail = normalizeEmail(email);
        cleanupExpiredEntries();
        VerificationCodeEntry entry = codeStore.get(normalizedEmail);
        if (entry == null) {
            if (isMockMode() && mockCode.equals(code)) {
                verifiedEmailStore.put(normalizedEmail, Instant.now().plusSeconds(VERIFIED_TTL_SECONDS));
                return;
            }
            throw new RuntimeException("인증코드가 없거나 만료되었습니다.");
        }
        if (!entry.code().equals(code)) {
            throw new RuntimeException("인증코드가 올바르지 않습니다.");
        }
        codeStore.remove(normalizedEmail);
        verifiedEmailStore.put(normalizedEmail, Instant.now().plusSeconds(VERIFIED_TTL_SECONDS));
    }

    public boolean isVerified(String email) {
        String normalizedEmail = normalizeEmail(email);
        cleanupExpiredEntries();
        Instant verifiedUntil = verifiedEmailStore.get(normalizedEmail);
        return verifiedUntil != null && verifiedUntil.isAfter(Instant.now());
    }

    public void consumeVerification(String email) {
        verifiedEmailStore.remove(normalizeEmail(email));
    }

    private void cleanupExpiredEntries() {
        Instant now = Instant.now();
        codeStore.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(now));
        verifiedEmailStore.entrySet().removeIf(entry -> entry.getValue().isBefore(now));
    }

    private String generateCode() {
        int value = ThreadLocalRandom.current().nextInt(100000, 1000000);
        return Integer.toString(value);
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private boolean isMockMode() {
        return !"smtp".equalsIgnoreCase(verificationMode);
    }

    private record VerificationCodeEntry(String code, Instant expiresAt) {
    }
}
