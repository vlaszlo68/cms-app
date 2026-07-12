package hu.laci.cms.backend.servlet.template;

import hu.laci.cms.backend.dto.auth.AuthenticatedUser;
import hu.laci.cms.backend.model.user.UserRole;
import hu.laci.cms.backend.service.template.TemplateService;
import hu.laci.cms.backend.service.template.TemplateServiceException;
import hu.laci.cms.backend.servlet.support.ServletTestSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/** Unit tests for template list, detail, create, update, and deactivate endpoints. */
class TemplateServletTest {

    @Test
    void dispatchesAllTemplateOperations() throws Exception {
        TemplateService service = Mockito.mock(TemplateService.class);
        TemplateServlet servlet = new TemplateServlet();
        ServletTestSupport.setField(servlet, "templateService", service);
        get(servlet, null);
        get(servlet, "/6");
        post(servlet);
        put(servlet, "/6");
        delete(servlet, "/6");

        Mockito.verify(service).listTemplates();
        Mockito.verify(service).getTemplate(6L);
        Mockito.verify(service).createTemplate(Mockito.any());
        Mockito.verify(service).updateTemplate(Mockito.eq(6L), Mockito.any());
        Mockito.verify(service).deactivateTemplate(6L);
    }

    @Test
    void rejectsNestedTemplatePath() throws Exception {
        TemplateServlet servlet = new TemplateServlet();
        ServletTestSupport.setField(servlet, "templateService", Mockito.mock(TemplateService.class));
        javax.servlet.http.HttpServletRequest request = ServletTestSupport.request().withPathInfo("/6/extra").build();
        ServletTestSupport.ResponseFixture response = ServletTestSupport.response();
        ServletTestSupport.runAsAuthenticatedUser(request, response, admin(), () -> servlet.doGet(request, response.build()));
        Assertions.assertEquals(400, response.getStatus());
    }

    @Test
    void mapsDuplicateTemplateCodeToConflictResponse() throws Exception {
        TemplateService service = Mockito.mock(TemplateService.class);
        Mockito.when(service.createTemplate(Mockito.any())).thenThrow(new TemplateServiceException(
                TemplateService.DUPLICATE_CODE, "Duplicate"));
        TemplateServlet servlet = new TemplateServlet();
        ServletTestSupport.setField(servlet, "templateService", service);
        javax.servlet.http.HttpServletRequest request = ServletTestSupport.request().withJsonBody("{}").build();
        ServletTestSupport.ResponseFixture response = ServletTestSupport.response();

        ServletTestSupport.runAsAuthenticatedUser(request, response, admin(), () -> servlet.doPost(request, response.build()));

        Assertions.assertEquals(409, response.getStatus());
        Assertions.assertTrue(response.getBody().contains(TemplateService.DUPLICATE_CODE));
    }

    private void get(TemplateServlet servlet, String path) throws Exception {
        ServletTestSupport.RequestFixture fixture = ServletTestSupport.request();
        if (path != null) {
            fixture.withPathInfo(path);
        }
        javax.servlet.http.HttpServletRequest request = fixture.build();
        ServletTestSupport.ResponseFixture response = ServletTestSupport.response();
        ServletTestSupport.runAsAuthenticatedUser(request, response, admin(), () -> servlet.doGet(request, response.build()));
    }

    private void post(TemplateServlet servlet) throws Exception {
        javax.servlet.http.HttpServletRequest request = ServletTestSupport.request().withJsonBody("{}").build();
        ServletTestSupport.ResponseFixture response = ServletTestSupport.response();
        ServletTestSupport.runAsAuthenticatedUser(request, response, admin(), () -> servlet.doPost(request, response.build()));
        Assertions.assertEquals(201, response.getStatus());
    }

    private void put(TemplateServlet servlet, String path) throws Exception {
        javax.servlet.http.HttpServletRequest request = ServletTestSupport.request().withPathInfo(path).withJsonBody("{}").build();
        ServletTestSupport.ResponseFixture response = ServletTestSupport.response();
        ServletTestSupport.runAsAuthenticatedUser(request, response, admin(), () -> servlet.doPut(request, response.build()));
    }

    private void delete(TemplateServlet servlet, String path) throws Exception {
        javax.servlet.http.HttpServletRequest request = ServletTestSupport.request().withPathInfo(path).build();
        ServletTestSupport.ResponseFixture response = ServletTestSupport.response();
        ServletTestSupport.runAsAuthenticatedUser(request, response, admin(), () -> servlet.doDelete(request, response.build()));
    }

    private AuthenticatedUser admin() {
        return new AuthenticatedUser(1L, "admin", "admin@example.com", UserRole.ADMIN);
    }
}
