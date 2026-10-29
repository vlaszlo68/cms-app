package hu.laci.cms.backend.dto.textanalysis;

/**
 * Transport request for the {@code TEXT_ANALYZER/ANALYZE_TEXT} public function.
 */
public class AnalyzeTextRequest {

    private String text;

    /**
     * Creates an empty request for Gson deserialization.
     */
    public AnalyzeTextRequest() {
    }

    /**
     * Returns the text to analyze.
     *
     * @return supplied text, possibly {@code null} before validation
     */
    public String getText() {
        return text;
    }

    /**
     * Sets the text to analyze.
     *
     * @param text supplied text
     */
    public void setText(String text) {
        this.text = text;
    }
}
