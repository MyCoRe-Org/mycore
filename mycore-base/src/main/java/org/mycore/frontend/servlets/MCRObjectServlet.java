/*
 * 
 * $Revision$ $Date$
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

import static org.mycore.access.MCRAccessManager.PERMISSION_READ;

import java.io.IOException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Hashtable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.mycore.access.MCRAccessManager;
import org.mycore.common.MCRException;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRDOMContent;
import org.mycore.datamodel.common.MCRXMLMetadataManager;
import org.mycore.datamodel.ifs2.MCRMetadataStore;
import org.mycore.datamodel.ifs2.MCRVersioningMetadataStore;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.services.fieldquery.MCRCachedQueryData;
import org.mycore.services.fieldquery.MCRHit;
import org.mycore.services.fieldquery.MCRQueryClient;
import org.mycore.services.fieldquery.MCRResults;

/**
 * This servlet response the MCRObject certain by the call path
 * <em>.../receive/MCRObjectID</em> or
 * <em>.../servlets/MCRObjectServlet/id=MCRObjectID[&XSL.Style=...]</em>.
 * 
 * @author Jens Kupferschmidt
 * @author Anja Schaar
 * @author Thomas Scheffler (yagee)
 * 
 * @see org.mycore.frontend.servlets.MCRServlet
 */
public class MCRObjectServlet extends MCRServlet {
    private static final long serialVersionUID = 1L;

    private static Logger LOGGER = Logger.getLogger(MCRObjectServlet.class);

    private static MCRXMLMetadataManager TM = null;

    private static final Pattern SEARCH_ID_PATTERN = Pattern.compile("[\\?&]id=([^&]+)");

    private static final String EDITOR_ID_TABLE_KEY = "MCRObjectServlet.editorIds";

    private static final int REV_CURRENT = 0;

    private static final String I18N_ERROR_PREFIX = "component.base.error";

    /**
     * The initalization of the servlet.
     * 
     * @see javax.servlet.GenericServlet#init()
     */
    @Override
    public void init() throws ServletException {
        super.init();
        TM = MCRXMLMetadataManager.instance();
    }

    /**
     * The method replace the default form MCRServlet and redirect the
     * MCRLayoutService.
     * 
     * @param job
     *            the MCRServletJob instance
     */
    @Override
    public void doGetPost(MCRServletJob job) throws Exception {
        String host = getObjectHost(job);
        String id = getObjectID(job.getRequest());
        if (id == null || id.length() == 0) {
            return; // request failed;
        }
        String editorID = getEditorID(job.getRequest());
        setBrowseParameters(job, id, host, editorID);
        String revision = getProperty(job.getRequest(), "r");
        long rev = REV_CURRENT;
        if (revision != null) {
            rev = Long.parseLong(revision);
        }

        if (host == MCRHit.LOCAL) {
            MCRContent localObject;
            if (rev == REV_CURRENT) {
                localObject = requestLocalObject(job);
            } else {
                localObject = requestVersionedObject(job, rev);
            }
            if (localObject == null) {
                return;
            }
            getLayoutService().doLayout(job.getRequest(), job.getResponse(), localObject);
        } else {
            getLayoutService().doLayout(job.getRequest(), job.getResponse(),
                new MCRDOMContent(requestRemoteObject(job)));
        }
    }

    private String getObjectHost(MCRServletJob job) {
        String remoteHost = job.getRequest().getParameter("host");
        if (remoteHost == null || remoteHost.length() == 0) {
            remoteHost = MCRHit.LOCAL;
        }
        return remoteHost;
    }

    //---------------------------------------------------------------------

    private org.w3c.dom.Document requestRemoteObject(MCRServletJob job) {
        String id = getObjectID(job.getRequest());
        String host = getProperty(job.getRequest(), "host");
        return MCRQueryClient.doRetrieveObject(host, id);
    }

    //---------------------------------------------------------------------

    private MCRContent requestLocalObject(MCRServletJob job) throws IOException {
        MCRObjectID mcrid = getMCRObjectID(job);
        if (mcrid == null)
            return null;
        if (MCRMetadataManager.exists(mcrid)) {
            return TM.retrieveContent(mcrid);
        }
        job.getResponse().sendError(HttpServletResponse.SC_NOT_FOUND,
            getErrorI18N(I18N_ERROR_PREFIX, "notFound", mcrid));
        return null;
    }

    private MCRObjectID getMCRObjectID(MCRServletJob job) throws IOException {
        String id = getObjectID(job.getRequest());

        MCRObjectID mcrid = null;
        try {
            mcrid = MCRObjectID.getInstance(id); // create Object with given ID, only ID syntax check performed
        } catch (MCRException e) { // handle exception: invalid ID syntax, set HTTP error 400 "Invalid request"
            job.getResponse().sendError(HttpServletResponse.SC_BAD_REQUEST,
                getErrorI18N(I18N_ERROR_PREFIX, "invalidID", id));
            return null; // sorry, no object to return
        }

        if (!MCRAccessManager.checkPermission(mcrid, PERMISSION_READ)) { // check read permission for ID
            MCRSession currentSession = MCRSessionMgr.getCurrentSession();
            job.getResponse().sendError(
                HttpServletResponse.SC_UNAUTHORIZED,
                getErrorI18N(I18N_ERROR_PREFIX, "accessDenied", mcrid.toString(), currentSession.getUserInformation()
                    .getUserID(), currentSession.getCurrentIP()));
            return null;
        }
        return mcrid;
    }

    private MCRContent requestVersionedObject(MCRServletJob job, long rev) throws IOException {
        final MCRObjectID mcrid = getMCRObjectID(job);
        if (mcrid == null) {
            return null;
        }
        MCRXMLMetadataManager xmlMetadataManager = MCRXMLMetadataManager.instance();
        MCRMetadataStore metadataStore = xmlMetadataManager.getStore(mcrid);
        if (metadataStore instanceof MCRVersioningMetadataStore) {
            MCRContent content = xmlMetadataManager.retrieveContent(mcrid, rev);
            if (content != null) {
                return content;
            }
            job.getResponse().sendError(HttpServletResponse.SC_NOT_FOUND,
                getErrorI18N(I18N_ERROR_PREFIX, "revisionNotFound", rev, mcrid));
            return null;
        }
        job.getResponse().sendError(HttpServletResponse.SC_BAD_REQUEST,
            getErrorI18N(I18N_ERROR_PREFIX, "noVersions", mcrid));
        return null;
    }

    private void setBrowseParameters(MCRServletJob job, String mcrid, String host, String editorID) {
        if (host != MCRHit.LOCAL) {
            job.getRequest().setAttribute("XSL.objectHost", host);
        }
        if (editorID == null) {
            return;
        }

        MCRCachedQueryData qd = MCRCachedQueryData.getData(editorID);

        if (qd != null) {
            // editorID found and editorSession still valid
            storeEditorID(mcrid, editorID);
            job.getRequest().setAttribute("XSL.resultListEditorID", editorID);
            job.getRequest().setAttribute("XSL.numPerPage", String.valueOf(qd.getNumPerPage()));
            job.getRequest().setAttribute("XSL.page", String.valueOf(qd.getPage()));
            MCRResults results = qd.getResults();
            MCRHit previousObject = null, nextObject = null;
            int numHits = results.getNumHits();
            for (int i = 0; i < numHits; i++) {
                LOGGER.debug("Hit: " + results.getHit(i).getID());
                if (results.getHit(i).getID().equals(mcrid) && results.getHit(i).getHost().equals(host)) {
                    // hit allocated
                    // search for next and previous object readable by user
                    for (int j = i - 1; j >= 0; j--) {
                        if (results.getHit(j).getHost() != MCRHit.LOCAL
                            || MCRAccessManager.checkPermission(results.getHit(j).getID(), PERMISSION_READ)) {
                            previousObject = results.getHit(j);
                            break;
                        }
                    }
                    for (int j = i + 1; j < numHits; j++) {
                        if (results.getHit(j).getHost() != MCRHit.LOCAL
                            || MCRAccessManager.checkPermission(results.getHit(j).getID(), PERMISSION_READ)) {
                            nextObject = results.getHit(j);
                            break;
                        }
                    }
                    break;
                }
            }
            if (previousObject != null) {
                job.getRequest().setAttribute("XSL.previousObject", previousObject.getID());
                if (previousObject.getHost() != MCRHit.LOCAL) {
                    job.getRequest().setAttribute("XSL.previousObjectHost", previousObject.getHost());
                }
            }
            if (nextObject != null) {
                job.getRequest().setAttribute("XSL.nextObject", nextObject.getID());
                if (nextObject.getHost() != MCRHit.LOCAL) {
                    job.getRequest().setAttribute("XSL.nextObjectHost", nextObject.getHost());
                }
            }
        }
    }

    private static String getObjectID(HttpServletRequest request) {
        // the urn with information about the MCRObjectID
        String uri = request.getPathInfo();

        if (uri != null) {
            return getIDFromPathInfo(uri);
        }
        return getProperty(request, "id");
    }

    private static String getIDFromPathInfo(String pathInfo) {
        int j = pathInfo.length();
        LOGGER.debug("Path = " + pathInfo + "-->" + pathInfo.substring(1, j));
        return pathInfo.substring(1, j);
    }

    private String getEditorID(HttpServletRequest request) {
        String referer = getProperty(request, "referer");
        if (referer != null) {
            return resolveEditorID(referer);
        }
        URL refererURL = getReferer(request);
        if (refererURL == null) {
            return null;
        }
        referer = refererURL.toString();
        if (referer.contains("MCRSearchServlet")) {
            return getEditorIDFromSearch(referer);
        }
        return getEditorIDFromObjectID(request, referer);
    }

    protected static String getEditorIDFromSearch(String referer) {
        Matcher m = SEARCH_ID_PATTERN.matcher(referer);
        m.find();
        LOGGER.debug("Group count: " + m.groupCount());
        try {
            String editorID = m.group(1);
            return editorID;
        } catch (RuntimeException e) {
            LOGGER.warn("Exception occured while parsing referer: " + referer, e);
            throw e;
        }
    }

    protected final String getEditorIDFromObjectID(HttpServletRequest request, String referer) {
        String servletPath = request.getServletPath();
        //matches all referers with the same servletPath
        Pattern p = Pattern.compile(MessageFormat.format("^[^;\\?]+{0}([^;\\?]*)", servletPath));
        Matcher m = p.matcher(referer);
        if (m.find()) {
            try {
                return resolveEditorID(getIDFromPathInfo(m.group(1)));
            } catch (RuntimeException e) {
                LOGGER.warn("Exception occured while parsing referer: " + referer, e);
                throw e;
            }
        }
        LOGGER.debug("Didn't found ID in referer: " + m.toString());
        return resolveEditorID(getObjectID(request));
    }

    protected static String resolveEditorID(String objectID) {
        @SuppressWarnings("unchecked")
        Hashtable<String, String> h = (Hashtable<String, String>) MCRSessionMgr.getCurrentSession().get(
            EDITOR_ID_TABLE_KEY);
        if (h == null) {
            return null;
        }
        Object o = h.get(objectID);
        return o == null ? null : o.toString();
    }

    protected static void storeEditorID(String objectID, String editorID) {
        @SuppressWarnings("unchecked")
        Hashtable<String, String> h = (Hashtable<String, String>) MCRSessionMgr.getCurrentSession().get(
            EDITOR_ID_TABLE_KEY);
        if (h == null) {
            h = new Hashtable<String, String>();
            MCRSessionMgr.getCurrentSession().put(EDITOR_ID_TABLE_KEY, h);
        }
        LOGGER.debug("Storing editorID: " + editorID + " to MCRObjectID: " + objectID);
        h.put(objectID, editorID);
    }
}
