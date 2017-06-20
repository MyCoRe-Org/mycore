/*
 * $Id$
 * $Revision: 5697 $ $Date: 10.12.2010 $
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
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
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */

package org.mycore.frontend.servlets;

import java.security.Principal;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRSystemUserInformation;
import org.mycore.common.MCRUserInformation;
import org.mycore.frontend.MCRFrontendUtil;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRContainerLoginServlet extends MCRServlet {

    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = LogManager.getLogger(MCRContainerLoginServlet.class);

    /* (non-Javadoc)
     * @see org.mycore.frontend.servlets.MCRServlet#think(org.mycore.frontend.servlets.MCRServletJob)
     */
    @Override
    protected void think(MCRServletJob job) throws Exception {
        MCRSession session = MCRSessionMgr.getCurrentSession();
        session.setUserInformation(new ContainerUserInformation(session));
        LOGGER.info("Logged in: " + session.getUserInformation().getUserID());
    }

    /* (non-Javadoc)
     * @see org.mycore.frontend.servlets.MCRServlet#render(org.mycore.frontend.servlets.MCRServletJob, java.lang.Exception)
     */
    @Override
    protected void render(MCRServletJob job, Exception ex) throws Exception {
        String backto_url = getProperty(job.getRequest(), "url");

        if (backto_url == null) {
            String referer = job.getRequest().getHeader("Referer");
            backto_url = (referer != null) ? referer : MCRFrontendUtil.getBaseURL();
        }
        job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(backto_url));
    }

    protected static class ContainerUserInformation implements MCRUserInformation {
        protected MCRSession session;

        String lastUser;

        public ContainerUserInformation(MCRSession session) {
            this.session = session;
        }

        @Override
        public String getUserID() {
            lastUser = getCurrentRequest()
                .flatMap(r -> Optional.ofNullable(r.getUserPrincipal()))
                .map(Principal::getName)
                .orElseGet(() -> Optional.ofNullable(lastUser)
                    .orElseGet(MCRSystemUserInformation.getGuestInstance()::getUserID));
            return lastUser;
        }

        @Override
        public boolean isUserInRole(String role) {
            return getCurrentRequest().map(r -> r.isUserInRole(role)).orElse(Boolean.FALSE);
        }

        @Override
        public String getUserAttribute(String attribute) {
            return null;
        }

        protected Optional<HttpServletRequest> getCurrentRequest() {
            LogManager.getLogger(getClass()).debug("Getting request from session: " + session.getID());
            return session.getServletRequest();
        }
    }

}
