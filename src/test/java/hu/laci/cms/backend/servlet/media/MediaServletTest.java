package hu.laci.cms.backend.servlet.media;

import hu.laci.cms.backend.dto.auth.AuthenticatedUser;
import hu.laci.cms.backend.model.user.UserRole;
import hu.laci.cms.backend.service.media.MediaContent;
import hu.laci.cms.backend.service.media.MediaService;
import hu.laci.cms.backend.servlet.support.ServletTestSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/** Unit tests for metadata, content, upload, and deletion media endpoints. */
class MediaServletTest {

    @Test
    void getDispatchesListDetailAndBinaryContent() throws Exception {
        MediaService service = Mockito.mock(MediaService.class);
        Mockito.when(service.getMediaContent(4L)).thenReturn(new MediaContent("photo.png", "image/png", 3L,
                new byte[]{1, 2, 3}));
        MediaServlet servlet = servlet(service);

        javax.servlet.http.HttpServletRequest listRequest = ServletTestSupport.request().withParameter("activeOnly", "false").build();
        ServletTestSupport.ResponseFixture listResponse = ServletTestSupport.response();
        ServletTestSupport.runAsAuthenticatedUser(listRequest, listResponse, admin(), () -> servlet.doGet(listRequest, listResponse.build()));

        javax.servlet.http.HttpServletRequest detailRequest = ServletTestSupport.request().withPathInfo("/4").build();
        ServletTestSupport.ResponseFixture detailResponse = ServletTestSupport.response();
        ServletTestSupport.runAsAuthenticatedUser(detailRequest, detailResponse, admin(), () -> servlet.doGet(detailRequest, detailResponse.build()));

        javax.servlet.http.HttpServletRequest contentRequest = ServletTestSupport.request().withPathInfo("/4/content").build();
        ServletTestSupport.ResponseFixture contentResponse = ServletTestSupport.response();
        ServletTestSupport.runAsAuthenticatedUser(contentRequest, contentResponse, admin(), () -> servlet.doGet(contentRequest, contentResponse.build()));

        Mockito.verify(service).listMedia(false);
        Mockito.verify(service).getMedia(4L);
        Assertions.assertEquals("image/png", contentResponse.getContentType());
        Assertions.assertEquals(3L, contentResponse.getContentLength());
    }

    @Test
    void postUploadsPartAndDeleteUsesId() throws Exception {
        MediaService service = Mockito.mock(MediaService.class);
        MediaServlet servlet = servlet(service);
        javax.servlet.http.Part part = ServletTestSupport.filePart("photo.png", "image/png", new byte[]{1});
        javax.servlet.http.HttpServletRequest postRequest = ServletTestSupport.request()
                .withPart("file", part).withParameter("description", "Photo").build();
        ServletTestSupport.ResponseFixture postResponse = ServletTestSupport.response();
        ServletTestSupport.runAsAuthenticatedUser(postRequest, postResponse, admin(), () -> servlet.doPost(postRequest, postResponse.build()));

        javax.servlet.http.HttpServletRequest deleteRequest = ServletTestSupport.request().withPathInfo("/4").build();
        ServletTestSupport.ResponseFixture deleteResponse = ServletTestSupport.response();
        ServletTestSupport.runAsAuthenticatedUser(deleteRequest, deleteResponse, admin(), () -> servlet.doDelete(deleteRequest, deleteResponse.build()));

        Mockito.verify(service).uploadMedia(Mockito.eq("photo.png"), Mockito.any(), Mockito.eq("image/png"), Mockito.eq("Photo"));
        Mockito.verify(service).deleteMedia(4L);
        Assertions.assertEquals(201, postResponse.getStatus());
    }

    @Test
    void postRejectsMissingMultipartFile() throws Exception {
        MediaServlet servlet = servlet(Mockito.mock(MediaService.class));
        javax.servlet.http.HttpServletRequest request = ServletTestSupport.request().build();
        ServletTestSupport.ResponseFixture response = ServletTestSupport.response();
        ServletTestSupport.runAsAuthenticatedUser(request, response, admin(), () -> servlet.doPost(request, response.build()));
        Assertions.assertEquals(400, response.getStatus());
        Assertions.assertTrue(response.getBody().contains("INVALID_REQUEST"));
    }

    @Test
    void getRejectsMalformedMediaPathBeforeServiceLookup() throws Exception {
        MediaService service = Mockito.mock(MediaService.class);
        MediaServlet servlet = servlet(service);
        javax.servlet.http.HttpServletRequest request = ServletTestSupport.request().withPathInfo("/4/other").build();
        ServletTestSupport.ResponseFixture response = ServletTestSupport.response();

        ServletTestSupport.runAsAuthenticatedUser(request, response, admin(), () -> servlet.doGet(request, response.build()));

        Assertions.assertEquals(400, response.getStatus());
        Assertions.assertTrue(response.getBody().contains("INVALID_REQUEST"));
        Mockito.verifyNoInteractions(service);
    }

    private MediaServlet servlet(MediaService service) throws ReflectiveOperationException {
        MediaServlet servlet = new MediaServlet();
        ServletTestSupport.setField(servlet, "mediaService", service);
        return servlet;
    }

    private AuthenticatedUser admin() {
        return new AuthenticatedUser(1L, "admin", "admin@example.com", UserRole.ADMIN);
    }
}
