package hu.laci.cms.backend.service.application;

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

    private static final ApplicationPlatformRegistry EMPTY = new ApplicationPlatformRegistry(List.of(), List.of());

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
     * Returns the production registry. It is intentionally empty until a concrete application feature registers one.
     *
     * @return immutable production platform registry
     */
    public static ApplicationPlatformRegistry production() {
        return EMPTY;
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
