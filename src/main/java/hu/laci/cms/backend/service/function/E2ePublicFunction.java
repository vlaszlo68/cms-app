package hu.laci.cms.backend.service.function;

import com.google.gson.JsonObject;
import hu.laci.cms.backend.dto.auth.AuthenticatedUser;
import hu.laci.cms.backend.service.e2e.E2ePublicAuthFixtureService;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Minimal non-business function used only by the opt-in local E2E fixture set.
 */
public final class E2ePublicFunction implements PublicFunction {

    private final String applicationCode;
    private final String functionCode;
    private final AccessPolicy accessPolicy;

    /**
     * Creates one fixture function definition.
     *
     * @param applicationCode fixture application owner
     * @param functionCode fixture function identifier
     * @param accessPolicy policy verified before this handler is invoked
     */
    public E2ePublicFunction(String applicationCode, String functionCode, AccessPolicy accessPolicy) {
        this.applicationCode = applicationCode;
        this.functionCode = functionCode;
        this.accessPolicy = accessPolicy;
    }

    @Override
    public String getApplicationCode() {
        return applicationCode;
    }

    @Override
    public String getFunctionCode() {
        return functionCode;
    }

    @Override
    public AccessPolicy getAccessPolicy() {
        return accessPolicy;
    }

    @Override
    public Object execute(FunctionContext context, JsonObject payload) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("functionCode", functionCode);
        response.put("siteCode", context.site().getCode());
        context.authenticatedUser().ifPresent(user -> addPrincipal(response, user));
        return response;
    }

    private void addPrincipal(Map<String, Object> response, AuthenticatedUser user) {
        response.put("userId", user.getId());
        response.put("email", user.getEmail());
        response.put("role", user.getRole().name());
    }
}
