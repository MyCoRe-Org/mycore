package org.mycore.frontend.filter;

import java.io.IOException;
import java.util.regex.Pattern;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.config.MCRConfiguration;

/**
 * Automatically closes HttpSession of certain user agents.
 * 
 * If the <code>User-Agent</code> header matches a regular expression
 * defined by the property <code>MCR.Filter.UserAgent</code>
 * (default: "<code>(bot|spider|crawler|mercator|slurp|seek|nagios)</code>") the
 * HTTP session is closed after the request.
 * @author Thomas Scheffler (yagee)
 */
public class MCRUserAgentFilter implements Filter {
    private static Pattern agentPattern;

    private static final Logger LOGGER = LogManager.getLogger(MCRUserAgentFilter.class);

    @Override
    public void init(final FilterConfig arg0) throws ServletException {
        final String agentRegEx = MCRConfiguration.instance().getString("MCR.Filter.UserAgent",
            "(bot|spider|crawler|mercator|slurp|seek|nagios|Java)");
        agentPattern = Pattern.compile(agentRegEx);
    }

    @Override
    public void destroy() {
    }

    @Override
    public void doFilter(final ServletRequest sreq, final ServletResponse sres, final FilterChain chain)
        throws IOException, ServletException {
        final HttpServletRequest request = (HttpServletRequest) sreq;
        final boolean newSession = request.getSession(false) == null;
        chain.doFilter(sreq, sres);
        final HttpSession session = request.getSession(false);
        if (session != null && newSession) {
            final String userAgent = request.getHeader("User-Agent");
            if (userAgent != null) {
                if (agentPattern.matcher(userAgent).find()) {
                    try {
                        LOGGER.info("Closing session: {} matches {}", userAgent, agentPattern);
                        session.invalidate();
                    } catch (IllegalStateException e) {
                        LOGGER.warn("Session was allready closed");
                    }
                } else {
                    LOGGER.debug("{} does not match {}", userAgent, agentPattern);
                }
            } else {
                LOGGER.warn("No User-Agent was send.");
            }
        }
    }

}
