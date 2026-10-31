package hu.laci.cms.backend.service.application;

import hu.laci.cms.backend.service.function.AccessPolicy;
import hu.laci.cms.backend.service.function.AnalyzeTextFunction;
import hu.laci.cms.backend.service.function.GetWeatherFunction;
import hu.laci.cms.backend.service.function.SearchLocationFunction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Verifies the production composition registers the concrete text analyzer only under its own code. */
class ApplicationPlatformRegistryTest {

    @Test
    void registersTextAnalyzerAndItsAuthenticatedAnalyzeFunction() {
        ApplicationPlatformRegistry registry = ApplicationPlatformRegistry.production();

        assertEquals(AccessPolicy.AUTHENTICATED, registry.getApplicationDefinitions()
                .find(AnalyzeTextFunction.APPLICATION_CODE).orElseThrow().accessPolicy());
        assertTrue(registry.getPublicFunctions().find(AnalyzeTextFunction.APPLICATION_CODE,
                AnalyzeTextFunction.FUNCTION_CODE).isPresent());
        assertTrue(registry.getPublicFunctions().find("OTHER_APPLICATION",
                AnalyzeTextFunction.FUNCTION_CODE).isEmpty());
    }

    @Test
    void registersWeatherExplorerPublicFunctionsOnlyUnderWeatherExplorer() {
        ApplicationPlatformRegistry registry = ApplicationPlatformRegistry.production();

        assertEquals(AccessPolicy.PUBLIC, registry.getApplicationDefinitions()
                .find(SearchLocationFunction.APPLICATION_CODE).orElseThrow().accessPolicy());
        assertTrue(registry.getPublicFunctions().find(SearchLocationFunction.APPLICATION_CODE,
                SearchLocationFunction.FUNCTION_CODE).isPresent());
        assertTrue(registry.getPublicFunctions().find(GetWeatherFunction.APPLICATION_CODE,
                GetWeatherFunction.FUNCTION_CODE).isPresent());
        assertTrue(registry.getPublicFunctions().find(AnalyzeTextFunction.APPLICATION_CODE,
                GetWeatherFunction.FUNCTION_CODE).isEmpty());
    }
}
