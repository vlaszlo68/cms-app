package hu.laci.cms.backend.service.auth;

/**
 * Generated CAPTCHA challenge rendered as SVG and validated through the HTTP session.
 */
public class CaptchaChallenge {

    private final String id;
    private final int expectedAnswer;
    private final String svg;

    public CaptchaChallenge(String id, int expectedAnswer, String svg) {
        this.id = id;
        this.expectedAnswer = expectedAnswer;
        this.svg = svg;
    }

    public String getId() {
        return id;
    }

    public int getExpectedAnswer() {
        return expectedAnswer;
    }

    public String getSvg() {
        return svg;
    }
}
