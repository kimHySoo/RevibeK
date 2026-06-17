package com.ssafy.revibek.user.service;

import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private static final long CODE_TTL_SECONDS = 5 * 60;      // 5분
    private static final long VERIFIED_TTL_SECONDS = 30 * 60; // 30분

    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${spring.mail.host:}")
    private String smtpHost;

    @Value("${spring.mail.username:}")
    private String senderEmail;

    @Value("${app.email.verification.mode:mock}")
    private String verificationMode;

    @Value("${app.email.verification.mock-code:123456}")
    private String mockCode;

    // dev/발표용: SMTP 실패 시 콘솔 출력으로 폴백
    @Value("${app.email.verification.fallback-on-failure:false}")
    private boolean fallbackOnFailure;

    private final Map<String, VerificationCodeEntry> codeStore = new ConcurrentHashMap<>();
    private final Map<String, Instant> verifiedEmailStore = new ConcurrentHashMap<>();

    public void sendVerificationCode(String email) {
        String normalizedEmail = normalizeEmail(email);
        cleanupExpiredEntries();
        String code = isMockMode() ? mockCode : generateCode();
        Instant expiresAt = Instant.now().plusSeconds(CODE_TTL_SECONDS);
        codeStore.put(normalizedEmail, new VerificationCodeEntry(code, expiresAt));

        if (isMockMode()) {
            log.info("[EmailVerification] MOCK 모드 — 이메일: {}, 코드: {}", normalizedEmail, code);
            return;
        }

        if (senderEmail.isBlank()) {
            log.error("[EmailVerification] SMTP 발송 계정이 설정되지 않았습니다. " +
                      "verificationMode={}, SMTP_HOST={}, SMTP_USERNAME='{}'",
                      verificationMode, smtpHost, senderEmail);
            if (fallbackOnFailure) {
                log.warn("[EmailVerification] FALLBACK — 이메일: {}, 코드: {} " +
                         "(SMTP 설정 없음 → 콘솔 출력으로 대체)", normalizedEmail, code);
                return;
            }
            throw new RuntimeException("메일 발송 설정이 올바르지 않습니다. 관리자에게 문의하세요.");
        }

        JavaMailSender sender = mailSenderProvider.getIfAvailable();
        if (sender == null) {
            log.error("[EmailVerification] JavaMailSender 빈이 없습니다. " +
                      "spring.mail.host 설정을 확인하세요. host='{}'", smtpHost);
            if (fallbackOnFailure) {
                log.warn("[EmailVerification] FALLBACK — 이메일: {}, 코드: {} " +
                         "(메일 서비스 빈 없음 → 콘솔 출력으로 대체)", normalizedEmail, code);
                return;
            }
            throw new RuntimeException("메일 서비스를 사용할 수 없습니다. 관리자에게 문의하세요.");
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(senderEmail);
            message.setTo(normalizedEmail);
            message.setSubject("[RevibeK] 이메일 인증코드");
            message.setText(
                "아래 인증코드를 입력해주세요.\n\n" +
                "인증코드: " + code + "\n" +
                "만료시간: 5분"
            );
            sender.send(message);
            log.info("[EmailVerification] 인증코드 발송 완료 — 이메일: {}", normalizedEmail);

        } catch (MailAuthenticationException e) {
            log.error("[EmailVerification] SMTP 인증 실패 — host: '{}', username: '{}'. " +
                      "Gmail 사용 시 앱 비밀번호(App Password)가 필요합니다. 오류: {}",
                      smtpHost, senderEmail, e.getMessage());
            if (fallbackOnFailure) {
                log.warn("[EmailVerification] FALLBACK — 이메일: {}, 코드: {} " +
                         "(SMTP 인증 실패 → 콘솔 출력으로 대체)", normalizedEmail, code);
                return;
            }
            throw new RuntimeException(
                "이메일 발송 서버 인증에 실패했습니다. " +
                "Gmail 사용 시 앱 비밀번호(Google 계정 → 보안 → 2단계 인증 → 앱 비밀번호)를 사용해야 합니다."
            );

        } catch (MailException e) {
            log.error("[EmailVerification] 메일 발송 실패 — 이메일: {}, host: '{}', 오류: {}",
                      normalizedEmail, smtpHost, e.getMessage());
            if (fallbackOnFailure) {
                log.warn("[EmailVerification] FALLBACK — 이메일: {}, 코드: {} " +
                         "(메일 발송 실패 → 콘솔 출력으로 대체)", normalizedEmail, code);
                return;
            }
            throw new RuntimeException("이메일 발송에 실패했습니다. 잠시 후 다시 시도해주세요.");
        }
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
