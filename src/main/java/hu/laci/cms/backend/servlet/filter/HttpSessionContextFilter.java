package hu.laci.cms.backend.servlet.filter;

import hu.laci.cms.backend.config.session.SessionContext;
import hu.laci.cms.backend.dto.auth.AuthenticatedUser;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.IOException;

public class HttpSessionContextFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            HttpSession session = httpRequest.getSession(false);
            if (session != null && session.getAttribute("user") instanceof AuthenticatedUser authenticatedUser) {
                SessionContext.setCurrentUserId(authenticatedUser.getId());
            }

            chain.doFilter(request, response);
        } finally {
            SessionContext.clear();
        }
    }
}
