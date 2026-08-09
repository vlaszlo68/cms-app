package hu.laci.cms.backend.servlet.page;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import hu.laci.cms.backend.dto.page.PublicPageBlockResponse;
import hu.laci.cms.backend.dto.page.PublicPageResponse;
import hu.laci.cms.backend.model.page.PageType;
import hu.laci.cms.backend.service.page.PageService;
import hu.laci.cms.backend.service.page.PageServiceException;
import hu.laci.cms.backend.servlet.support.ServletTestSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Set;

/**
 * Unit tests for the unauthenticated public page HTTP contract.
 */
class PublicPageServletTest {

    @Test
    void getReturnsLimitedPublicPageEnvelopeForExactSlug() throws Exception {
        PageService pageService = Mockito.mock(PageService.class);
        Mockito.when(pageService.getPublicPageBySlug("About-Us"))
                .thenReturn(new PublicPageResponse(5L, "About", "About-Us", PageType.CONTENT,
                        "LANDING", "Public text"));
        PublicPageServlet servlet = servlet(pageService);
        HttpServletRequest request = ServletTestSupport.request().withPathInfo("/About-Us").build();
        ServletTestSupport.ResponseFixture response = ServletTestSupport.response();

        servlet.doGet(request, response.build());

        JsonObject data = JsonParser.parseString(response.getBody()).getAsJsonObject().getAsJsonObject("data");
        Assertions.assertEquals(200, response.getStatus());
        Assertions.assertTrue(JsonParser.parseString(response.getBody()).getAsJsonObject().get("success").getAsBoolean());
        Assertions.assertEquals(Set.of("id", "title", "slug", "pageType", "templateCode", "content"),
                data.keySet());
        Assertions.assertEquals("About-Us", data.get("slug").getAsString());
        Assertions.assertEquals("LANDING", data.get("templateCode").getAsString());
        Mockito.verify(pageService).getPublicPageBySlug("About-Us");
    }

    @Test
    void getMapsIneligibleOrUnknownPageToNotFoundEnvelope() throws Exception {
        PageService pageService = Mockito.mock(PageService.class);
        Mockito.when(pageService.getPublicPageBySlug("draft"))
                .thenThrow(new PageServiceException(PageService.PAGE_NOT_FOUND, "Page not found."));
        PublicPageServlet servlet = servlet(pageService);
        HttpServletRequest request = ServletTestSupport.request().withPathInfo("/draft").build();
        ServletTestSupport.ResponseFixture response = ServletTestSupport.response();

        servlet.doGet(request, response.build());

        Assertions.assertEquals(404, response.getStatus());
        Assertions.assertTrue(response.getBody().contains(PageService.PAGE_NOT_FOUND));
    }

    @Test
    void getReturnsLimitedBlockPageEnvelopeWithoutContentOrInternalBlockFields() throws Exception {
        PageService pageService = Mockito.mock(PageService.class);
        Mockito.when(pageService.getPublicPageBySlug("landing"))
                .thenReturn(new PublicPageResponse(6L, "Landing", "landing", PageType.BLOCK, "LANDING", null,
                        List.of(new PublicPageBlockResponse(7L, "HERO", "Welcome", 1, true,
                                "{\"headline\":\"Hello\"}"))));
        PublicPageServlet servlet = servlet(pageService);
        HttpServletRequest request = ServletTestSupport.request().withPathInfo("/landing").build();
        ServletTestSupport.ResponseFixture response = ServletTestSupport.response();

        servlet.doGet(request, response.build());

        JsonObject data = JsonParser.parseString(response.getBody()).getAsJsonObject().getAsJsonObject("data");
        JsonArray blocks = data.getAsJsonArray("blocks");
        JsonObject block = blocks.get(0).getAsJsonObject();
        Assertions.assertEquals(200, response.getStatus());
        Assertions.assertEquals(Set.of("id", "title", "slug", "pageType", "templateCode", "blocks"),
                data.keySet());
        Assertions.assertFalse(data.has("content"));
        Assertions.assertEquals(Set.of("id", "blockType", "title", "sortOrder", "visible", "configJson"),
                block.keySet());
        Assertions.assertFalse(block.has("pageId"));
        Assertions.assertTrue(block.get("visible").getAsBoolean());
        Assertions.assertEquals("HERO", block.get("blockType").getAsString());
        Assertions.assertEquals("{\"headline\":\"Hello\"}", block.get("configJson").getAsString());
    }

    @Test
    void getRejectsMissingOrMultiSegmentPublicPagePaths() throws Exception {
        assertMalformedPath(null);
        assertMalformedPath("/");
        assertMalformedPath("/about/extra");
        assertMalformedPath("about");
    }

    @Test
    void getRejectsEncodedPathDelimiter() throws Exception {
        assertMalformedPath("/about%2Fextra");
        assertMalformedPath("/about%2fextra");
    }

    private void assertMalformedPath(String pathInfo) throws Exception {
        PageService pageService = Mockito.mock(PageService.class);
        PublicPageServlet servlet = servlet(pageService);
        HttpServletRequest request = ServletTestSupport.request().withPathInfo(pathInfo).build();
        ServletTestSupport.ResponseFixture response = ServletTestSupport.response();

        servlet.doGet(request, response.build());

        Assertions.assertEquals(400, response.getStatus());
        Assertions.assertTrue(response.getBody().contains("INVALID_REQUEST"));
        Mockito.verifyNoInteractions(pageService);
    }

    private PublicPageServlet servlet(PageService pageService) throws ReflectiveOperationException {
        PublicPageServlet servlet = new PublicPageServlet();
        ServletTestSupport.setField(servlet, "pageService", pageService);
        return servlet;
    }
}
