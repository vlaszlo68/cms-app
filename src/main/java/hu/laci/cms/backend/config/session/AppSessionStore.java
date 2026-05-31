package hu.laci.cms.backend.config.session;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Optional;

/**
 * Store-neutral persistence boundary for application sessions.
 */
public interface AppSessionStore {

    /**
     * Finds the current request's session.
     *
     * @param request HTTP request
     * @param response HTTP response, available for cookie cleanup if needed
     * @return current session when present and valid
     */
    Optional<AppSession> find(HttpServletRequest request, HttpServletResponse response);

    /**
     * Creates a new anonymous application session.
     *
     * @param request HTTP request
     * @param response HTTP response for cookie creation
     * @return created session
     */
    AppSession create(HttpServletRequest request, HttpServletResponse response);

    /**
     * Persists a session after mutation.
     *
     * @param request HTTP request
     * @param response HTTP response
     * @param session session to persist
     */
    void save(HttpServletRequest request, HttpServletResponse response, AppSession session);

    /**
     * Invalidates the current request's session.
     *
     * @param request HTTP request
     * @param response HTTP response for cookie clearing
     */
    void invalidate(HttpServletRequest request, HttpServletResponse response);
}
