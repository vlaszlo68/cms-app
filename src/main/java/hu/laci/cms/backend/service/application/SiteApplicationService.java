package hu.laci.cms.backend.service.application;
import hu.laci.cms.backend.dao.application.SiteApplicationDao;
import hu.laci.cms.backend.dto.application.*;
import hu.laci.cms.backend.model.application.SiteApplication;
import hu.laci.cms.backend.model.site.Site;
import hu.laci.cms.backend.service.function.AccessPolicy;
import java.util.List;
import java.util.Objects;
/** Business layer for Site-owned application configuration and public eligibility. */
public class SiteApplicationService {
    public static final String VALIDATION_ERROR="VALIDATION_ERROR", APPLICATION_NOT_FOUND="APPLICATION_NOT_FOUND", DUPLICATE_APPLICATION_CODE="DUPLICATE_APPLICATION_CODE", UNSUPPORTED_APPLICATION="UNSUPPORTED_APPLICATION";
    private final SiteApplicationDao dao; private final ApplicationDefinitionRegistry registry;
    /** @param dao application persistence dependency @param registry deployed-code whitelist */
    public SiteApplicationService(SiteApplicationDao dao, ApplicationDefinitionRegistry registry) { this.dao=Objects.requireNonNull(dao);this.registry=Objects.requireNonNull(registry); }
    /** @param site owning Site @return only applications belonging to the Site */
    public List<SiteApplicationResponse> list(Site site){requireSite(site);return dao.findAllBySite(site).stream().map(this::response).toList();}
    /** @param site owning Site @param id application id @return application response @throws SiteApplicationServiceException when absent */
    public SiteApplicationResponse get(Site site,Long id){return response(require(site,id));}
    /** @param site owning Site @param request configuration request @return created application @throws SiteApplicationServiceException for invalid or duplicate input */
    public SiteApplicationResponse create(Site site,CreateSiteApplicationRequest request){validate(request); String applicationCode=request.getCode().trim(); requireSupported(applicationCode); if(dao.findBySiteAndApplicationCode(site,applicationCode).isPresent()) throw new SiteApplicationServiceException(DUPLICATE_APPLICATION_CODE,"code is already used for this Site."); return response(dao.create(new SiteApplication(null,site,applicationCode,request.getName().trim(),request.getActive()==null||request.getActive())));}
    /**
     * Updates a Site-owned application after validating its deployed code and Site-scoped uniqueness.
     *
     * @param site owning Site
     * @param id application identifier within the Site
     * @param request replacement configuration
     * @return updated application response
     * @throws SiteApplicationServiceException when input is invalid, unsupported, duplicated, or absent
     */
    public SiteApplicationResponse update(Site site,Long id,UpdateSiteApplicationRequest request){validate(request); SiteApplication siteApplication=require(site,id);String applicationCode=request.getCode().trim();requireSupported(applicationCode);dao.findBySiteAndApplicationCode(site,applicationCode).filter(found->!found.getId().equals(id)).ifPresent(found->{throw new SiteApplicationServiceException(DUPLICATE_APPLICATION_CODE,"code is already used for this Site.");});siteApplication.setApplicationCode(applicationCode);siteApplication.setName(request.getName().trim());siteApplication.setActive(request.getActive()==null||request.getActive());return response(dao.update(siteApplication));}
    /**
     * Resolves an application that is eligible for anonymous public access.
     *
     * @param site active public Site
     * @param code application code from the public URL
     * @return active, Site-owned, explicitly supported application
     * @throws SiteApplicationServiceException when the application is absent, inactive, or unsupported
     */
    public SiteApplication requirePublic(Site site,String applicationCode){if(applicationCode==null||applicationCode.isBlank())throw new SiteApplicationServiceException(APPLICATION_NOT_FOUND,"Application not found."); SiteApplication siteApplication=dao.findBySiteAndApplicationCode(site,applicationCode.trim()).orElseThrow(()->new SiteApplicationServiceException(APPLICATION_NOT_FOUND,"Application not found.")); if(!siteApplication.isActive()||!registry.isSupported(siteApplication.getApplicationCode()))throw new SiteApplicationServiceException(APPLICATION_NOT_FOUND,"Application not found.");return siteApplication;}
    /**
     * Resolves an application strictly within its owning Site.
     *
     * @param site owning Site
     * @param id application identifier
     * @return matching application
     * @throws SiteApplicationServiceException when the Site or identifier is invalid or no matching row exists
     */
    public SiteApplication require(Site site,Long id){requireSite(site);if(id==null)throw new SiteApplicationServiceException(VALIDATION_ERROR,"applicationId is required.");return dao.findBySiteAndId(site,id).orElseThrow(()->new SiteApplicationServiceException(APPLICATION_NOT_FOUND,"Application not found."));}
    private void validate(SiteApplicationRequestBase request){if(request==null||request.getCode()==null||request.getCode().isBlank()||request.getName()==null||request.getName().isBlank())throw new SiteApplicationServiceException(VALIDATION_ERROR,"code and name are required.");}
    private void requireSupported(String applicationCode){if(!registry.isSupported(applicationCode))throw new SiteApplicationServiceException(UNSUPPORTED_APPLICATION,"Application code is not supported.");}
    private static void requireSite(Site site){if(site==null||site.getId()==null)throw new SiteApplicationServiceException(VALIDATION_ERROR,"Site is required.");}
    private SiteApplicationResponse response(SiteApplication siteApplication){return new SiteApplicationResponse(siteApplication.getId(),siteApplication.getApplicationCode(),siteApplication.getName(),siteApplication.isActive());}
    /**
     * Maps an eligible application to the minimal anonymous descriptor.
     *
     * @param app public application resolved by this service
     * @return descriptor without audit or administration fields
     */
    public PublicApplicationResponse publicResponse(SiteApplication siteApplication){return new PublicApplicationResponse(siteApplication.getApplicationCode(),siteApplication.getName(),registry.find(siteApplication.getApplicationCode()).orElseThrow().accessPolicy());}
    /** @return trusted code-defined access policy for a previously resolved application */
    public AccessPolicy accessPolicy(SiteApplication siteApplication){return registry.find(siteApplication.getApplicationCode()).orElseThrow().accessPolicy();}
}
