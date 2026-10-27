package hu.laci.cms.backend.servlet.site;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import hu.laci.cms.backend.model.application.Application;
import hu.laci.cms.backend.model.site.Site;
import hu.laci.cms.backend.service.application.ApplicationService;
import hu.laci.cms.backend.service.function.*;
import hu.laci.cms.backend.service.site.SiteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.Reader;
import java.util.Objects;
/**
 * Dispatcher component for the public function endpoint, intentionally without HTTP routing or business functions.
 * The Site servlet owns the URL mapping and delegates the exact Site/Application/Function request context here.
 */
public class PublicFunctionServlet {
    public static final String FUNCTION_NOT_FOUND="FUNCTION_NOT_FOUND", INVALID_FUNCTION_INPUT="INVALID_FUNCTION_INPUT", FUNCTION_EXECUTION_FAILED="FUNCTION_EXECUTION_FAILED";
    private static final Logger LOGGER=LoggerFactory.getLogger(PublicFunctionServlet.class);
    private final SiteService siteService; private final ApplicationService applicationService; private final FunctionRegistry registry;
    /**
     * Creates a dispatcher using only trusted service and registry dependencies.
     *
     * @param siteService resolves publicly active Sites
     * @param applicationService resolves active supported Site Applications
     * @param registry resolves exact application/function handlers
     */
    public PublicFunctionServlet(SiteService siteService,ApplicationService applicationService,FunctionRegistry registry){this.siteService=Objects.requireNonNull(siteService);this.applicationService=Objects.requireNonNull(applicationService);this.registry=Objects.requireNonNull(registry);}
    /**
     * Resolves request context, validates a JSON object payload, and invokes an explicit handler.
     *
     * @param siteCode Site path value
     * @param applicationCode Application path value
     * @param functionCode function path value
     * @param body JSON request body
     * @return handler result for the normal API response envelope
     * @throws PublicFunctionException for controlled input, lookup, or execution failures
     */
    public Object dispatch(String siteCode,String applicationCode,String functionCode,Reader body){
        Site site=siteService.requireActiveSite(siteCode); Application application=applicationService.requirePublic(site,applicationCode);
        PublicFunction function=registry.find(application.getCode(),functionCode).orElseThrow(()->new PublicFunctionException(FUNCTION_NOT_FOUND,"Function not found."));
        JsonObject payload=parseObject(body);
        try { Object result=function.execute(new FunctionContext(site,application),payload); LOGGER.info("Public function siteCode={} applicationCode={} functionCode={} success=true",siteCode,applicationCode,functionCode);return result; }
        catch(PublicFunctionException e){LOGGER.info("Public function siteCode={} applicationCode={} functionCode={} success=false",siteCode,applicationCode,functionCode);throw e;}
        catch(RuntimeException e){LOGGER.warn("Public function siteCode={} applicationCode={} functionCode={} success=false",siteCode,applicationCode,functionCode);throw new PublicFunctionException(FUNCTION_EXECUTION_FAILED,"Function execution failed.");}
    }
    private JsonObject parseObject(Reader body){try{if(body==null)throw new IllegalArgumentException();com.google.gson.JsonElement value=JsonParser.parseReader(body);if(!value.isJsonObject())throw new IllegalArgumentException();return value.getAsJsonObject();}catch(RuntimeException e){throw new PublicFunctionException(INVALID_FUNCTION_INPUT,"Function payload must be a JSON object.");}}
}
