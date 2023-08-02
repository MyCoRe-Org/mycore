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
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.config.MCRConfiguration2;

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
 * Automatically closes HttpSession of certain user agents.
 * 
 * If the <code>User-Agent</code> header matches a regular expression
 * defined by the property <code>MCR.Filter.UserAgent.Pattern</code> the
 * HTTP session is closed after the request.
 *
 * If the <code>User-Agent</code> header is invalid a 403 FORBIDDEN response
 * is sent to client if not <code>MCR.Filter.UserAgent.AcceptInvalid = true</code>.
 *
 * A <code>User-Agent</code> header is considered invalid if it is not sent at all or
 * its value has not at least <code>MCR.Filter.UserAgent.MinLength</code> characters.
 *
 * @author Thomas Scheffler (yagee)
 */
public class MCRUserAgentFilter implements Filter {
    private static final int MIN_USER_AGENT_LENGTH = MCRConfiguration2
            .getOrThrow("MCR.Filter.UserAgent.MinLength", Integer::parseInt);
    private static final boolean ACCEPT_INVALID_USER_AGENTS = MCRConfiguration2
            .getOrThrow("MCR.Filter.UserAgent.AcceptInvalid", Boolean::parseBoolean);
    private static Pattern agentPattern;

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public void init(final FilterConfig arg0) throws ServletException {
        final String agentRegEx = MCRConfiguration2.getStringOrThrow("MCR.Filter.UserAgent.BotPattern");
        agentPattern = Pattern.compile(agentRegEx);
    }

    @Override
    public void destroy() {
    }

    @Override
    public void doFilter(final ServletRequest sreq, final ServletResponse sres, final FilterChain chain)
        throws IOException, ServletException {
        final HttpServletRequest request = (HttpServletRequest) sreq;
        final HttpServletResponse response = (HttpServletResponse) sres;
        final String userAgent = request.getHeader("User-Agent");
        boolean invalidUserAgent = isInvalidUserAgent(userAgent);
        if (invalidUserAgent) {
            handleInvalidUserAgent(response, userAgent);
            if (response.isCommitted()) {
                return;
            }
        }
        final boolean newSession = request.getSession(false) == null;
        chain.doFilter(sreq, sres);
        final HttpSession session = request.getSession(false);
        if (session != null && newSession) {
            try {
                if (invalidUserAgent) {
                    LOGGER.info("Closing session, invalid User-Agent: " + userAgent);
                    session.invalidate();
                } else if (agentPattern.matcher(userAgent).find()) {
                    LOGGER.info("Closing session: {} matches {}", userAgent, agentPattern);
                    session.invalidate();
                } else {
                    LOGGER.debug("{} does not match {}", userAgent, agentPattern);
                }
            } catch (IllegalStateException e) {
                LOGGER.warn("Session was allready closed");
            }
        }
    }

    private static boolean isInvalidUserAgent(String userAgent) {
        return userAgent == null || userAgent.length() < MIN_USER_AGENT_LENGTH;
    }

    private void handleInvalidUserAgent(HttpServletResponse response, String userAgent) throws IOException {
        if (ACCEPT_INVALID_USER_AGENTS) {
            return;
        }
        //don't use sendError here as we do not want to trigger error handling by container
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        try (var os = response.getOutputStream()) {
            byte[] msg = getInvalidMsg(userAgent).getBytes(StandardCharsets.UTF_8);
            response.setContentLength(msg.length);
            os.write(msg);
        }
        response.flushBuffer();
    }

    private static String getInvalidMsg(String userAgent) {
        return userAgent == null ? "User-Agent header required." : "Invalid User-Agent: " + userAgent;
    }
}
