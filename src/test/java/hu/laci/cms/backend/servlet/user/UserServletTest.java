package hu.laci.cms.backend.servlet.user;

import hu.laci.cms.backend.dto.auth.AuthenticatedUser;
import hu.laci.cms.backend.model.user.UserRole;
import hu.laci.cms.backend.service.user.UserService;
import hu.laci.cms.backend.service.user.UserServiceException;
import hu.laci.cms.backend.servlet.support.ServletTestSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/** Unit tests for user CRUD and approval endpoint dispatch. */
class UserServletTest {

    @Test
    void dispatchesCollectionDetailAndApprovalOperations() throws Exception {
        UserService service = Mockito.mock(UserService.class);
        UserServlet servlet = new UserServlet();
        ServletTestSupport.setField(servlet, "userService", service);

        javax.servlet.http.HttpServletRequest listRequest = ServletTestSupport.request().build();
        ServletTestSupport.ResponseFixture listResponse = ServletTestSupport.response();
        ServletTestSupport.runAsAuthenticatedUser(listRequest, listResponse, admin(),
                () -> servlet.doGet(listRequest, listResponse.build()));

        ServletTestSupport.ResponseFixture detailResponse = ServletTestSupport.response();
        javax.servlet.http.HttpServletRequest detailRequest = ServletTestSupport.request().withPathInfo("/7").build();
        ServletTestSupport.runAsAuthenticatedUser(detailRequest, detailResponse, admin(),
                () -> servlet.doGet(detailRequest, detailResponse.build()));

        ServletTestSupport.ResponseFixture approvalResponse = ServletTestSupport.response();
        javax.servlet.http.HttpServletRequest approvalRequest = ServletTestSupport.request().withPathInfo("/7/approve").build();
        ServletTestSupport.runAsAuthenticatedUser(approvalRequest, approvalResponse, admin(),
                () -> servlet.doPost(approvalRequest, approvalResponse.build()));

        Mockito.verify(service).findAll();
        Mockito.verify(service).findById(7L);
        Mockito.verify(service).approve(7L);
        Assertions.assertEquals(200, approvalResponse.getStatus());
    }

    @Test
    void dispatchesCreateUpdateAndDeactivateOperations() throws Exception {
        UserService service = Mockito.mock(UserService.class);
        UserServlet servlet = new UserServlet();
        ServletTestSupport.setField(servlet, "userService", service);

        javax.servlet.http.HttpServletRequest createRequest = ServletTestSupport.request().withJsonBody("{}").build();
        ServletTestSupport.ResponseFixture createResponse = ServletTestSupport.response();
        ServletTestSupport.runAsAuthenticatedUser(createRequest, createResponse, admin(),
                () -> servlet.doPost(createRequest, createResponse.build()));

        javax.servlet.http.HttpServletRequest updateRequest = ServletTestSupport.request()
                .withPathInfo("/7").withJsonBody("{}").build();
        ServletTestSupport.ResponseFixture updateResponse = ServletTestSupport.response();
        ServletTestSupport.runAsAuthenticatedUser(updateRequest, updateResponse, admin(),
                () -> servlet.doPut(updateRequest, updateResponse.build()));

        javax.servlet.http.HttpServletRequest deleteRequest = ServletTestSupport.request().withPathInfo("/7").build();
        ServletTestSupport.ResponseFixture deleteResponse = ServletTestSupport.response();
        ServletTestSupport.runAsAuthenticatedUser(deleteRequest, deleteResponse, admin(),
                () -> servlet.doDelete(deleteRequest, deleteResponse.build()));

        Mockito.verify(service).create(Mockito.any());
        Mockito.verify(service).update(Mockito.eq(7L), Mockito.any());
        Mockito.verify(service).deactivate(7L);
        Assertions.assertEquals(201, createResponse.getStatus());
    }

    @Test
    void rejectsInvalidUserIdBeforeCallingService() throws Exception {
        UserService service = Mockito.mock(UserService.class);
        UserServlet servlet = new UserServlet();
        ServletTestSupport.setField(servlet, "userService", service);
        javax.servlet.http.HttpServletRequest request = ServletTestSupport.request().withPathInfo("/nope").build();
        ServletTestSupport.ResponseFixture response = ServletTestSupport.response();

        ServletTestSupport.runAsAuthenticatedUser(request, response, admin(), () -> servlet.doGet(request, response.build()));

        Assertions.assertEquals(400, response.getStatus());
        Assertions.assertTrue(response.getBody().contains("INVALID_REQUEST"));
        Mockito.verifyNoInteractions(service);
    }

    @Test
    void mapsMissingUserToNotFoundResponse() throws Exception {
        UserService service = Mockito.mock(UserService.class);
        Mockito.when(service.findById(7L)).thenThrow(new UserServiceException(UserService.USER_NOT_FOUND, "Missing"));
        UserServlet servlet = new UserServlet();
        ServletTestSupport.setField(servlet, "userService", service);
        javax.servlet.http.HttpServletRequest request = ServletTestSupport.request().withPathInfo("/7").build();
        ServletTestSupport.ResponseFixture response = ServletTestSupport.response();

        ServletTestSupport.runAsAuthenticatedUser(request, response, admin(), () -> servlet.doGet(request, response.build()));

        Assertions.assertEquals(404, response.getStatus());
        Assertions.assertTrue(response.getBody().contains(UserService.USER_NOT_FOUND));
    }

    private AuthenticatedUser admin() {
        return new AuthenticatedUser(1L, "admin", "admin@example.com", UserRole.ADMIN);
    }
}
