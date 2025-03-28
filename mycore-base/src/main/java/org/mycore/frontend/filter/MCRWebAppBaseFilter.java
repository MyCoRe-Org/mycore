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

import org.mycore.frontend.MCRFrontendUtil;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

public class MCRWebAppBaseFilter implements Filter {
    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException,
        ServletException {
        // check if BASE_URL_ATTRIBUTE is present
        // for used proxy header use the first entry of list
        if (req.getAttribute(MCRFrontendUtil.BASE_URL_ATTRIBUTE) == null) {
            String webappBase = MCRFrontendUtil.getBaseURL(req);
            req.setAttribute(MCRFrontendUtil.BASE_URL_ATTRIBUTE, webappBase);
        }
        chain.doFilter(req, res);
    }

}
