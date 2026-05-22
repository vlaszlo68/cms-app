package hu.laci.cms.backend.service.auth;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CaptchaServiceTest {

    private static final Instant NOW = Instant.parse("2026-05-22T10:00:00Z");

    private final CaptchaService captchaService = new CaptchaService(Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void validateChallengeAcceptsCorrectAnswerAfterMinimumSolveTime() {
        CaptchaService.CaptchaValidationResult result = captchaService.validateChallenge(
                "captcha-1",
                8,
                CaptchaService.PURPOSE_REGISTRATION,
                NOW.minus(CaptchaService.MINIMUM_SOLVE_TIME).toEpochMilli(),
                0,
                "captcha-1",
                "8",
                CaptchaService.PURPOSE_REGISTRATION
        );

        assertTrue(result.valid());
        assertTrue(result.challengeConsumed());
        assertEquals(1, result.attemptsUsed());
    }

    @Test
    void validateChallengeRejectsAnswersSubmittedTooQuicklyWithoutConsumingFirstAttempt() {
        CaptchaService.CaptchaValidationResult result = captchaService.validateChallenge(
                "captcha-1",
                8,
                CaptchaService.PURPOSE_REGISTRATION,
                NOW.toEpochMilli(),
                0,
                "captcha-1",
                "8",
                CaptchaService.PURPOSE_REGISTRATION
        );

        assertFalse(result.valid());
        assertFalse(result.challengeConsumed());
        assertEquals(1, result.attemptsUsed());
    }

    @Test
    void validateChallengeConsumesAfterTwoFailedAttempts() {
        CaptchaService.CaptchaValidationResult result = captchaService.validateChallenge(
                "captcha-1",
                8,
                CaptchaService.PURPOSE_REGISTRATION,
                NOW.minus(CaptchaService.MINIMUM_SOLVE_TIME).toEpochMilli(),
                1,
                "captcha-1",
                "9",
                CaptchaService.PURPOSE_REGISTRATION
        );

        assertFalse(result.valid());
        assertTrue(result.challengeConsumed());
        assertEquals(2, result.attemptsUsed());
    }

    @Test
    void validateChallengeRejectsExpiredChallenge() {
        CaptchaService.CaptchaValidationResult result = captchaService.validateChallenge(
                "captcha-1",
                8,
                CaptchaService.PURPOSE_REGISTRATION,
                NOW.minus(CaptchaService.CHALLENGE_TTL).minusMillis(1).toEpochMilli(),
                0,
                "captcha-1",
                "8",
                CaptchaService.PURPOSE_REGISTRATION
        );

        assertFalse(result.valid());
        assertTrue(result.challengeConsumed());
    }

    @Test
    void validateChallengeRejectsWrongPurpose() {
        CaptchaService.CaptchaValidationResult result = captchaService.validateChallenge(
                "captcha-1",
                8,
                CaptchaService.PURPOSE_REGISTRATION,
                NOW.minus(CaptchaService.MINIMUM_SOLVE_TIME).toEpochMilli(),
                0,
                "captcha-1",
                "8",
                CaptchaService.PURPOSE_LOGIN
        );

        assertFalse(result.valid());
        assertTrue(result.challengeConsumed());
    }
}
