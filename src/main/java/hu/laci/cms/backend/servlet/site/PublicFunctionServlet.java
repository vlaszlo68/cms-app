package hu.laci.cms.backend.servlet.site;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import hu.laci.cms.backend.model.application.SiteApplication;
import hu.laci.cms.backend.model.site.Site;
import hu.laci.cms.backend.model.user.User;
import hu.laci.cms.backend.dao.common.DaoRegistry;
import hu.laci.cms.backend.config.session.AppSession;
import hu.laci.cms.backend.config.session.AppSessionManager;
import hu.laci.cms.backend.dto.auth.AuthenticatedUser;
import hu.laci.cms.backend.servlet.support.CsrfTokenSupport;
import hu.laci.cms.backend.service.application.SiteApplicationService;
import hu.laci.cms.backend.service.function.*;
import hu.laci.cms.backend.service.site.SiteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.Reader;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Optional;
import java.util.Objects;
/**
 * Dispatcher component for the public function endpoint, intentionally without HTTP routing or business functions.
 * The Site servlet owns the URL mapping and delegates the exact Site/Application/Function request context here.
 */
public class PublicFunctionServlet {
    public static final String FUNCTION_NOT_FOUND="FUNCTION_NOT_FOUND", INVALID_FUNCTION_INPUT="INVALID_FUNCTION_INPUT", FUNCTION_EXECUTION_FAILED="FUNCTION_EXECUTION_FAILED";
    private static final Logger LOGGER=LoggerFactory.getLogger(PublicFunctionServlet.class);
    private final SiteService siteService; private final SiteApplicationService applicationService; private final FunctionRegistry registry; private final FunctionAuthorizationService authorizationService;
    /**
     * Creates a dispatcher using only trusted service and registry dependencies.
     *
     * @param siteService resolves publicly active Sites
     * @param applicationService resolves active supported Site Applications
     * @param registry resolves exact application/function handlers
     */
    public PublicFunctionServlet(SiteService siteService,SiteApplicationService applicationService,FunctionRegistry registry){this(siteService,applicationService,registry,new FunctionAuthorizationService(DaoRegistry.getDao(User.class)));}
    public PublicFunctionServlet(SiteService siteService,SiteApplicationService applicationService,FunctionRegistry registry,FunctionAuthorizationService authorizationService){this.siteService=Objects.requireNonNull(siteService);this.applicationService=Objects.requireNonNull(applicationService);this.registry=Objects.requireNonNull(registry);this.authorizationService=Objects.requireNonNull(authorizationService);}
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
        return dispatchInternal(siteCode, applicationCode, functionCode, body, Optional.empty(), null);
    }
    /** Dispatches with the server session principal and applies CSRF after authorization. */
    public Object dispatch(String siteCode,String applicationCode,String functionCode,Reader body,HttpServletRequest request,HttpServletResponse response){
        Optional<AppSession> session=AppSessionManager.findSession(request,response);
        Optional<AuthenticatedUser> principal=session.flatMap(AppSession::getAuthenticatedUser);
        return dispatchInternal(siteCode,applicationCode,functionCode,body,principal,session.orElse(null),request);
    }
    private Object dispatchInternal(String siteCode,String applicationCode,String functionCode,Reader body,Optional<AuthenticatedUser> principal,AppSession session){return dispatchInternal(siteCode,applicationCode,functionCode,body,principal,session,null);}
    private Object dispatchInternal(String siteCode,String applicationCode,String functionCode,Reader body,Optional<AuthenticatedUser> principal,AppSession session,HttpServletRequest request){
        Site site=siteService.requireActiveSite(siteCode); SiteApplication siteApplication=applicationService.requirePublic(site,applicationCode);
        FunctionRegistry.FunctionDefinition definition=registry.findDefinition(siteApplication.getApplicationCode(),functionCode).orElseThrow(()->new PublicFunctionException(FUNCTION_NOT_FOUND,"Function not found."));
        AccessPolicy applicationPolicy=applicationService.accessPolicy(siteApplication);
        if (applicationPolicy==null) applicationPolicy=AccessPolicy.PUBLIC;
        authorizationService.authorize(applicationPolicy,principal);
        Optional<AuthenticatedUser> authorized=authorizationService.authorize(definition.accessPolicy(),principal);
        if ((applicationPolicy!=AccessPolicy.PUBLIC || definition.accessPolicy()!=AccessPolicy.PUBLIC)
                && (session==null || request==null || !validCsrf(session,request))) throw new PublicFunctionException("CSRF_INVALID","Invalid CSRF token.");
        JsonObject payload=parseObject(body);
        try { Object result=definition.handler().execute(new FunctionContext(site,siteApplication,authorized),payload); LOGGER.info("Public function siteCode={} applicationCode={} functionCode={} userId={} success=true",siteCode,applicationCode,functionCode,authorized.map(AuthenticatedUser::getId).orElse(null));return result; }
        catch(PublicFunctionException e){LOGGER.info("Public function siteCode={} applicationCode={} functionCode={} success=false",siteCode,applicationCode,functionCode);throw e;}
        catch(RuntimeException e){LOGGER.warn("Public function siteCode={} applicationCode={} functionCode={} success=false",siteCode,applicationCode,functionCode);throw new PublicFunctionException(FUNCTION_EXECUTION_FAILED,"Function execution failed.");}
    }
    private boolean validCsrf(AppSession session,HttpServletRequest request){String token=session.getCsrfToken();return token!=null&&!token.isBlank()&&token.equals(request.getHeader(CsrfTokenSupport.HEADER_NAME));}
    private JsonObject parseObject(Reader body){try{if(body==null)throw new IllegalArgumentException();com.google.gson.JsonElement value=JsonParser.parseReader(body);if(!value.isJsonObject())throw new IllegalArgumentException();return value.getAsJsonObject();}catch(RuntimeException e){throw new PublicFunctionException(INVALID_FUNCTION_INPUT,"Function payload must be a JSON object.");}}
}
