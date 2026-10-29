package hu.laci.cms.backend.service.function;
import hu.laci.cms.backend.model.application.SiteApplication;
import hu.laci.cms.backend.model.site.Site;
import hu.laci.cms.backend.dto.auth.AuthenticatedUser;
import java.util.Objects;
import java.util.Optional;
/** Small immutable Site/Application domain context supplied to registered public functions. */
public record FunctionContext(Site site, SiteApplication siteApplication, Optional<AuthenticatedUser> authenticatedUser) {
    public FunctionContext(Site site, SiteApplication siteApplication) { this(site, siteApplication, Optional.empty()); }
    /**
     * Validates the resolved request context.
     *
     * @param site active Site serving the request
     * @param siteApplication active SiteApplication serving the request
     */
    public FunctionContext { Objects.requireNonNull(site, "site"); Objects.requireNonNull(siteApplication, "siteApplication"); Objects.requireNonNull(authenticatedUser, "authenticatedUser"); }
}
