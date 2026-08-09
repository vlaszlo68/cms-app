package hu.laci.cms.backend.servlet.page;

import hu.laci.cms.backend.dao.common.DaoRegistry;
import hu.laci.cms.backend.dao.page.PageBlockDao;
import hu.laci.cms.backend.dao.page.PageDao;
import hu.laci.cms.backend.dao.template.TemplateDao;
import hu.laci.cms.backend.model.page.Page;
import hu.laci.cms.backend.model.page.PageBlock;
import hu.laci.cms.backend.model.template.Template;
import hu.laci.cms.backend.service.page.PageService;
import hu.laci.cms.backend.service.page.PageServiceException;
import hu.laci.cms.backend.servlet.support.JsonServletSupport;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Locale;

/**
 * Unauthenticated JSON endpoint for resolving published CONTENT and BLOCK pages by an exact public slug.
 */
@WebServlet(urlPatterns = "/api/public/pages/*")
public class PublicPageServlet extends JsonServletSupport {

    private PageService pageService;

    @Override
    public void init() throws ServletException {
        PageDao pageDao = DaoRegistry.getDao(Page.class);
        PageBlockDao pageBlockDao = DaoRegistry.getDao(PageBlock.class);
        TemplateDao templateDao = DaoRegistry.getDao(Template.class);
        pageService = new PageService(pageDao, templateDao, pageBlockDao);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            writeJsonResponse(response, HttpServletResponse.SC_OK,
                    pageService.getPublicPageBySlug(parseSlug(request)));
        } catch (BadRequestException e) {
            writeErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "INVALID_REQUEST", e.getMessage());
        } catch (PageServiceException e) {
            int status = PageService.PAGE_NOT_FOUND.equals(e.getCode())
                    ? HttpServletResponse.SC_NOT_FOUND
                    : HttpServletResponse.SC_BAD_REQUEST;
            writeErrorResponse(response, status, e.getCode(), e.getMessage());
        }
    }

    private String parseSlug(HttpServletRequest request) {
        String path = request.getPathInfo();
        if (path == null || path.isBlank() || "/".equals(path) || !path.startsWith("/")) {
            throw new BadRequestException("Page slug is required.");
        }

        String slug = path.substring(1);
        if (slug.isBlank() || slug.contains("/") || slug.toLowerCase(Locale.ROOT).contains("%2f")) {
            throw new BadRequestException("Invalid public page slug path.");
        }
        return slug;
    }

    private static final class BadRequestException extends RuntimeException {

        private BadRequestException(String message) {
            super(message);
        }
    }
}
