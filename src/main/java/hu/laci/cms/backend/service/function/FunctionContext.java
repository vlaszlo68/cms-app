package hu.laci.cms.backend.service.function;
import hu.laci.cms.backend.model.application.Application;
import hu.laci.cms.backend.model.site.Site;
import java.util.Objects;
/** Small immutable Site/Application domain context supplied to registered public functions. */
public record FunctionContext(Site site, Application application) {
    /**
     * Validates the resolved request context.
     *
     * @param site active Site serving the request
     * @param application active Site-owned Application serving the request
     */
    public FunctionContext { Objects.requireNonNull(site, "site"); Objects.requireNonNull(application, "application"); }
}
