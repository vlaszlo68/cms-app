package hu.laci.cms.backend.servlet.page;

import hu.laci.cms.backend.dto.auth.AuthenticatedUser;
import hu.laci.cms.backend.model.user.UserRole;
import hu.laci.cms.backend.service.page.PageBlockService;
import hu.laci.cms.backend.servlet.support.ServletTestSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/** Unit tests for individual page-block endpoint dispatch and validation. */
class PageBlockServletTest {

    @Test
    void dispatchesGetCreateUpdateAndDelete() throws Exception {
        PageBlockService service = Mockito.mock(PageBlockService.class);
        PageBlockServlet servlet = new PageBlockServlet();
        ServletTestSupport.setField(servlet, "pageBlockService", service);

        invokeGet(servlet, "/3", service);
        invokePost(servlet, service);
        invokePut(servlet, "/3", service);
        invokeDelete(servlet, "/3", service);

        Mockito.verify(service).getBlock(3L);
        Mockito.verify(service).createBlock(Mockito.any());
        Mockito.verify(service).updateBlock(Mockito.eq(3L), Mockito.any());
        Mockito.verify(service).deleteBlock(3L);
    }

    @Test
    void rejectsCollectionGetWithoutBlockId() throws Exception {
        PageBlockService service = Mockito.mock(PageBlockService.class);
        PageBlockServlet servlet = new PageBlockServlet();
        ServletTestSupport.setField(servlet, "pageBlockService", service);
        javax.servlet.http.HttpServletRequest request = ServletTestSupport.request().build();
        ServletTestSupport.ResponseFixture response = ServletTestSupport.response();

        ServletTestSupport.runAsAuthenticatedUser(request, response, admin(), () -> servlet.doGet(request, response.build()));

        Assertions.assertEquals(400, response.getStatus());
        Mockito.verifyNoInteractions(service);
    }

    private void invokeGet(PageBlockServlet servlet, String path, PageBlockService service) throws Exception {
        javax.servlet.http.HttpServletRequest request = ServletTestSupport.request().withPathInfo(path).build();
        ServletTestSupport.ResponseFixture response = ServletTestSupport.response();
        ServletTestSupport.runAsAuthenticatedUser(request, response, admin(), () -> servlet.doGet(request, response.build()));
    }

    private void invokePost(PageBlockServlet servlet, PageBlockService service) throws Exception {
        javax.servlet.http.HttpServletRequest request = ServletTestSupport.request().withJsonBody("{}").build();
        ServletTestSupport.ResponseFixture response = ServletTestSupport.response();
        ServletTestSupport.runAsAuthenticatedUser(request, response, admin(), () -> servlet.doPost(request, response.build()));
        Assertions.assertEquals(201, response.getStatus());
    }

    private void invokePut(PageBlockServlet servlet, String path, PageBlockService service) throws Exception {
        javax.servlet.http.HttpServletRequest request = ServletTestSupport.request().withPathInfo(path).withJsonBody("{}").build();
        ServletTestSupport.ResponseFixture response = ServletTestSupport.response();
        ServletTestSupport.runAsAuthenticatedUser(request, response, admin(), () -> servlet.doPut(request, response.build()));
    }

    private void invokeDelete(PageBlockServlet servlet, String path, PageBlockService service) throws Exception {
        javax.servlet.http.HttpServletRequest request = ServletTestSupport.request().withPathInfo(path).build();
        ServletTestSupport.ResponseFixture response = ServletTestSupport.response();
        ServletTestSupport.runAsAuthenticatedUser(request, response, admin(), () -> servlet.doDelete(request, response.build()));
    }

    private AuthenticatedUser admin() {
        return new AuthenticatedUser(1L, "admin", "admin@example.com", UserRole.ADMIN);
    }
}
