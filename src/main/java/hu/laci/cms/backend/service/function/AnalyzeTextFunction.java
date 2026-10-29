package hu.laci.cms.backend.service.function;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import hu.laci.cms.backend.dto.textanalysis.AnalyzeTextRequest;
import hu.laci.cms.backend.dto.textanalysis.AnalyzeTextResponse;
import hu.laci.cms.backend.service.textanalysis.TextAnalysisService;

import java.util.Objects;

/**
 * Transport adapter for the authenticated {@code TEXT_ANALYZER/ANALYZE_TEXT} function.
 * Authorization and CSRF verification are completed by the generic dispatcher before this handler runs.
 */
public final class AnalyzeTextFunction implements PublicFunction {

    /** Source-code application definition code. */
    public static final String APPLICATION_CODE = "TEXT_ANALYZER";
    /** Source-code function definition code. */
    public static final String FUNCTION_CODE = "ANALYZE_TEXT";

    private final Gson gson;
    private final TextAnalysisService textAnalysisService;

    /**
     * Creates the production handler with the pure analysis service.
     */
    public AnalyzeTextFunction() {
        this(new Gson(), new TextAnalysisService());
    }

    /**
     * Creates a handler with explicit dependencies for focused tests.
     *
     * @param gson JSON deserializer used only at the transport boundary
     * @param textAnalysisService pure analysis service
     */
    public AnalyzeTextFunction(Gson gson, TextAnalysisService textAnalysisService) {
        this.gson = Objects.requireNonNull(gson, "gson");
        this.textAnalysisService = Objects.requireNonNull(textAnalysisService, "textAnalysisService");
    }

    @Override
    public String getApplicationCode() {
        return APPLICATION_CODE;
    }

    @Override
    public String getFunctionCode() {
        return FUNCTION_CODE;
    }

    @Override
    public AccessPolicy getAccessPolicy() {
        return AccessPolicy.AUTHENTICATED;
    }

    /**
     * Deserializes the function payload and delegates all analysis to the pure service.
     *
     * @param context authorized generic function context
     * @param payload JSON object already validated by the dispatcher
     * @return typed text-analysis result
     * @throws PublicFunctionException for controlled malformed or invalid input
     */
    @Override
    public AnalyzeTextResponse execute(FunctionContext context, JsonObject payload) {
        try {
            if (!payload.has("text") || payload.get("text").isJsonNull()
                    || !payload.get("text").isJsonPrimitive()
                    || !payload.getAsJsonPrimitive("text").isString()) {
                throw new PublicFunctionException("INVALID_FUNCTION_INPUT", "Text must be a JSON string.");
            }
            AnalyzeTextRequest request = gson.fromJson(payload, AnalyzeTextRequest.class);
            if (request == null) {
                throw new PublicFunctionException("INVALID_FUNCTION_INPUT", "Function payload is required.");
            }
            return textAnalysisService.analyze(request.getText());
        } catch (PublicFunctionException exception) {
            throw exception;
        } catch (JsonParseException | IllegalArgumentException exception) {
            throw new PublicFunctionException("INVALID_FUNCTION_INPUT", exception.getMessage() == null
                    ? "Invalid text analysis request." : exception.getMessage());
        }
    }
}
