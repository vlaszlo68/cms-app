package hu.laci.cms.backend.service.application;

import hu.laci.cms.backend.config.e2e.E2ePublicAuthFixtures;
import hu.laci.cms.backend.service.e2e.E2ePublicAuthFixtureService;
import hu.laci.cms.backend.service.function.AccessPolicy;
import hu.laci.cms.backend.service.function.AnalyzeTextFunction;
import hu.laci.cms.backend.service.function.E2ePublicFunction;
import hu.laci.cms.backend.service.function.FunctionRegistry;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Single backend composition point for deployed application definitions and their public functions.
 *
 * <p>Database rows may enable only the application codes exposed by this registry. Future application
 * features add their explicit definitions and handlers here; they never provide executable metadata from
 * a database row or request payload.</p>
 */
public final class ApplicationPlatformRegistry {

    private static final ApplicationPlatformRegistry PRODUCTION = new ApplicationPlatformRegistry(
            List.of(new ApplicationDefinition(AnalyzeTextFunction.APPLICATION_CODE, AccessPolicy.AUTHENTICATED)),
            List.of(new AnalyzeTextFunction()));
    private static final ApplicationPlatformRegistry E2E_FIXTURES = new ApplicationPlatformRegistry(
            List.of(
                    new ApplicationDefinition(AnalyzeTextFunction.APPLICATION_CODE, AccessPolicy.AUTHENTICATED),
                    new ApplicationDefinition(E2ePublicAuthFixtureService.PUBLIC_APPLICATION_CODE,
                            AccessPolicy.PUBLIC),
                    new ApplicationDefinition(E2ePublicAuthFixtureService.AUTHENTICATED_APPLICATION_CODE,
                            AccessPolicy.AUTHENTICATED)),
            List.of(
                    new AnalyzeTextFunction(),
                    new E2ePublicFunction(E2ePublicAuthFixtureService.PUBLIC_APPLICATION_CODE,
                            E2ePublicAuthFixtureService.PUBLIC_FUNCTION_CODE, AccessPolicy.PUBLIC),
                    new E2ePublicFunction(E2ePublicAuthFixtureService.AUTHENTICATED_APPLICATION_CODE,
                            E2ePublicAuthFixtureService.AUTHENTICATED_FUNCTION_CODE, AccessPolicy.AUTHENTICATED),
                    new E2ePublicFunction(E2ePublicAuthFixtureService.AUTHENTICATED_APPLICATION_CODE,
                            E2ePublicAuthFixtureService.ADMIN_FUNCTION_CODE, AccessPolicy.ADMIN)));

    private final ApplicationDefinitionRegistry applicationDefinitions;
    private final FunctionRegistry publicFunctions;

    /**
     * Creates a platform registry from explicitly deployed definitions and handlers.
     *
     * @param applicationDefinitions supported application types
     * @param publicFunctions handlers registered for their exact application/function pair
     */
    public ApplicationPlatformRegistry(Collection<ApplicationDefinition> applicationDefinitions,
                                       Collection<? extends hu.laci.cms.backend.service.function.PublicFunction> publicFunctions) {
        this.applicationDefinitions = new ApplicationDefinitionRegistry(Objects.requireNonNull(applicationDefinitions));
        this.publicFunctions = new FunctionRegistry(Objects.requireNonNull(publicFunctions));
    }

    /**
     * Returns the deployed registry. The local E2E fixture registrations are added only after explicit
     * environment opt-in.
     *
     * @return immutable production platform registry
     */
    public static ApplicationPlatformRegistry production() {
        return E2ePublicAuthFixtures.isEnabled() ? E2E_FIXTURES : PRODUCTION;
    }

    /**
     * Returns the whitelist of supported application codes.
     *
     * @return application definition registry
     */
    public ApplicationDefinitionRegistry getApplicationDefinitions() {
        return applicationDefinitions;
    }

    /**
     * Returns the exact-pair registry used for public function dispatch.
     *
     * @return public function registry
     */
    public FunctionRegistry getPublicFunctions() {
        return publicFunctions;
    }
}
