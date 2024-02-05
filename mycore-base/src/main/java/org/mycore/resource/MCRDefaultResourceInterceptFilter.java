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

package org.mycore.resource;

import java.io.IOException;
import java.util.Enumeration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;

/**
 * A {@link HttpFilter} that delivers web resources identified by the {@link HttpServletRequest#getServletPath()}
 * of the incoming request using an internally instantiated {@link MCRResourceServlet}.
 * <p>
 * Intended to be mapped to the servlet name <code>default</code>, i.e.:
 * <pre>
 * &lt;filter-mapping&gt;
 *   &lt;filter-name&gt;MCRDefaultResourceInterceptFilter&lt;/filter-name&gt;
 *   &lt;servlet-name&gt;default&lt;/servlet-name&gt;
 * &lt;/filter-mapping&gt;
 * </pre>
 * <p>
 * This will intercept request that aren't handled by any registered servlet and would normally be handed over to the
 * default servlet of the web container, i.e. <em>Tomcat</em> or <em>Jetty</em>, which serve the content of the
 * webapp directory (and the content inside <code>/META-INF/resources</code> directories of JAR files) as web content,
 * using the default servlet.
 * <p>
 * This filter, when configured as mentioned above, replaces this default behaviour and serves resources
 * found by a deviating resource lookup strategy, specifically {@link MCRResourceResolver#resolveWebResource(String)}.
 */
public final class MCRDefaultResourceInterceptFilter extends HttpFilter {

    private static final Logger LOGGER = LogManager.getLogger();

    private MCRResourceServlet servlet;

    @Override
    public void init(FilterConfig config) throws ServletException {

        servlet = new MCRResourceServlet();
        servlet.init(getServletConfig(config));

    }

    private static ServletConfig getServletConfig(FilterConfig config) {
        return new ServletConfig() {

            @Override
            public String getServletName() {
                return "internal resource servlet";
            }

            @Override
            public ServletContext getServletContext() {
                return config.getServletContext();
            }

            @Override
            public String getInitParameter(String name) {
                return config.getInitParameter(name);
            }

            @Override
            public Enumeration<String> getInitParameterNames() {
                return config.getInitParameterNames();
            }

        };
    }

    @Override
    public void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException {
        String path = request.getServletPath();
        if (path != null) {
            LOGGER.debug("Responding to request for {} with internal resource servlet", path);
            servlet.service(getModifiedRequest(request), response);
        } else {
            LOGGER.debug("Responding to request with default behaviour");
            chain.doFilter(request, response);
        }
    }

    private static HttpServletRequest getModifiedRequest(HttpServletRequest request) {
        return new HttpServletRequestWrapper(request) {

            @Override
            public String getContextPath() {
                return super.getContextPath();
            }

            @Override
            public String getServletPath() {
                return "/resources";
            }

            @Override
            public String getPathInfo() {
                return super.getServletPath();
            }

        };
    }

}
