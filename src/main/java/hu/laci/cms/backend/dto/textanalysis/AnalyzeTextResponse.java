package hu.laci.cms.backend.dto.textanalysis;

import java.math.BigDecimal;
import java.util.List;

/**
 * Typed deterministic analysis result returned by {@code TEXT_ANALYZER/ANALYZE_TEXT}.
 */
public class AnalyzeTextResponse {

    private final int characterCount;
    private final int characterCountWithoutWhitespace;
    private final int wordCount;
    private final int uniqueWordCount;
    private final int sentenceCount;
    private final int paragraphCount;
    private final BigDecimal averageWordLength;
    private final List<WordFrequencyDto> mostFrequentWords;

    /**
     * Creates a complete text-analysis response.
     *
     * @param characterCount Unicode code-point count
     * @param characterCountWithoutWhitespace code points excluding Unicode whitespace
     * @param wordCount detected word-token count
     * @param uniqueWordCount number of normalized distinct words
     * @param sentenceCount nonblank standard-library sentence segments
     * @param paragraphCount nonblank blocks separated by blank lines
     * @param averageWordLength two-decimal average detected token length
     * @param mostFrequentWords at most ten sorted normalized frequencies
     */
    public AnalyzeTextResponse(int characterCount, int characterCountWithoutWhitespace, int wordCount,
                               int uniqueWordCount, int sentenceCount, int paragraphCount,
                               BigDecimal averageWordLength, List<WordFrequencyDto> mostFrequentWords) {
        this.characterCount = characterCount;
        this.characterCountWithoutWhitespace = characterCountWithoutWhitespace;
        this.wordCount = wordCount;
        this.uniqueWordCount = uniqueWordCount;
        this.sentenceCount = sentenceCount;
        this.paragraphCount = paragraphCount;
        this.averageWordLength = averageWordLength;
        this.mostFrequentWords = List.copyOf(mostFrequentWords);
    }

    /** @return Unicode code points in the submitted text */
    public int getCharacterCount() { return characterCount; }

    /** @return Unicode code points that are not whitespace */
    public int getCharacterCountWithoutWhitespace() { return characterCountWithoutWhitespace; }

    /** @return detected tokens containing at least one letter or digit */
    public int getWordCount() { return wordCount; }

    /** @return case-normalized distinct word count */
    public int getUniqueWordCount() { return uniqueWordCount; }

    /** @return nonblank BreakIterator sentence-segment count */
    public int getSentenceCount() { return sentenceCount; }

    /** @return nonblank text blocks separated by blank lines */
    public int getParagraphCount() { return paragraphCount; }

    /** @return two-decimal average Unicode code-point token length */
    public BigDecimal getAverageWordLength() { return averageWordLength; }

    /** @return at most ten frequency-sorted normalized words */
    public List<WordFrequencyDto> getMostFrequentWords() { return mostFrequentWords; }
}
