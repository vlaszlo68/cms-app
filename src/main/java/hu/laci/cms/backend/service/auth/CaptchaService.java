package hu.laci.cms.backend.service.auth;

import java.security.SecureRandom;
import java.util.UUID;

/**
 * Generates simple math CAPTCHA challenges and validates one-time answers.
 */
public class CaptchaService {

    public static final String SESSION_ID_ATTRIBUTE = "registrationCaptchaId";
    public static final String SESSION_ANSWER_ATTRIBUTE = "registrationCaptchaAnswer";

    private final SecureRandom random = new SecureRandom();

    public CaptchaChallenge createChallenge() {
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
        String question = left + " " + operator + " " + right + " = ?";
        return new CaptchaChallenge(UUID.randomUUID().toString(), expectedAnswer, renderSvg(question));
    }

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

    private static String renderSvg(String question) {
        String escapedQuestion = escapeXml(question);
        return """
                <svg xmlns="http://www.w3.org/2000/svg" width="180" height="64" viewBox="0 0 180 64" role="img" aria-label="captcha">
                  <rect width="180" height="64" fill="#f7fafc"/>
                  <path d="M8 18 C38 4, 52 36, 86 21 S132 13, 172 38" stroke="#94a3b8" stroke-width="2" fill="none"/>
                  <path d="M12 48 C42 35, 70 58, 102 42 S142 29, 170 50" stroke="#cbd5e1" stroke-width="2" fill="none"/>
                  <text x="90" y="39" text-anchor="middle" font-family="Arial, sans-serif" font-size="24" font-weight="700" fill="#1f2937">%s</text>
                </svg>
                """.formatted(escapedQuestion);
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
}
