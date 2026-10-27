package hu.laci.cms.backend.service.application;
import hu.laci.cms.backend.dao.application.ApplicationDao;
import hu.laci.cms.backend.dto.application.*;
import hu.laci.cms.backend.model.application.Application;
import hu.laci.cms.backend.model.site.Site;
import java.util.List;
import java.util.Objects;
/** Business layer for Site-owned application configuration and public eligibility. */
public class ApplicationService {
    public static final String VALIDATION_ERROR="VALIDATION_ERROR", APPLICATION_NOT_FOUND="APPLICATION_NOT_FOUND", DUPLICATE_APPLICATION_CODE="DUPLICATE_APPLICATION_CODE", UNSUPPORTED_APPLICATION="UNSUPPORTED_APPLICATION";
    private final ApplicationDao dao; private final ApplicationDefinitionRegistry registry;
    /** @param dao application persistence dependency @param registry deployed-code whitelist */
    public ApplicationService(ApplicationDao dao, ApplicationDefinitionRegistry registry) { this.dao=Objects.requireNonNull(dao);this.registry=Objects.requireNonNull(registry); }
    /** @param site owning Site @return only applications belonging to the Site */
    public List<ApplicationResponse> list(Site site){requireSite(site);return dao.findAllBySite(site).stream().map(this::response).toList();}
    /** @param site owning Site @param id application id @return application response @throws ApplicationServiceException when absent */
    public ApplicationResponse get(Site site,Long id){return response(require(site,id));}
    /** @param site owning Site @param request configuration request @return created application @throws ApplicationServiceException for invalid or duplicate input */
    public ApplicationResponse create(Site site,CreateApplicationRequest request){validate(request); String code=request.getCode().trim(); requireSupported(code); if(dao.findBySiteAndCode(site,code).isPresent()) throw new ApplicationServiceException(DUPLICATE_APPLICATION_CODE,"code is already used for this Site."); return response(dao.create(new Application(null,site,code,request.getName().trim(),request.getActive()==null||request.getActive())));}
    /**
     * Updates a Site-owned application after validating its deployed code and Site-scoped uniqueness.
     *
     * @param site owning Site
     * @param id application identifier within the Site
     * @param request replacement configuration
     * @return updated application response
     * @throws ApplicationServiceException when input is invalid, unsupported, duplicated, or absent
     */
    public ApplicationResponse update(Site site,Long id,UpdateApplicationRequest request){validate(request); Application app=require(site,id);String code=request.getCode().trim();requireSupported(code);dao.findBySiteAndCode(site,code).filter(found->!found.getId().equals(id)).ifPresent(found->{throw new ApplicationServiceException(DUPLICATE_APPLICATION_CODE,"code is already used for this Site.");});app.setCode(code);app.setName(request.getName().trim());app.setActive(request.getActive()==null||request.getActive());return response(dao.update(app));}
    /**
     * Resolves an application that is eligible for anonymous public access.
     *
     * @param site active public Site
     * @param code application code from the public URL
     * @return active, Site-owned, explicitly supported application
     * @throws ApplicationServiceException when the application is absent, inactive, or unsupported
     */
    public Application requirePublic(Site site,String code){if(code==null||code.isBlank())throw new ApplicationServiceException(APPLICATION_NOT_FOUND,"Application not found."); Application app=dao.findBySiteAndCode(site,code.trim()).orElseThrow(()->new ApplicationServiceException(APPLICATION_NOT_FOUND,"Application not found.")); if(!app.isActive()||!registry.isSupported(app.getCode()))throw new ApplicationServiceException(APPLICATION_NOT_FOUND,"Application not found.");return app;}
    /**
     * Resolves an application strictly within its owning Site.
     *
     * @param site owning Site
     * @param id application identifier
     * @return matching application
     * @throws ApplicationServiceException when the Site or identifier is invalid or no matching row exists
     */
    public Application require(Site site,Long id){requireSite(site);if(id==null)throw new ApplicationServiceException(VALIDATION_ERROR,"applicationId is required.");return dao.findBySiteAndId(site,id).orElseThrow(()->new ApplicationServiceException(APPLICATION_NOT_FOUND,"Application not found."));}
    private void validate(ApplicationRequestBase request){if(request==null||request.getCode()==null||request.getCode().isBlank()||request.getName()==null||request.getName().isBlank())throw new ApplicationServiceException(VALIDATION_ERROR,"code and name are required.");}
    private void requireSupported(String code){if(!registry.isSupported(code))throw new ApplicationServiceException(UNSUPPORTED_APPLICATION,"Application code is not supported.");}
    private static void requireSite(Site site){if(site==null||site.getId()==null)throw new ApplicationServiceException(VALIDATION_ERROR,"Site is required.");}
    private ApplicationResponse response(Application app){return new ApplicationResponse(app.getId(),app.getCode(),app.getName(),app.isActive());}
    /**
     * Maps an eligible application to the minimal anonymous descriptor.
     *
     * @param app public application resolved by this service
     * @return descriptor without audit or administration fields
     */
    public PublicApplicationResponse publicResponse(Application app){return new PublicApplicationResponse(app.getCode(),app.getName());}
}
