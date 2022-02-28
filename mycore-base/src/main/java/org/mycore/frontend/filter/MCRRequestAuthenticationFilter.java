/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mycore.frontend.filter;

import java.io.IOException;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

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
        if (httpSession.map(s -> s.getAttribute(SESSION_KEY)).isPresent() && req.getUserPrincipal() == null) {
            LogManager.getLogger().info("request authentication required for: {}", req.getRemoteUser());
            req.authenticate((HttpServletResponse) response);
        }
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
    }

}
