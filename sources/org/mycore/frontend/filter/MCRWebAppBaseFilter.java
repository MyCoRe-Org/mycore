/**
 * $RCSfile: MCRWebAppBaseFilter.java,v $
 * $Revision: 1.0 $ $Date: 11.03.2008 08:36:16 $
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

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.mycore.frontend.servlets.MCRServlet;

public class MCRWebAppBaseFilter implements Filter {

    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        // check if BASE_URL_ATTRIBUTE is present
        if (req.getAttribute(MCRServlet.BASE_URL_ATTRIBUTE) == null) {
            HttpServletRequest request = (HttpServletRequest) req;
            StringBuilder webappBase = new StringBuilder(request.getScheme());
            webappBase.append("://").append(request.getServerName());
            if (!(request.getLocalPort() == 80 || (request.isSecure() && request.getLocalPort() == 443))) {
                webappBase.append(':').append(request.getLocalPort());
            }
            webappBase.append(request.getContextPath()).append('/');
            request.setAttribute(MCRServlet.BASE_URL_ATTRIBUTE, webappBase);
        }
        chain.doFilter(req, res);
    }

    public void destroy() {
    }

    public void init(FilterConfig arg0) throws ServletException {
    }

}
