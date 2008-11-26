/**
 * $RCSfile: MCRUserAgentFilter.java,v $
 * $Revision: 1.0 $ $Date: 26.11.2008 12:30:56 $
 *
 * This file is part of ** M y C o R e **
 * Visit our homepage at http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, normally in the file license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 *
 **/
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

import org.apache.log4j.Logger;

import org.mycore.common.MCRConfiguration;

/**
 * Automatically closes HttpSession of certain user agents.
 * 
 * If the <code>User-Agent</code> header matches a regular expression
 * defined by the property <code>MCR.Filter.UserAgent</code>
 * (default: "<code>(bot|spider|crawler|mercator|slurp|seek)</code>") the
 * HTTP session is closed after the request.
 * @author Thomas Scheffler (yagee)
 */
public class MCRUserAgentFilter implements Filter {
    private static Pattern agentPattern;

    private static final Logger LOGGER = Logger.getLogger(MCRUserAgentFilter.class);

    public void init(FilterConfig arg0) throws ServletException {
        String agentRegEx = MCRConfiguration.instance().getString("MCR.Filter.UserAgent", "(bot|spider|crawler|mercator|slurp|seek)");
        agentPattern = Pattern.compile(agentRegEx);
    }

    public void destroy() {
    }

    public void doFilter(ServletRequest sreq, ServletResponse sres, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) sreq;
        chain.doFilter(sreq, sres);
        HttpSession session = request.getSession(false);
        if (session != null) {
            String userAgent = request.getHeader("User-Agent");
            if (agentPattern.matcher(userAgent).find()) {
                try {
                    session.invalidate();
                    LOGGER.info("Closing session: " + userAgent + " matches " + agentPattern);
                } catch (IllegalStateException e) {
                    //session is already closed
                }
            } else {
                LOGGER.debug(userAgent + " does not match " + agentPattern);
            }
        }
    }

}
