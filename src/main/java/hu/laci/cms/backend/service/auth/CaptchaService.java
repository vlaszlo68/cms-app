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
                  <g opacity="0.75">%s</g>
                  <g opacity="0.45">%s</g>
                  <g filter="url(#captchaWarp)">%s</g>
                  <path d="M8 53 C42 24, 67 65, 103 34 S163 22, 212 50" stroke="#64748b" stroke-width="1.4" fill="none" opacity="0.55"/>
                  <path d="M12 20 C48 6, 78 37, 113 18 S166 11, 207 32" stroke="#94a3b8" stroke-width="1.2" fill="none" opacity="0.5"/>
                </svg>
                """.formatted(random.nextInt(10_000), buildNoiseLines(), buildNoiseDots(), buildGlyphs(question));
    }

    private String buildNoiseLines() {
        StringBuilder lines = new StringBuilder();
        for (int index = 0; index < 7; index++) {
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
        for (int index = 0; index < 42; index++) {
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
        int x = 30;
        for (int index = 0; index < question.length(); index++) {
            char character = question.charAt(index);
            if (character == ' ') {
                x += 9;
                continue;
            }

            int y = 43 + random.nextInt(9) - 4;
            int rotation = random.nextInt(17) - 8;
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
                    .append(")\" font-family=\"Arial, sans-serif\" font-size=\"25\" font-weight=\"700\" fill=\"")
                    .append(color)
                    .append("\">")
                    .append(escapeXml(String.valueOf(character)))
                    .append("</text>");
            x += 20 + random.nextInt(5);
        }
        return glyphs.toString();
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
