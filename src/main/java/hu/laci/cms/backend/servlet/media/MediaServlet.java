package hu.laci.cms.backend.servlet.media;

import hu.laci.cms.backend.config.media.MediaStorageConfig;
import hu.laci.cms.backend.config.media.MediaStorageServiceFactory;
import hu.laci.cms.backend.config.session.AppSessionManager;
import hu.laci.cms.backend.dao.common.DaoRegistry;
import hu.laci.cms.backend.dao.media.MediaDao;
import hu.laci.cms.backend.dto.auth.AuthenticatedUser;
import hu.laci.cms.backend.model.media.Media;
import hu.laci.cms.backend.model.user.UserRole;
import hu.laci.cms.backend.service.media.MediaService;
import hu.laci.cms.backend.service.media.MediaServiceException;
import hu.laci.cms.backend.service.media.MediaStorageService;
import hu.laci.cms.backend.service.media.MediaContent;
import hu.laci.cms.backend.servlet.support.JsonServletSupport;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

@MultipartConfig
@WebServlet(urlPatterns = {"/api/media", "/api/media/*"})
public class MediaServlet extends JsonServletSupport {

    private MediaService mediaService;

    @Override
    public void init() throws ServletException {
        MediaDao mediaDao = DaoRegistry.getDao(Media.class);
        MediaStorageConfig storageConfig = MediaStorageConfig.getCurrent();
        MediaStorageService mediaStorageService = MediaStorageServiceFactory.create(storageConfig);
        this.mediaService = new MediaService(mediaDao, mediaStorageService, storageConfig.getStorageType());
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!requireAdmin(request, response)) {
            return;
        }

        try {
            ContentPath contentPath = parseContentPath(request);
            if (contentPath != null) {
                writeMediaContent(response, mediaService.getMediaContent(contentPath.id()));
                return;
            }

            Long id = parseOptionalId(request);
            if (id != null) {
                writeJsonResponse(response, HttpServletResponse.SC_OK, mediaService.getMedia(id));
                return;
            }

            boolean activeOnly = !"false".equalsIgnoreCase(request.getParameter("activeOnly"));
            writeJsonResponse(response, HttpServletResponse.SC_OK, mediaService.listMedia(activeOnly));
        } catch (BadRequestException e) {
            writeErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "INVALID_REQUEST", e.getMessage());
        } catch (MediaServiceException e) {
            writeServiceError(response, e);
        }
    }

    private void writeMediaContent(HttpServletResponse response, MediaContent mediaContent) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(mediaContent.getMimeType());
        response.setContentLengthLong(mediaContent.getFileSize());
        response.setHeader("Content-Disposition",
                "inline; filename=\"" + sanitizeHeaderValue(mediaContent.getOriginalFileName()) + "\"");
        response.getOutputStream().write(mediaContent.getContent());
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        if (!requireAdmin(request, response)) {
            return;
        }

        try {
            requireCollectionPath(request);
            Part filePart = request.getPart("file");
            if (filePart == null) {
                throw new BadRequestException("Multipart field 'file' is required.");
            }

            String originalFileName = filePart.getSubmittedFileName();
            String mimeType = filePart.getContentType();
            String description = request.getParameter("description");
            try (InputStream inputStream = filePart.getInputStream()) {
                writeJsonResponse(response, HttpServletResponse.SC_CREATED,
                        mediaService.uploadMedia(originalFileName, inputStream, mimeType, description));
            }
        } catch (BadRequestException e) {
            writeErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "INVALID_REQUEST", e.getMessage());
        } catch (MediaServiceException e) {
            writeServiceError(response, e);
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!requireAdmin(request, response)) {
            return;
        }

        try {
            Long id = parseRequiredId(request);
            writeJsonResponse(response, HttpServletResponse.SC_OK, mediaService.deleteMedia(id));
        } catch (BadRequestException e) {
            writeErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "INVALID_REQUEST", e.getMessage());
        } catch (MediaServiceException e) {
            writeServiceError(response, e);
        }
    }

    private boolean requireAdmin(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Optional<AuthenticatedUser> authenticatedUser = AppSessionManager.getAuthenticatedUser(request, response);
        if (authenticatedUser.isEmpty()) {
            writeErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "AUTH_REQUIRED", "Authentication required");
            return false;
        }

        if (authenticatedUser.get().getRole() != UserRole.ADMIN) {
            writeErrorResponse(response, HttpServletResponse.SC_FORBIDDEN,
                    "FORBIDDEN", "Administrator role is required.");
            return false;
        }

        return true;
    }

    private void requireCollectionPath(HttpServletRequest request) {
        if (parsePathInfo(request) != null) {
            throw new BadRequestException("Endpoint does not accept a media id.");
        }
    }

    private Long parseOptionalId(HttpServletRequest request) {
        String pathInfo = parsePathInfo(request);
        return pathInfo == null ? null : parseId(pathInfo);
    }

    private Long parseRequiredId(HttpServletRequest request) {
        String pathInfo = parsePathInfo(request);
        if (pathInfo == null) {
            throw new BadRequestException("Media id is required.");
        }
        return parseId(pathInfo);
    }

    private ContentPath parseContentPath(HttpServletRequest request) {
        String pathInfo = parsePathInfo(request);
        if (pathInfo == null || !pathInfo.endsWith("/content")) {
            return null;
        }

        String idPart = pathInfo.substring(0, pathInfo.length() - "/content".length());
        if (idPart.isBlank() || idPart.contains("/")) {
            throw new BadRequestException("Invalid media content path.");
        }
        return new ContentPath(parseId(idPart));
    }

    private String parsePathInfo(HttpServletRequest request) {
        String pathInfo = request.getPathInfo();
        if (pathInfo == null || pathInfo.isBlank() || "/".equals(pathInfo)) {
            return null;
        }
        if (!pathInfo.startsWith("/")) {
            throw new BadRequestException("Invalid media path.");
        }
        return pathInfo.substring(1);
    }

    private Long parseId(String value) {
        if (value.contains("/")) {
            throw new BadRequestException("Invalid media path.");
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new BadRequestException("Media id must be a number.", e);
        }
    }

    private String sanitizeHeaderValue(String value) {
        if (value == null || value.isBlank()) {
            return "media";
        }
        return value.replace("\\", "_")
                .replace("\"", "_")
                .replace("\r", "_")
                .replace("\n", "_");
    }

    private void writeServiceError(HttpServletResponse response, MediaServiceException e) throws IOException {
        int status = switch (e.getCode()) {
            case MediaService.VALIDATION_ERROR -> HttpServletResponse.SC_BAD_REQUEST;
            case MediaService.MEDIA_NOT_FOUND -> HttpServletResponse.SC_NOT_FOUND;
            case MediaService.STORAGE_ERROR -> HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
            default -> HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
        };
        if (e.getValidationErrors().isEmpty()) {
            writeErrorResponse(response, status, e.getCode(), e.getMessage());
            return;
        }
        writeErrorResponse(response, status, e.getCode(), e.getMessage(), e.getValidationErrors());
    }

    private static final class BadRequestException extends RuntimeException {

        private BadRequestException(String message) {
            super(message);
        }

        private BadRequestException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private record ContentPath(Long id) {
    }
}
