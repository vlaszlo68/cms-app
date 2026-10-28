package hu.laci.cms.backend.service.textanalysis;

import hu.laci.cms.backend.dto.textanalysis.AnalyzeTextResponse;
import hu.laci.cms.backend.dto.textanalysis.WordFrequencyDto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Stateless, deterministic, pure-Java analysis of submitted text.
 *
 * <p>The service has no transport, persistence, registry, or external-service dependencies. It uses
 * Unicode code points for character and token-length metrics, {@link Character#isWhitespace(int)} for
 * whitespace, and {@link BreakIterator} with {@link Locale#ROOT} for word and sentence boundaries.</p>
 */
public class TextAnalysisService {

    /** Maximum accepted text size measured in Unicode code points. */
    public static final int MAX_TEXT_CODE_POINTS = 50_000;
    private static final int MAX_FREQUENT_WORDS = 10;

    /**
     * Validates and analyzes supplied text without persisting or sending it elsewhere.
     *
     * @param text text to analyze
     * @return deterministic metrics and at most ten normalized word frequencies
     * @throws IllegalArgumentException when text is absent, blank, or exceeds the code-point limit
     */
    public AnalyzeTextResponse analyze(String text) {
        validate(text);
        int characterCount = text.codePointCount(0, text.length());
        int withoutWhitespace = countNonWhitespaceCodePoints(text);
        Map<String, Integer> frequencies = new HashMap<>();
        WordMetrics wordMetrics = collectWords(text, frequencies);
        BigDecimal average = wordMetrics.count == 0 ? BigDecimal.ZERO
                : BigDecimal.valueOf(wordMetrics.codePoints).divide(BigDecimal.valueOf(wordMetrics.count), 2,
                RoundingMode.HALF_UP);
        return new AnalyzeTextResponse(characterCount, withoutWhitespace, wordMetrics.count, frequencies.size(),
                countSentences(text), countParagraphs(text), average, mostFrequentWords(frequencies));
    }

    private void validate(String text) {
        if (text == null) {
            throw new IllegalArgumentException("text is required.");
        }
        int codePointCount = text.codePointCount(0, text.length());
        if (codePointCount > MAX_TEXT_CODE_POINTS) {
            throw new IllegalArgumentException("text must contain at most " + MAX_TEXT_CODE_POINTS + " Unicode code points.");
        }
        if (text.codePoints().allMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException("text must contain at least one non-whitespace character.");
        }
    }

    private int countNonWhitespaceCodePoints(String text) {
        return (int) text.codePoints().filter(codePoint -> !Character.isWhitespace(codePoint)).count();
    }

    private WordMetrics collectWords(String text, Map<String, Integer> frequencies) {
        BreakIterator iterator = BreakIterator.getWordInstance(Locale.ROOT);
        iterator.setText(text);
        int count = 0;
        int codePoints = 0;
        int start = iterator.first();
        for (int end = iterator.next(); end != BreakIterator.DONE; start = end, end = iterator.next()) {
            String token = text.substring(start, end);
            if (!containsLetterOrDigit(token)) {
                continue;
            }
            count++;
            codePoints += token.codePointCount(0, token.length());
            String normalized = token.toLowerCase(Locale.ROOT);
            frequencies.merge(normalized, 1, Integer::sum);
        }
        return new WordMetrics(count, codePoints);
    }

    private boolean containsLetterOrDigit(String token) {
        return token.codePoints().anyMatch(Character::isLetterOrDigit);
    }

    private int countSentences(String text) {
        BreakIterator iterator = BreakIterator.getSentenceInstance(Locale.ROOT);
        iterator.setText(text);
        int count = 0;
        int start = iterator.first();
        for (int end = iterator.next(); end != BreakIterator.DONE; start = end, end = iterator.next()) {
            if (!text.substring(start, end).isBlank()) {
                count++;
            }
        }
        return count;
    }

    private int countParagraphs(String text) {
        String normalizedLineEndings = text.replace("\r\n", "\n").replace('\r', '\n');
        String[] lines = normalizedLineEndings.split("\n", -1);
        boolean insideParagraph = false;
        int count = 0;
        for (String line : lines) {
            if (line.codePoints().allMatch(Character::isWhitespace)) {
                insideParagraph = false;
            } else if (!insideParagraph) {
                count++;
                insideParagraph = true;
            }
        }
        return count;
    }

    private List<WordFrequencyDto> mostFrequentWords(Map<String, Integer> frequencies) {
        List<Map.Entry<String, Integer>> entries = new ArrayList<>(frequencies.entrySet());
        entries.sort(Comparator.<Map.Entry<String, Integer>>comparingInt(Map.Entry::getValue).reversed()
                .thenComparing(Map.Entry::getKey));
        List<WordFrequencyDto> results = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : entries) {
            if (results.size() == MAX_FREQUENT_WORDS) {
                break;
            }
            results.add(new WordFrequencyDto(entry.getKey(), entry.getValue()));
        }
        return results;
    }

    private record WordMetrics(int count, int codePoints) {
    }
}
