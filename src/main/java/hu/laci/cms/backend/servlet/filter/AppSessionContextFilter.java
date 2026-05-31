package hu.laci.cms.backend.servlet.filter;

import hu.laci.cms.backend.config.session.AppSessionManager;
import hu.laci.cms.backend.config.session.SessionContext;
import hu.laci.cms.backend.dto.auth.AuthenticatedUser;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

public class AppSessionContextFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            AppSessionManager.getAuthenticatedUser(httpRequest, (javax.servlet.http.HttpServletResponse) response)
                    .map(AuthenticatedUser::getId)
                    .ifPresent(SessionContext::setCurrentUserId);

            chain.doFilter(request, response);
        } finally {
            SessionContext.clear();
        }
    }
}
