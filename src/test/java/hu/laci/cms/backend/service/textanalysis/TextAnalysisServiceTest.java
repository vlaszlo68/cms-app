package hu.laci.cms.backend.service.textanalysis;

import hu.laci.cms.backend.dto.textanalysis.AnalyzeTextResponse;
import hu.laci.cms.backend.dto.textanalysis.WordFrequencyDto;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Verifies deterministic, Unicode-aware local text-analysis semantics. */
class TextAnalysisServiceTest {

    private final TextAnalysisService service = new TextAnalysisService();

    @Test
    void analyzesWordsSentencesParagraphsAndNormalizedFrequencies() {
        AnalyzeTextResponse response = service.analyze("Java java backend Java backend servlet.\n\nSecond paragraph!");

        assertEquals(8, response.getWordCount());
        assertEquals(5, response.getUniqueWordCount());
        assertEquals(2, response.getSentenceCount());
        assertEquals(2, response.getParagraphCount());
        assertEquals(new BigDecimal("6.00"), response.getAverageWordLength());
        List<WordFrequencyDto> frequencies = response.getMostFrequentWords();
        assertEquals(List.of("java", "backend", "paragraph", "second", "servlet"),
                frequencies.stream().map(WordFrequencyDto::getWord).toList());
        assertEquals(List.of(3, 2, 1, 1, 1), frequencies.stream().map(WordFrequencyDto::getCount).toList());
    }

    @Test
    void usesUnicodeCodePointsAndUnicodeWhitespace() {
        String text = "Árvíztűrő 😊\u2003Java";
        AnalyzeTextResponse response = service.analyze(text);

        assertEquals(text.codePointCount(0, text.length()), response.getCharacterCount());
        assertEquals(14, response.getCharacterCountWithoutWhitespace());
        assertEquals(2, response.getWordCount());
        assertEquals(1, response.getParagraphCount());
    }

    @Test
    void countsUnixAndWindowsBlankLineParagraphSeparators() {
        assertEquals(2, service.analyze("first\r\n\r\nsecond").getParagraphCount());
        assertEquals(2, service.analyze("first\n \t\nsecond").getParagraphCount());
    }

    @Test
    void handlesPunctuationWithoutWordsOrDivisionByZero() {
        AnalyzeTextResponse response = service.analyze("... !!!");

        assertEquals(0, response.getWordCount());
        assertEquals(0, response.getUniqueWordCount());
        assertEquals(BigDecimal.ZERO, response.getAverageWordLength());
        assertEquals(List.of(), response.getMostFrequentWords());
    }

    @Test
    void ordersFrequencyTiesAndLimitsTheResponseToTenWords() {
        AnalyzeTextResponse response = service.analyze("k j i h g f e d c b a");

        assertEquals(10, response.getMostFrequentWords().size());
        assertEquals("a", response.getMostFrequentWords().get(0).getWord());
        assertEquals("j", response.getMostFrequentWords().get(9).getWord());
    }

    @Test
    void rejectsMissingBlankAndOversizedText() {
        assertThrows(IllegalArgumentException.class, () -> service.analyze(null));
        assertThrows(IllegalArgumentException.class, () -> service.analyze(" \t\n"));
        assertThrows(IllegalArgumentException.class,
                () -> service.analyze("a".repeat(TextAnalysisService.MAX_TEXT_CODE_POINTS + 1)));
    }
}
