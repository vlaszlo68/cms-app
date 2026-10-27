package hu.laci.cms.backend.service.application;

import hu.laci.cms.backend.dao.application.ApplicationDao;
import hu.laci.cms.backend.dto.application.CreateApplicationRequest;
import hu.laci.cms.backend.dto.application.UpdateApplicationRequest;
import hu.laci.cms.backend.model.application.Application;
import hu.laci.cms.backend.model.site.Site;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Focused Site-isolation tests for application administration. */
class ApplicationServiceTest {
    @Test
    void sameCodeMayExistOnDifferentSites() {
        ApplicationDao dao=Mockito.mock(ApplicationDao.class);
        Site a=new Site(1L,"a","A",true); Site b=new Site(2L,"b","B",true);
        Mockito.when(dao.findBySiteAndCode(a,"TEST_APP")).thenReturn(Optional.empty());
        Mockito.when(dao.findBySiteAndCode(b,"TEST_APP")).thenReturn(Optional.empty());
        Mockito.when(dao.create(Mockito.any())).thenAnswer(call->{Application app=call.getArgument(0);app.setId(9L);return app;});
        ApplicationDefinitionRegistry registry=new ApplicationDefinitionRegistry(List.of(new ApplicationDefinition("TEST_APP")));
        ApplicationService service=new ApplicationService(dao,registry);
        CreateApplicationRequest request=new CreateApplicationRequest();request.setCode("TEST_APP");request.setName("Test");
        assertEquals("TEST_APP",service.create(a,request).getCode());
        assertEquals("TEST_APP",service.create(b,request).getCode());
    }
    @Test
    void crossSiteIdCannotBeResolved() {
        ApplicationDao dao=Mockito.mock(ApplicationDao.class); Site a=new Site(1L,"a","A",true);
        Mockito.when(dao.findBySiteAndId(a,7L)).thenReturn(Optional.empty());
        ApplicationService service=new ApplicationService(dao,new ApplicationDefinitionRegistry(List.of()));
        ApplicationServiceException error=assertThrows(ApplicationServiceException.class,()->service.require(a,7L));
        assertEquals(ApplicationService.APPLICATION_NOT_FOUND,error.getCode());
    }

    @Test
    void listsGetsAndUpdatesOnlyApplicationsOwnedByTheRequestedSite() {
        ApplicationDao dao = Mockito.mock(ApplicationDao.class);
        Site site = new Site(1L, "site-a", "A", true);
        Application application = new Application(7L, site, "TEST_APP", "Original", true);
        Mockito.when(dao.findAllBySite(site)).thenReturn(List.of(application));
        Mockito.when(dao.findBySiteAndId(site, 7L)).thenReturn(Optional.of(application));
        Mockito.when(dao.findBySiteAndCode(site, "TEST_APP")).thenReturn(Optional.of(application));
        Mockito.when(dao.update(application)).thenReturn(application);
        ApplicationService service = new ApplicationService(dao,
                new ApplicationDefinitionRegistry(List.of(new ApplicationDefinition("TEST_APP"))));

        assertEquals(1, service.list(site).size());
        assertEquals("Original", service.get(site, 7L).getName());

        UpdateApplicationRequest update = new UpdateApplicationRequest();
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
