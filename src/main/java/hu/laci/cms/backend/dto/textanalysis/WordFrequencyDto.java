package hu.laci.cms.backend.dto.textanalysis;

/**
 * One normalized word and its number of occurrences in a text-analysis result.
 */
public class WordFrequencyDto {

    private final String word;
    private final int count;

    /**
     * Creates one frequency result.
     *
     * @param word Locale.ROOT-normalized word
     * @param count occurrence count
     */
    public WordFrequencyDto(String word, int count) {
        this.word = word;
        this.count = count;
    }

    /**
     * Returns the normalized word.
     *
     * @return normalized word
     */
    public String getWord() {
        return word;
    }

    /**
     * Returns how many times the word occurred.
     *
     * @return occurrence count
     */
    public int getCount() {
        return count;
    }
}
