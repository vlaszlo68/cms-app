package hu.laci.cms.backend.servlet.support;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import hu.laci.cms.backend.dto.common.ApiErrorResponse;
import hu.laci.cms.backend.dto.common.ApiResponse;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

/**
 * Base servlet with common JSON request/response helpers.
 * <p>
 * Servlet subclasses use this class to keep API responses in the common
 * {@link ApiResponse} envelope and to centralize Gson usage.
 */
public abstract class JsonServletSupport extends HttpServlet {

    /**
     * Shared JSON serializer/deserializer for servlet subclasses.
     */
    protected final Gson gson = new Gson();

    /**
     * Reads a JSON request body into the target type.
     *
     * @param reader     request body reader
     * @param targetType target DTO class
     * @param <T>        target DTO type
     * @return parsed DTO
     */
    protected <T> T readJsonBody(InputStreamReader reader, Class<T> targetType) {
        return gson.fromJson(reader, targetType);
    }

    /**
     * Writes a successful JSON response wrapped in {@link ApiResponse}.
     *
     * @param response HTTP response
     * @param status   HTTP status code
     * @param payload  response payload
     * @throws IOException when writing fails
     */
    protected void writeJsonResponse(HttpServletResponse response, int status, Object payload)
            throws IOException {
        writeJson(response, status, ApiResponse.success(payload));
    }

    /**
     * Writes a successful JSON response while retaining null-valued payload fields.
     *
     * @param response HTTP response
     * @param status HTTP status code
     * @param payload response payload
     * @throws IOException when writing fails
     */
    protected void writeJsonResponseIncludingNulls(HttpServletResponse response, int status, Object payload)
            throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setStatus(status);
        Gson nullSerializingGson = new GsonBuilder().serializeNulls().create();
        response.getWriter().write(nullSerializingGson.toJson(ApiResponse.success(payload)));
    }

    /**
     * Writes an error JSON response wrapped in {@link ApiResponse}.
     *
     * @param response HTTP response
     * @param status   HTTP status code
     * @param code     stable API error code
     * @param message  human-readable error message
     * @throws IOException when writing fails
     */
    protected void writeErrorResponse(HttpServletResponse response, int status, String code, String message)
            throws IOException {
        writeJson(response, status, ApiResponse.error(new ApiErrorResponse(code, message)));
    }

    /**
     * Writes an error JSON response with structured validation error codes.
     *
     * @param response         HTTP response
     * @param status           HTTP status code
     * @param code             stable API error code
     * @param message          human-readable error message
     * @param validationErrors stable validation error codes
     * @throws IOException when writing fails
     */
    protected void writeErrorResponse(HttpServletResponse response, int status, String code, String message,
                                      List<String> validationErrors) throws IOException {
        writeJson(response, status, ApiResponse.error(new ApiErrorResponse(code, message, validationErrors)));
    }

    private void writeJson(HttpServletResponse response, int status, Object payload)
            throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setStatus(status);
        response.getWriter().write(gson.toJson(payload));
    }

    @Override
    public void init() throws ServletException {
        super.init();
        System.out.println(">>> Servlet initialized: " + getClass().getName());
    }
}
