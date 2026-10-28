package hu.laci.cms.backend.service.function;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Verifies the function registry has an explicit application/function namespace. */
class FunctionRegistryTest {
    @Test
    void onlyResolvesTheRegisteredApplicationFunctionPair() {
        FunctionRegistry registry = new FunctionRegistry(List.of(new TestFunction("APP_A", "LOOKUP")));
        assertTrue(registry.find("APP_A", "LOOKUP").isPresent());
        assertFalse(registry.find("APP_B", "LOOKUP").isPresent());
        assertFalse(registry.find("APP_A", "OTHER").isPresent());
    }

    private record TestFunction(String applicationCode, String functionCode) implements PublicFunction {
        @Override public String getApplicationCode() { return applicationCode; }
        @Override public String getFunctionCode() { return functionCode; }
        @Override public Object execute(FunctionContext context, JsonObject payload) { return new Object(); }
    }
}
