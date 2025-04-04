/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
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
import java.net.URISyntaxException;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.datamodel.ifs.MCRFileNodeServlet;
import org.mycore.frontend.MCRFrontendUtil;
import org.mycore.frontend.support.MCRSecureTokenV2;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Filter for {@link MCRFileNodeServlet} that uses {@link MCRSecureTokenV2} to check access to specific file types.
 * <p>
 * used properties:
 * </p>
 * <dl>
 * <dt>MCR.SecureTokenV2.Extensions=mp4,mpeg4</dt>
 * <dd>List of file extension. If empty, disables this filter</dd>
 * <dt>MCR.SecureTokenV2.HashParameter=securetoken</dt>
 * <dd>Name of request parameter that holds hash value</dd>
 * <dt>MCR.SecureTokenV2.SharedSecret=mySharedSecret</dt>
 * <dd>shared secret used to calculate secure token</dd>
 * </dl>
 * <code>contentPath</code> used for {@link MCRSecureTokenV2} is the {@link HttpServletRequest#getPathInfo() path info}
 * without leading '/'.
 *
 * @author Thomas Scheffler (yagee)
 */
public class MCRSecureTokenV2Filter implements Filter {

    private static final Logger LOGGER = LogManager.getLogger();

    private boolean filterEnabled = true;

    private String hashParameter;

    private String sharedSecret;

    /* (non-Javadoc)
     * @see jakarta.servlet.Filter#init(jakarta.servlet.FilterConfig)
     */
    @Override
    public void init(FilterConfig filterConfig) {
        filterEnabled = MCRSecureTokenV2FilterConfig.isFilterEnabled();
        hashParameter = MCRSecureTokenV2FilterConfig.getHashParameterName();
        sharedSecret = MCRSecureTokenV2FilterConfig.getSharedSecret();
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException {
        if (filterEnabled) {
            HttpServletRequest httpServletRequest = (HttpServletRequest) request;
            String pathInfo = httpServletRequest.getPathInfo();
            if (pathInfo != null && MCRSecureTokenV2FilterConfig.requireHash(pathInfo)
                && !validateSecureToken(httpServletRequest)) {
                ((HttpServletResponse) response).sendError(HttpServletResponse.SC_FORBIDDEN);
                LOGGER.warn("Access to {} forbidden by secure token check.", pathInfo);
                return;
            }
        }
        chain.doFilter(request, response);
    }

    private boolean validateSecureToken(HttpServletRequest httpServletRequest) throws ServletException {
        String queryString = httpServletRequest.getQueryString();
        if (queryString == null) {
            LOGGER.warn("Request contains no parameters {}.", httpServletRequest::getRequestURL);
        }
        String hashValue = httpServletRequest.getParameter(hashParameter);
        if (hashValue == null) {
            LOGGER.warn("Could not find parameter '{}' in request {}.",
                () -> hashParameter, () -> httpServletRequest.getRequestURL().append('?').append(queryString));
            return false;
        }
        String[] origParams = Pattern.compile("&").split(queryString);
        String[] stripParams = new String[origParams.length - 1];
        for (int i = origParams.length - 1; i > -1; i--) {
            if (origParams[i].startsWith(hashParameter + "=")) {
                removeElement(origParams, stripParams, i);
            }
        }
        MCRSecureTokenV2 token = new MCRSecureTokenV2(httpServletRequest.getPathInfo().substring(1),
            MCRFrontendUtil.getRemoteAddr(httpServletRequest), sharedSecret, stripParams);
        LOGGER.info(() -> {
            try {
                return token.toURI(MCRFrontendUtil.getBaseURL() + "servlets/MCRFileNodeServlet/", hashParameter);
            } catch (URISyntaxException e) {
                return e.getMessage();
            }
        });
        return hashValue.equals(token.getHash());
    }

    private static void removeElement(String[] src, String[] dest, int i) {
        if (i == 0) {
            return;
        }
        System.arraycopy(src, 0, dest, 0, i - 1);
        if (i < src.length - 1) {
            System.arraycopy(src, i + 1, dest, i, src.length - 1 - i);
        }
    }
}
