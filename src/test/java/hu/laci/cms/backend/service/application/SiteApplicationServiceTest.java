package hu.laci.cms.backend.service.application;

import hu.laci.cms.backend.dao.application.SiteApplicationDao;
import hu.laci.cms.backend.dto.application.CreateSiteApplicationRequest;
import hu.laci.cms.backend.dto.application.UpdateSiteApplicationRequest;
import hu.laci.cms.backend.model.application.SiteApplication;
import hu.laci.cms.backend.model.site.Site;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Focused Site-isolation tests for application administration. */
class SiteApplicationServiceTest {
    @Test
    void sameCodeMayExistOnDifferentSites() {
        SiteApplicationDao dao=Mockito.mock(SiteApplicationDao.class);
        Site a=new Site(1L,"a","A",true); Site b=new Site(2L,"b","B",true);
        Mockito.when(dao.findBySiteAndApplicationCode(a,"TEST_APP")).thenReturn(Optional.empty());
        Mockito.when(dao.findBySiteAndApplicationCode(b,"TEST_APP")).thenReturn(Optional.empty());
        Mockito.when(dao.create(Mockito.any())).thenAnswer(call->{SiteApplication app=call.getArgument(0);app.setId(9L);return app;});
        ApplicationDefinitionRegistry registry=new ApplicationDefinitionRegistry(List.of(new ApplicationDefinition("TEST_APP")));
        SiteApplicationService service=new SiteApplicationService(dao,registry);
        CreateSiteApplicationRequest request=new CreateSiteApplicationRequest();request.setCode("TEST_APP");request.setName("Test");
        assertEquals("TEST_APP",service.create(a,request).getCode());
        assertEquals("TEST_APP",service.create(b,request).getCode());
    }
    @Test
    void crossSiteIdCannotBeResolved() {
        SiteApplicationDao dao=Mockito.mock(SiteApplicationDao.class); Site a=new Site(1L,"a","A",true);
        Mockito.when(dao.findBySiteAndId(a,7L)).thenReturn(Optional.empty());
        SiteApplicationService service=new SiteApplicationService(dao,new ApplicationDefinitionRegistry(List.of()));
        SiteApplicationServiceException error=assertThrows(SiteApplicationServiceException.class,()->service.require(a,7L));
        assertEquals(SiteApplicationService.APPLICATION_NOT_FOUND,error.getCode());
    }

    @Test
    void listsGetsAndUpdatesOnlyApplicationsOwnedByTheRequestedSite() {
        SiteApplicationDao dao = Mockito.mock(SiteApplicationDao.class);
        Site site = new Site(1L, "site-a", "A", true);
        SiteApplication application = new SiteApplication(7L, site, "TEST_APP", "Original", true);
        Mockito.when(dao.findAllBySite(site)).thenReturn(List.of(application));
        Mockito.when(dao.findBySiteAndId(site, 7L)).thenReturn(Optional.of(application));
        Mockito.when(dao.findBySiteAndApplicationCode(site, "TEST_APP")).thenReturn(Optional.of(application));
        Mockito.when(dao.update(application)).thenReturn(application);
        SiteApplicationService service = new SiteApplicationService(dao,
                new ApplicationDefinitionRegistry(List.of(new ApplicationDefinition("TEST_APP"))));

        assertEquals(1, service.list(site).size());
        assertEquals("Original", service.get(site, 7L).getName());

        UpdateSiteApplicationRequest update = new UpdateSiteApplicationRequest();
        update.setCode("TEST_APP");
        update.setName("Renamed");
        update.setActive(false);
        assertEquals("Renamed", service.update(site, 7L, update).getName());
        assertEquals(false, application.isActive());
        Mockito.verify(dao).findAllBySite(site);
        Mockito.verify(dao, Mockito.times(2)).findBySiteAndId(site, 7L);
        Mockito.verify(dao).update(application);
    }
}
