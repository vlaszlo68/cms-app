package hu.laci.cms.backend.servlet.filter;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Forces UTF-8 character encoding on request and response objects.
 * <p>
 * This keeps JSON body parsing and response writing consistent across servlets
 * and filters.
 */
public class CharacterEncodingFilter implements Filter {

    /**
     * Sets UTF-8 on request and response before continuing the filter chain.
     *
     * @param request servlet request
     * @param response servlet response
     * @param chain downstream filter chain
     * @throws IOException when downstream processing fails
     * @throws ServletException when downstream processing fails
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        request.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        chain.doFilter(request, response);
    }
}
