package hu.laci.cms.backend.service.auth;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Generates simple math CAPTCHA challenges and validates one-time answers.
 */
public class CaptchaService {

    public static final String SESSION_ID_ATTRIBUTE = "registrationCaptchaId";
    public static final String SESSION_ANSWER_ATTRIBUTE = "registrationCaptchaAnswer";
    public static final String SESSION_PURPOSE_ATTRIBUTE = "registrationCaptchaPurpose";
    public static final String SESSION_CREATED_AT_ATTRIBUTE = "registrationCaptchaCreatedAt";
    public static final String SESSION_ATTEMPTS_ATTRIBUTE = "registrationCaptchaAttempts";
    public static final String PURPOSE_LOGIN = "login";
    public static final String PURPOSE_REGISTRATION = "registration";
    public static final int MAX_VALIDATION_ATTEMPTS = 2;
    public static final Duration CHALLENGE_TTL = Duration.ofMinutes(3);
    public static final Duration MINIMUM_SOLVE_TIME = Duration.ofSeconds(1);

    private final SecureRandom random = new SecureRandom();
    private final Clock clock;

    /**
     * Creates a CAPTCHA service using the system UTC clock.
     */
    public CaptchaService() {
        this(Clock.systemUTC());
    }

    /**
     * Creates a CAPTCHA service using a caller-provided clock, primarily for deterministic tests.
     *
     * @param clock clock used for TTL and minimum solve-time checks
     */
    public CaptchaService(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    /**
     * Creates a new math CAPTCHA challenge with randomized SVG rendering.
     *
     * @return generated challenge containing id, expected answer, and SVG body
     */
    public CaptchaChallenge createChallenge() {
        MathChallenge challenge = createMathChallenge();
        return new CaptchaChallenge(UUID.randomUUID().toString(), challenge.expectedAnswer(),
                renderSvg(challenge.question()));
    }

    /**
     * Validates the submitted id and answer against expected values.
     *
     * @param expectedId expected CAPTCHA id from server-side state
     * @param expectedAnswer expected CAPTCHA answer from server-side state
     * @param submittedId submitted CAPTCHA id
     * @param submittedAnswer submitted answer text
     * @return true when the submitted id and parsed answer match
     */
    public boolean validate(String expectedId, Integer expectedAnswer, String submittedId, String submittedAnswer) {
        if (expectedId == null || expectedAnswer == null || isBlank(submittedId) || isBlank(submittedAnswer)) {
            return false;
        }

        int parsedAnswer;
        try {
            parsedAnswer = Integer.parseInt(submittedAnswer.trim());
        } catch (NumberFormatException e) {
            return false;
        }

        return expectedId.equals(submittedId.trim()) && expectedAnswer == parsedAnswer;
    }

    /**
     * Validates a session-backed CAPTCHA challenge with purpose binding and timing rules.
     *
     * @param expectedId expected CAPTCHA id from session state
     * @param expectedAnswer expected CAPTCHA answer from session state
     * @param expectedPurpose purpose stored when the challenge was generated
     * @param createdAtEpochMillis challenge creation time in epoch milliseconds
     * @param currentAttempts attempts already used before this validation
     * @param submittedId submitted CAPTCHA id
     * @param submittedAnswer submitted answer text
     * @param requiredPurpose purpose required by the current endpoint
     * @return validation result including updated attempt count and whether the challenge should be cleared
     */
    public CaptchaValidationResult validateChallenge(String expectedId, Integer expectedAnswer,
                                                     String expectedPurpose, Long createdAtEpochMillis,
                                                     Integer currentAttempts, String submittedId,
                                                     String submittedAnswer, String requiredPurpose) {
        int attemptsUsed = normalizeAttempts(currentAttempts) + 1;
        if (expectedId == null || expectedAnswer == null || expectedPurpose == null || createdAtEpochMillis == null) {
            return CaptchaValidationResult.invalid(attemptsUsed, true);
        }
        if (attemptsUsed > MAX_VALIDATION_ATTEMPTS) {
            return CaptchaValidationResult.invalid(attemptsUsed, true);
        }
        if (!expectedPurpose.equals(requiredPurpose)) {
            return CaptchaValidationResult.invalid(attemptsUsed, true);
        }

        Instant createdAt = Instant.ofEpochMilli(createdAtEpochMillis);
        Instant now = Instant.now(clock);
        if (now.isAfter(createdAt.plus(CHALLENGE_TTL))) {
            return CaptchaValidationResult.invalid(attemptsUsed, true);
        }
        if (now.isBefore(createdAt.plus(MINIMUM_SOLVE_TIME))) {
            return CaptchaValidationResult.invalid(attemptsUsed, attemptsUsed >= MAX_VALIDATION_ATTEMPTS);
        }

        boolean valid = validate(expectedId, expectedAnswer, submittedId, submittedAnswer);
        return new CaptchaValidationResult(valid, attemptsUsed, valid || attemptsUsed >= MAX_VALIDATION_ATTEMPTS);
    }

    /**
     * Returns the current service time as epoch milliseconds.
     *
     * @return current time according to this service clock
     */
    public long currentTimeMillis() {
        return Instant.now(clock).toEpochMilli();
    }

    /**
     * Checks whether a CAPTCHA purpose is supported.
     *
     * @param purpose normalized purpose value
     * @return true for supported purpose values
     */
    public boolean isSupportedPurpose(String purpose) {
        return PURPOSE_LOGIN.equals(purpose) || PURPOSE_REGISTRATION.equals(purpose);
    }

    private MathChallenge createMathChallenge() {
        return switch (random.nextInt(5)) {
            case 0 -> createAdditionOrSubtractionChallenge();
            case 1 -> createThreeTermChallenge();
            case 2 -> createMultiplicationChallenge();
            case 3 -> createWordNumberChallenge();
            default -> createMissingOperandChallenge();
        };
    }

    private MathChallenge createAdditionOrSubtractionChallenge() {
        int left = random.nextInt(12) + 1;
        int right = random.nextInt(12) + 1;
        boolean addition = random.nextBoolean();
        if (!addition && right > left) {
            int swap = left;
            left = right;
            right = swap;
        }

        String operator = addition ? "+" : "-";
        int expectedAnswer = addition ? left + right : left - right;
        return new MathChallenge(left + " " + operator + " " + right + " = ?", expectedAnswer);
    }

    private MathChallenge createThreeTermChallenge() {
        int left = random.nextInt(9) + 2;
        int middle = random.nextInt(8) + 2;
        int right = random.nextInt(Math.min(left + middle, 9)) + 1;
        return new MathChallenge(left + " + " + middle + " - " + right + " = ?", left + middle - right);
    }

    private MathChallenge createMultiplicationChallenge() {
        int left = random.nextInt(8) + 2;
        int right = random.nextInt(8) + 2;
        return new MathChallenge(left + " * " + right + " = ?", left * right);
    }

    private MathChallenge createWordNumberChallenge() {
        String[] words = {"one", "two", "three", "four", "five", "six", "seven", "eight", "nine"};
        int left = random.nextInt(words.length) + 1;
        int right = random.nextInt(9) + 1;
        return new MathChallenge(words[left - 1] + " + " + right + " = ?", left + right);
    }

    private MathChallenge createMissingOperandChallenge() {
        int missing = random.nextInt(10) + 1;
        int right = random.nextInt(8) + 2;
        int total = missing + right;
        return new MathChallenge("? + " + right + " = " + total, missing);
    }

    private String renderSvg(String question) {
        return """
                <svg xmlns="http://www.w3.org/2000/svg" width="220" height="72" viewBox="0 0 220 72" role="img" aria-label="captcha">
                  <defs>
                    <filter id="captchaWarp" x="-10%%" y="-10%%" width="120%%" height="120%%">
                      <feTurbulence type="fractalNoise" baseFrequency="0.025 0.08" numOctaves="2" seed="%d"/>
                      <feDisplacementMap in="SourceGraphic" scale="1.8"/>
                    </filter>
                  </defs>
                  <rect width="220" height="72" fill="#f8fafc"/>
                  <g opacity="0.22">%s</g>
                  <g opacity="0.25">%s</g>
                  <g opacity="0.75">%s</g>
                  <g opacity="0.45">%s</g>
                  <g filter="url(#captchaWarp)">%s</g>
                  <path d="M8 53 C42 24, 67 65, 103 34 S163 22, 212 50" stroke="#64748b" stroke-width="1.4" fill="none" opacity="0.55"/>
                  <path d="M12 20 C48 6, 78 37, 113 18 S166 11, 207 32" stroke="#94a3b8" stroke-width="1.2" fill="none" opacity="0.5"/>
                </svg>
                """.formatted(random.nextInt(10_000), buildBackgroundGrid(), buildFaintShapes(),
                buildNoiseLines(), buildNoiseDots(), buildGlyphs(question));
    }

    private String buildBackgroundGrid() {
        StringBuilder grid = new StringBuilder();
        for (int x = 0; x <= 220; x += 14) {
            grid.append("<line x1=\"")
                    .append(x)
                    .append("\" y1=\"0\" x2=\"")
                    .append(x)
                    .append("\" y2=\"72\" stroke=\"#94a3b8\" stroke-width=\"0.6\"/>");
        }
        for (int y = 0; y <= 72; y += 12) {
            grid.append("<line x1=\"0\" y1=\"")
                    .append(y)
                    .append("\" x2=\"220\" y2=\"")
                    .append(y)
                    .append("\" stroke=\"#cbd5e1\" stroke-width=\"0.6\"/>");
        }
        return grid.toString();
    }

    private String buildFaintShapes() {
        StringBuilder shapes = new StringBuilder();
        for (int index = 0; index < 7; index++) {
            int cx = random.nextInt(200) + 10;
            int cy = random.nextInt(52) + 10;
            int radius = random.nextInt(13) + 6;
            String color = index % 2 == 0 ? "#bfdbfe" : "#fde68a";
            shapes.append("<circle cx=\"")
                    .append(cx)
                    .append("\" cy=\"")
                    .append(cy)
                    .append("\" r=\"")
                    .append(radius)
                    .append("\" fill=\"")
                    .append(color)
                    .append("\"/>");
        }
        return shapes.toString();
    }

    private String buildNoiseLines() {
        StringBuilder lines = new StringBuilder();
        for (int index = 0; index < 11; index++) {
            int x1 = random.nextInt(220);
            int y1 = random.nextInt(72);
            int x2 = random.nextInt(220);
            int y2 = random.nextInt(72);
            String color = index % 2 == 0 ? "#cbd5e1" : "#94a3b8";
            lines.append("<line x1=\"")
                    .append(x1)
                    .append("\" y1=\"")
                    .append(y1)
                    .append("\" x2=\"")
                    .append(x2)
                    .append("\" y2=\"")
                    .append(y2)
                    .append("\" stroke=\"")
                    .append(color)
                    .append("\" stroke-width=\"1\"/>");
        }
        return lines.toString();
    }

    private String buildNoiseDots() {
        StringBuilder dots = new StringBuilder();
        for (int index = 0; index < 70; index++) {
            int cx = random.nextInt(220);
            int cy = random.nextInt(72);
            int radiusTenths = random.nextInt(9) + 3;
            dots.append("<circle cx=\"")
                    .append(cx)
                    .append("\" cy=\"")
                    .append(cy)
                    .append("\" r=\"")
                    .append(radiusTenths / 10)
                    .append(".")
                    .append(radiusTenths % 10)
                    .append("\" fill=\"#64748b\"/>");
        }
        return dots.toString();
    }

    private String buildGlyphs(String question) {
        StringBuilder glyphs = new StringBuilder();
        int x = 20;
        for (int index = 0; index < question.length(); index++) {
            char character = question.charAt(index);
            if (character == ' ') {
                x += 6;
                continue;
            }

            int y = 43 + random.nextInt(9) - 4;
            int rotation = random.nextInt(17) - 8;
            int fontSize = 21 + random.nextInt(6);
            String color = index % 2 == 0 ? "#0f172a" : "#334155";
            glyphs.append("<text x=\"")
                    .append(x)
                    .append("\" y=\"")
                    .append(y)
                    .append("\" transform=\"rotate(")
                    .append(rotation)
                    .append(" ")
                    .append(x)
                    .append(" ")
                    .append(y)
                    .append(")\" font-family=\"Arial, sans-serif\" font-size=\"")
                    .append(fontSize)
                    .append("\" font-weight=\"700\" fill=\"")
                    .append(color)
                    .append("\">")
                    .append(escapeXml(String.valueOf(character)))
                    .append("</text>");
            if (random.nextBoolean()) {
                glyphs.append("<line x1=\"")
                        .append(x - 3)
                        .append("\" y1=\"")
                        .append(y - random.nextInt(16) - 3)
                        .append("\" x2=\"")
                        .append(x + 14)
                        .append("\" y2=\"")
                        .append(y + random.nextInt(8) - 4)
                        .append("\" stroke=\"#475569\" stroke-width=\"0.9\" opacity=\"0.55\"/>");
            }
            x += 14 + random.nextInt(4);
        }
        return glyphs.toString();
    }

    private int normalizeAttempts(Integer attempts) {
        return attempts == null ? 0 : Math.max(0, attempts);
    }

    private static String escapeXml(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private record MathChallenge(String question, int expectedAnswer) {
    }

    /**
     * Result of full session-backed CAPTCHA validation.
     *
     * @param valid true when the CAPTCHA answer is accepted
     * @param attemptsUsed attempts used after the current validation
     * @param challengeConsumed true when the session challenge should be cleared
     */
    public record CaptchaValidationResult(boolean valid, int attemptsUsed, boolean challengeConsumed) {

        private static CaptchaValidationResult invalid(int attemptsUsed, boolean challengeConsumed) {
            return new CaptchaValidationResult(false, attemptsUsed, challengeConsumed);
        }
    }
}
