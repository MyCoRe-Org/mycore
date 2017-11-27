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

package org.mycore.frontend.servlets;

import static org.mycore.access.MCRAccessManager.PERMISSION_READ;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.TransformerException;

import org.mycore.access.MCRAccessManager;
import org.mycore.common.MCRException;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.content.MCRContent;
import org.mycore.datamodel.common.MCRXMLMetadataManager;
import org.mycore.datamodel.ifs2.MCRMetadataStore;
import org.mycore.datamodel.ifs2.MCRVersioningMetadataStore;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.xml.sax.SAXException;

/**
 * Serves a given MCROBject.
 * <em>.../receive/{MCRObjectID}</em>
 * 
 * @author Thomas Scheffler (yagee)
 */
public class MCRObjectServlet extends MCRContentServlet {
    private static final long serialVersionUID = 1L;

    private static final int REV_CURRENT = 0;

    private static final String I18N_ERROR_PREFIX = "component.base.error";

    private MCRXMLMetadataManager metadataManager = null;

    @Override
    public void init() throws ServletException {
        super.init();
        metadataManager = MCRXMLMetadataManager.instance();
    }

    @Override
    public MCRContent getContent(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
        final MCRObjectID mcrid = getMCRObjectID(req, resp);
        if (mcrid == null) {
            return null;
        }
        if (!MCRAccessManager.checkPermission(mcrid, PERMISSION_READ)) { // check read permission for ID
            final MCRSession currentSession = MCRSessionMgr.getCurrentSession();
            resp.sendError(
                HttpServletResponse.SC_UNAUTHORIZED,
                getErrorI18N(I18N_ERROR_PREFIX, "accessDenied", mcrid.toString(), currentSession.getUserInformation()
                    .getUserID(), currentSession.getCurrentIP()));
            return null;
        }
        long rev = REV_CURRENT;
        final String revision = getProperty(req, "r");
        if (revision != null) {
            rev = Long.parseLong(revision);
        }
        MCRContent localObject = (rev == REV_CURRENT) ? requestLocalObject(mcrid, resp)
            : requestVersionedObject(mcrid,
                resp, rev);
        if (localObject == null) {
            return null;
        }
        try {
            return getLayoutService().getTransformedContent(req, resp, localObject);
        } catch (TransformerException | SAXException e) {
            throw new IOException(e);
        }
    }

    private MCRContent requestLocalObject(MCRObjectID mcrid, final HttpServletResponse resp) throws IOException {
        if (MCRMetadataManager.exists(mcrid)) {
            return metadataManager.retrieveContent(mcrid);
        }
        resp.sendError(HttpServletResponse.SC_NOT_FOUND, getErrorI18N(I18N_ERROR_PREFIX, "notFound", mcrid));
        return null;
    }

    private MCRContent requestVersionedObject(final MCRObjectID mcrid, final HttpServletResponse resp, final long rev)
        throws IOException {
        MCRXMLMetadataManager xmlMetadataManager = MCRXMLMetadataManager.instance();
        MCRMetadataStore metadataStore = xmlMetadataManager.getStore(mcrid);
        if (metadataStore instanceof MCRVersioningMetadataStore) {
            MCRContent content = xmlMetadataManager.retrieveContent(mcrid, rev);
            if (content != null) {
                return content;
            }
            resp.sendError(HttpServletResponse.SC_NOT_FOUND,
                getErrorI18N(I18N_ERROR_PREFIX, "revisionNotFound", rev, mcrid));
            return null;
        }
        resp.sendError(HttpServletResponse.SC_BAD_REQUEST, getErrorI18N(I18N_ERROR_PREFIX, "noVersions", mcrid));
        return null;
    }

    private MCRObjectID getMCRObjectID(final HttpServletRequest req, final HttpServletResponse resp)
        throws IOException {
        final String pathInfo = req.getPathInfo();
        final String id = pathInfo == null ? null : pathInfo.substring(1);

        MCRObjectID mcrid = null;
        if (id != null) {
            try {
                mcrid = MCRObjectID.getInstance(id); // create Object with given ID, only ID syntax check performed
            } catch (final MCRException e) { // handle exception: invalid ID syntax, set HTTP error 400 "Invalid request"
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, getErrorI18N(I18N_ERROR_PREFIX, "invalidID", id));
                return null; // sorry, no object to return
            }
        }
        return mcrid;
    }
}
