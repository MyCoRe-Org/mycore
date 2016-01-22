/**
 * 
 */
package org.mycore.frontend.filter;

import java.io.IOException;
import java.util.Optional;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.logging.log4j.LogManager;

/**
 * @author Thomas Scheffler
 *
 */
public class MCRRequestAuthenticationFilter implements Filter {

    public static final String SESSION_KEY = "mcr.authenticateRequest";

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        Optional<HttpSession> httpSession = Optional.ofNullable(req.getSession(false));
        if (httpSession.map(s -> s.getAttribute(SESSION_KEY)).isPresent()) {
            LogManager.getLogger().info("authenticated request for: " + req.getRemoteUser());
            req.authenticate((HttpServletResponse) response);
        }
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
    }

}
