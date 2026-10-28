package hu.laci.cms.backend.service.function;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import hu.laci.cms.backend.dto.textanalysis.AnalyzeTextResponse;
import hu.laci.cms.backend.model.application.Application;
import hu.laci.cms.backend.model.site.Site;
import hu.laci.cms.backend.service.textanalysis.TextAnalysisService;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Tests the typed JSON boundary for the concrete text-analysis public function. */
class AnalyzeTextFunctionTest {

    @Test
    void deserializesARequestAndReturnsATypedResponse() {
        AnalyzeTextFunction function = new AnalyzeTextFunction(new Gson(), new TextAnalysisService());

        AnalyzeTextResponse response = function.execute(context(), JsonParser.parseString("{\"text\":\"Java java\"}").getAsJsonObject());

        assertEquals(2, response.getWordCount());
        assertEquals("java", response.getMostFrequentWords().get(0).getWord());
        assertEquals(2, response.getMostFrequentWords().get(0).getCount());
        assertEquals(AnalyzeTextFunction.APPLICATION_CODE, function.getApplicationCode());
        assertEquals(AnalyzeTextFunction.FUNCTION_CODE, function.getFunctionCode());
        assertEquals(AccessPolicy.AUTHENTICATED, function.getAccessPolicy());
        String serialized = new Gson().toJson(response);
        assertEquals(2, JsonParser.parseString(serialized).getAsJsonObject().get("wordCount").getAsInt());
        assertEquals("java", JsonParser.parseString(serialized).getAsJsonObject()
                .getAsJsonArray("mostFrequentWords").get(0).getAsJsonObject().get("word").getAsString());
    }

    @Test
    void returnsControlledValidationErrorForMissingText() {
        AnalyzeTextFunction function = new AnalyzeTextFunction(new Gson(), new TextAnalysisService());

        PublicFunctionException exception = assertThrows(PublicFunctionException.class,
                () -> function.execute(context(), JsonParser.parseString("{}").getAsJsonObject()));

        assertEquals("INVALID_FUNCTION_INPUT", exception.getCode());
    }

    @Test
    void returnsControlledValidationErrorForNonStringText() {
        AnalyzeTextFunction function = new AnalyzeTextFunction(new Gson(), new TextAnalysisService());

        for (String payload : new String[]{"{\"text\":123}", "{\"text\":true}", "{\"text\":{}}", "{\"text\":null}"}) {
            PublicFunctionException exception = assertThrows(PublicFunctionException.class,
                    () -> function.execute(context(), JsonParser.parseString(payload).getAsJsonObject()));

            assertEquals("INVALID_FUNCTION_INPUT", exception.getCode());
        }
    }

    private FunctionContext context() {
        Site site = new Site(1L, "site-a", "Site A", true);
        Application application = new Application(2L, site, AnalyzeTextFunction.APPLICATION_CODE, "Text", true);
        return new FunctionContext(site, application, Optional.empty());
    }
}
