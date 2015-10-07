/**
 *
 */
package org.mycore.frontend.filter;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.regex.Pattern;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.datamodel.ifs.MCRFileNodeServlet;
import org.mycore.frontend.MCRFrontendUtil;
import org.mycore.frontend.support.MCRSecureTokenV2;

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
     * @see javax.servlet.Filter#init(javax.servlet.FilterConfig)
     */
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
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
            if (pathInfo != null && MCRSecureTokenV2FilterConfig.requireHash(pathInfo)) {
                if (!validateSecureToken(httpServletRequest)) {
                    ((HttpServletResponse) response).sendError(HttpServletResponse.SC_FORBIDDEN);
                    LOGGER.warn("Access to " + pathInfo + " forbidden by secure token check.");
                    return;
                }
            }
        }
        chain.doFilter(request, response);
    }

    private boolean validateSecureToken(HttpServletRequest httpServletRequest) throws ServletException {
        String queryString = httpServletRequest.getQueryString();
        if (queryString == null) {
            LOGGER.warn("Request contains no parameters {}.", httpServletRequest.getRequestURL());
        }
        String hashValue = httpServletRequest.getParameter(hashParameter);
        if (hashValue == null) {
            LOGGER.warn("Could not find parameter '{}' in request {}.", hashParameter,
                httpServletRequest.getRequestURL().append('?').append(queryString));
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
        try {
            LOGGER.info(token.toURI(MCRFrontendUtil.getBaseURL() + "servlets/MCRFileNodeServlet/", hashParameter));
        } catch (URISyntaxException e) {
            throw new ServletException(e);
        }
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

    @Override
    public void destroy() {
    }

}
