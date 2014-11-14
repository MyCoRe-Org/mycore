/*
 * $Id$
 * $Revision: 5697 $ $Date: Jun 19, 2014 $
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

package org.mycore.mods;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.mycore.common.MCRCache;
import org.mycore.common.MCRCache.ModifiedHandle;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRSystemUserInformation;
import org.mycore.datamodel.common.MCRISO8601Date;
import org.mycore.datamodel.common.MCRISO8601Format;
import org.mycore.datamodel.common.MCRXMLMetadataManager;
import org.mycore.datamodel.ifs.MCRFileNodeServlet;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.services.i18n.MCRTranslation;

/**
 * Enforces embargo of mods documents to {@link MCRFileNodeServlet}.
 * @author Thomas Scheffler (yagee)
 */
public class MCRMODSEmbargoFilter implements Filter {

    private static final int CAPACITY = 10000;

    private static final Logger LOGGER = Logger.getLogger(MCRMODSEmbargoFilter.class);

    private static final String EMPTY_VALUE = "";

    private static final long EXPIRE = 1;

    private static final TimeUnit EXPIRE_UNIT = TimeUnit.HOURS;

    private MCRCache<MCRObjectID, String> embargoCache = new MCRCache<>(CAPACITY, "MODS embargo filter cache");

    /* (non-Javadoc)
     * @see javax.servlet.Filter#init(javax.servlet.FilterConfig)
     */
    @Override
    public void init(FilterConfig config) throws ServletException {
    }

    @Override
    public void destroy() {
        embargoCache.close();
    }

    /* (non-Javadoc)
     * @see javax.servlet.Filter#doFilter(javax.servlet.ServletRequest, javax.servlet.ServletResponse, javax.servlet.FilterChain)
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException,
        ServletException {
        boolean forward = true; //forward to servlet by default
        HttpServletRequest req = (HttpServletRequest) request;
        boolean newSession = req.getSession(false) == null;
        MCRSession session = MCRServlet.getSession(req);
        MCRSessionMgr.setCurrentSession(session);
        try {
            String ownerID = MCRFileNodeServlet.getOwnerID(req);
            MCRObjectID derivateID = MCRObjectID.getInstance(ownerID);
            //get session for DB access
            session.beginTransaction();
            try {
                MCRObjectID objectId = MCRMetadataManager.getObjectId(derivateID, EXPIRE, EXPIRE_UNIT);
                if (objectId == null || !"mods".equals(objectId.getTypeId())) {
                    return; //no embargo check for non MODS documents
                }
                if (!(newSession || session.getUserInformation().getUserID()
                    .equals(MCRSystemUserInformation.getGuestInstance().getUserID()))) {
                    return; //user is logged in
                }
                String embargo = getEmbargo(objectId);
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Current user: " + session.getUserInformation().getUserID());
                    LOGGER.debug("embargo for " + derivateID + " of " + objectId + ": " + embargo);
                    if (embargo != null) {
                        String currentDateTime = getCurrentDateTime();
                        LOGGER.debug("Current time: " + currentDateTime);
                        LOGGER.debug("Compare embargo to current time: " + embargo.compareTo(currentDateTime));
                    }
                }
                if (embargo != null && embargo.compareTo(getCurrentDateTime()) > 0) {
                    LOGGER.warn(MessageFormat.format("Denied request {0} that is under embargo until {1}.",
                        req.getPathInfo(), embargo));
                    String embargoMsg = MCRTranslation.translate("component.mods.error.underEmbargo",
                        req.getPathInfo(), embargo);
                    ((HttpServletResponse) response).sendError(HttpServletResponse.SC_FORBIDDEN, embargoMsg);
                    forward = false;
                    return;
                }
            } finally {
                session.commitTransaction();
            }
        } finally {
            MCRSessionMgr.releaseCurrentSession();
            if (newSession) {
                HttpSession httpSession = req.getSession(false);
                if (httpSession != null) {
                    httpSession.invalidate();
                }
            }
            if (forward) {
                LOGGER.info("Request " + req.getPathInfo() + " verified by " + getClass().getSimpleName());
                filterChain.doFilter(request, response);
            }
        }
    }

    private String getEmbargo(MCRObjectID objectId) {
        ModifiedHandle modifiedHandle = MCRXMLMetadataManager.instance().getLastModifiedHandle(objectId, 10,
            TimeUnit.MINUTES);
        String embargo = null;
        try {
            embargo = embargoCache.getIfUpToDate(objectId, modifiedHandle);
        } catch (IOException e) {
            LOGGER.warn("Could not determine last modified timestamp of object " + objectId);
        }
        if (embargo != null) {
            return embargo == EMPTY_VALUE ? null : embargo;
        }
        MCRMODSWrapper modsWrapper = new MCRMODSWrapper(MCRMetadataManager.retrieveMCRObject(objectId));
        embargo = modsWrapper.getElementValue("mods:accessCondition[@type='embargo']");
        embargoCache.put(objectId, embargo != null ? embargo : EMPTY_VALUE);
        return embargo;
    }

    private static String getCurrentDateTime() {
        MCRISO8601Date now = new MCRISO8601Date();
        now.setFormat(MCRISO8601Format.COMPLETE_HH_MM_SS);
        now.setDate(new Date());
        return now.getISOString();
    }

}
