package hu.laci.cms.backend.servlet.support;

import hu.laci.cms.backend.config.session.AppSessionManager;
import hu.laci.cms.backend.dto.auth.AuthenticatedUser;
import org.mockito.Mockito;
import org.mockito.MockedStatic;

import javax.servlet.FilterChain;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Factory methods and fixtures for unit-testing servlet endpoints and filters.
 * <p>
 * The fixtures use Mockito to model only the request and response behavior a
 * test needs. {@link ResponseFixture} captures JSON and binary output, status,
 * headers, and content metadata without requiring a running Tomcat instance.
 */
public final class ServletTestSupport {

    private ServletTestSupport() {
    }

    /**
     * Creates a configurable mocked HTTP request.
     *
     * @return fixture wrapping the request mock
     */
    public static RequestFixture request() {
        return new RequestFixture();
    }

    /**
     * Creates a response fixture that captures servlet output.
     *
     * @return fixture wrapping the response mock
     * @throws IOException when the response writer cannot be created
     */
    public static ResponseFixture response() throws IOException {
        return new ResponseFixture();
    }

    /**
     * Creates a mocked HTTP session for servlet tests.
     *
     * @return mocked HTTP session
     */
    public static HttpSession session() {
        return Mockito.mock(HttpSession.class);
    }

    /**
     * Creates a mocked filter chain for filter tests.
     *
     * @return mocked filter chain, verifiable with Mockito
     */
    public static FilterChain filterChain() {
        return Mockito.mock(FilterChain.class);
    }

    /**
     * Creates a multipart file part backed by the supplied bytes.
     *
     * @param fileName submitted file name
     * @param contentType MIME type reported by the part
     * @param content uploaded file bytes
     * @return mocked multipart part
     * @throws IOException when the part input stream cannot be configured
     */
    public static Part filePart(String fileName, String contentType, byte[] content) throws IOException {
        Part part = Mockito.mock(Part.class);
        Mockito.when(part.getSubmittedFileName()).thenReturn(fileName);
        Mockito.when(part.getContentType()).thenReturn(contentType);
        Mockito.when(part.getInputStream()).thenAnswer(invocation -> new ByteArrayInputStream(content));
        return part;
    }

    /**
     * Injects a test double into a private servlet dependency field.
     *
     * @param target object containing the field
     * @param fieldName declared field name
     * @param value replacement value
     * @throws ReflectiveOperationException when the field cannot be updated
     */
    public static void setField(Object target, String fieldName, Object value) throws ReflectiveOperationException {
        java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    /**
     * Executes servlet handling with a mocked authenticated application user.
     *
     * @param request request mock used by the servlet
     * @param response response fixture used by the servlet
     * @param user authenticated user to expose through the session manager
     * @param action servlet invocation
     * @throws Exception when the servlet invocation fails
     */
    public static void runAsAuthenticatedUser(HttpServletRequest request, ResponseFixture response,
                                              AuthenticatedUser user, ThrowingAction action) throws Exception {
        try (MockedStatic<AppSessionManager> sessions = Mockito.mockStatic(AppSessionManager.class)) {
            sessions.when(() -> AppSessionManager.getAuthenticatedUser(request, response.build()))
                    .thenReturn(java.util.Optional.of(user));
            action.run();
        }
    }

    /**
     * Action executed while a servlet test fixture is active.
     */
    @FunctionalInterface
    public interface ThrowingAction {

        /**
         * Executes the action.
         *
         * @throws Exception when the action fails
         */
        void run() throws Exception;
    }

    /**
     * Configurable mocked request fixture.
     */
    public static final class RequestFixture {

        private final HttpServletRequest request;

        private RequestFixture() {
            this.request = Mockito.mock(HttpServletRequest.class);
        }

        /**
         * Configures a JSON request body.
         *
         * @param json request body text
         * @return this fixture
         * @throws IOException when the request reader cannot be configured
         */
        public RequestFixture withJsonBody(String json) throws IOException {
            Mockito.when(request.getReader()).thenAnswer(invocation -> new BufferedReader(new StringReader(json)));
            Mockito.when(request.getContentType()).thenReturn("application/json");
            return this;
        }

        /**
         * Configures an HTTP method.
         *
         * @param method HTTP method
         * @return this fixture
         */
        public RequestFixture withMethod(String method) {
            Mockito.when(request.getMethod()).thenReturn(method);
            return this;
        }

        /**
         * Configures a request header.
         *
         * @param name header name
         * @param value header value
         * @return this fixture
         */
        public RequestFixture withHeader(String name, String value) {
            Mockito.when(request.getHeader(name)).thenReturn(value);
            return this;
        }

        /**
         * Configures a query or form parameter.
         *
         * @param name parameter name
         * @param value parameter value
         * @return this fixture
         */
        public RequestFixture withParameter(String name, String value) {
            Mockito.when(request.getParameter(name)).thenReturn(value);
            return this;
        }

        /**
         * Configures the servlet-relative path.
         *
         * @param servletPath servlet path
         * @return this fixture
         */
        public RequestFixture withServletPath(String servletPath) {
            Mockito.when(request.getServletPath()).thenReturn(servletPath);
            return this;
        }

        /**
         * Configures the path portion after the servlet mapping.
         *
         * @param pathInfo path info
         * @return this fixture
         */
        public RequestFixture withPathInfo(String pathInfo) {
            Mockito.when(request.getPathInfo()).thenReturn(pathInfo);
            return this;
        }

        /**
         * Configures the remote address used by rate-limit and audit tests.
         *
         * @param remoteAddress client address
         * @return this fixture
         */
        public RequestFixture withRemoteAddress(String remoteAddress) {
            Mockito.when(request.getRemoteAddr()).thenReturn(remoteAddress);
            return this;
        }

        /**
         * Configures the current HTTP session.
         *
         * @param session current session, or {@code null} when none exists
         * @return this fixture
         */
        public RequestFixture withSession(HttpSession session) {
            Mockito.when(request.getSession()).thenReturn(session);
            Mockito.when(request.getSession(false)).thenReturn(session);
            return this;
        }

        /**
         * Configures a named multipart request part.
         *
         * @param name part name
         * @param part multipart part
         * @return this fixture
         * @throws IOException when the part cannot be configured
         * @throws javax.servlet.ServletException when the part cannot be configured
         */
        public RequestFixture withPart(String name, Part part) throws IOException, javax.servlet.ServletException {
            Mockito.when(request.getPart(name)).thenReturn(part);
            return this;
        }

        /**
         * Returns the configured request mock.
         *
         * @return mocked request
         */
        public HttpServletRequest build() {
            return request;
        }
    }

    /**
     * Captures the observable state written to an HTTP response.
     */
    public static final class ResponseFixture {

        private final HttpServletResponse response;
        private final StringWriter bodyWriter;
        private final CapturingServletOutputStream outputStream;
        private final Map<String, String> headers;
        private int status;
        private String contentType;
        private String characterEncoding;
        private long contentLength;

        private ResponseFixture() throws IOException {
            this.response = Mockito.mock(HttpServletResponse.class);
            this.bodyWriter = new StringWriter();
            this.outputStream = new CapturingServletOutputStream();
            this.headers = new LinkedHashMap<>();
            this.status = HttpServletResponse.SC_OK;
            this.contentLength = -1L;

            Mockito.when(response.getWriter()).thenReturn(new PrintWriter(bodyWriter));
            Mockito.when(response.getOutputStream()).thenReturn(outputStream);
            Mockito.doAnswer(invocation -> {
                status = invocation.getArgument(0, Integer.class);
                return null;
            }).when(response).setStatus(Mockito.anyInt());
            Mockito.doAnswer(invocation -> {
                contentType = invocation.getArgument(0, String.class);
                return null;
            }).when(response).setContentType(Mockito.anyString());
            Mockito.doAnswer(invocation -> {
                characterEncoding = invocation.getArgument(0, String.class);
                return null;
            }).when(response).setCharacterEncoding(Mockito.anyString());
            Mockito.doAnswer(invocation -> {
                headers.put(invocation.getArgument(0, String.class), invocation.getArgument(1, String.class));
                return null;
            }).when(response).setHeader(Mockito.anyString(), Mockito.anyString());
            Mockito.doAnswer(invocation -> {
                contentLength = invocation.getArgument(0, Long.class);
                return null;
            }).when(response).setContentLengthLong(Mockito.anyLong());
        }

        /**
         * Returns the mocked response passed to the unit under test.
         *
         * @return mocked response
         */
        public HttpServletResponse build() {
            return response;
        }

        /**
         * Returns the final HTTP status.
         *
         * @return response status, defaulting to 200
         */
        public int getStatus() {
            return status;
        }

        /**
         * Returns the captured text response body.
         *
         * @return text body
         */
        public String getBody() {
            return bodyWriter.toString();
        }

        /**
         * Returns the captured binary response body.
         *
         * @return binary body bytes
         */
        public byte[] getBinaryBody() {
            return outputStream.toByteArray();
        }

        /**
         * Returns a captured response header value.
         *
         * @param name header name
         * @return header value, or {@code null} when absent
         */
        public String getHeader(String name) {
            return headers.get(name);
        }

        /**
         * Returns the content type written by the servlet.
         *
         * @return content type, or {@code null} when absent
         */
        public String getContentType() {
            return contentType;
        }

        /**
         * Returns the character encoding written by the servlet.
         *
         * @return response character encoding, or {@code null} when absent
         */
        public String getCharacterEncoding() {
            return characterEncoding;
        }

        /**
         * Returns the content length written by the servlet.
         *
         * @return content length, or {@code -1} when absent
         */
        public long getContentLength() {
            return contentLength;
        }
    }

    /**
     * In-memory servlet output stream used by {@link ResponseFixture}.
     */
    private static final class CapturingServletOutputStream extends ServletOutputStream {

        private final java.io.ByteArrayOutputStream delegate = new java.io.ByteArrayOutputStream();

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setWriteListener(WriteListener writeListener) {
        }

        @Override
        public void write(int value) {
            delegate.write(value);
        }

        private byte[] toByteArray() {
            return delegate.toByteArray();
        }
    }
}
