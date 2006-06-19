/*
 * $RCSfile$
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

import java.util.Hashtable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import org.jdom.Document;
import org.mycore.access.MCRAccessManager;
import org.mycore.common.MCRCache;
import org.mycore.common.MCRException;
import org.mycore.common.MCRSessionMgr;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.metadata.MCRXMLTableManager;
import org.mycore.services.fieldquery.MCRHit;
import org.mycore.services.fieldquery.MCRResults;
import org.mycore.services.fieldquery.MCRSearchServlet;

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

    private static MCRXMLTableManager TM = null;

    private static final Pattern SEARCH_ID_PATTERN = Pattern.compile("[\\?&]id=([^&]+)");

    private static final String EDITOR_ID_TABLE_KEY = "MCRObjectServlet.editorIds";

    /**
     * The initalization of the servlet.
     * 
     * @see javax.servlet.GenericServlet#init()
     */
    public void init() throws ServletException {
        super.init();
        TM = MCRXMLTableManager.instance();
    }

    /**
     * The method replace the default form MCRServlet and redirect the
     * MCRLayoutServlet.
     * 
     * @param job
     *            the MCRServletJob instance
     */
    public void doGetPost(MCRServletJob job) throws ServletException, Exception {
        String id = getObjectID(job.getRequest());

        try {
            MCRObjectID mcrid = new MCRObjectID(id);

            if (!MCRAccessManager.checkPermission(mcrid, "read")) {
                StringBuffer msg = new StringBuffer(1024);
                msg.append("Access denied reading MCRObject with ID: ").append(mcrid.getId());
                msg.append(".\nCurrent User: ").append(MCRSessionMgr.getCurrentSession().getCurrentUserID());
                msg.append("\nRemote IP: ").append(MCRSessionMgr.getCurrentSession().getCurrentIP());
                generateErrorPage(job.getRequest(),job.getResponse(),HttpServletResponse.SC_FORBIDDEN,msg.toString(),null,false);
                return;
            }

            Document doc=TM.readDocument(mcrid);
            // call the LayoutServlet
            job.getRequest().setAttribute("MCRLayoutServlet.Input.JDOM", doc);
            String editorID=getEditorID(job.getRequest());
            setBrowseParameters(job, mcrid, editorID);
            RequestDispatcher rd = getServletContext().getNamedDispatcher("MCRLayoutServlet");
            rd.forward(job.getRequest(), job.getResponse());
        } catch (MCRException e) {
            generateErrorPage(job.getRequest(), job.getResponse(), HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error while retrieving MCRObject with ID: "
                    + id, e, false);
            return;
        }
    }

    private void setBrowseParameters(MCRServletJob job, MCRObjectID mcrid, String editorID) {
        if (editorID==null){
            return;
        }
        MCRCache parameterCache=(MCRCache) MCRSessionMgr.getCurrentSession().get(MCRSearchServlet.getParametersKey());
        MCRSearchServlet.SearchParameters sp = (parameterCache==null)? null:(MCRSearchServlet.SearchParameters) (parameterCache).get(editorID);

        if (sp!=null){
            //editorID found and editorSession still valid
            storeEditorID(mcrid.toString(),editorID);
            job.getRequest().setAttribute("XSL.resultListEditorID",editorID);
            job.getRequest().setAttribute("XSL.numPerPage",String.valueOf(sp.numPerPage));
            job.getRequest().setAttribute("XSL.page",String.valueOf(sp.page));
            MCRResults results= (MCRResults)((MCRCache)MCRSessionMgr.getCurrentSession().get(MCRSearchServlet.getResultsKey())).get(editorID);
            MCRHit previousObject=null, nextObject=null;
            int numHits=results.getNumHits();
            for (int i=0;i<numHits;i++){
                LOGGER.debug("Hit: "+results.getHit(i).getID());
                if (results.getHit(i).getID().equals(mcrid.toString())){
                    //hit allocated
                    //search for next and previous object readable by user
                    for (int j=i-1;j>=0;j--){
                        if (MCRAccessManager.checkPermission(results.getHit(j).getID(), "read")) {
                            previousObject=results.getHit(j);
                            break;
                        }
                    }
                    for (int j=i+1;j<numHits;j++){
                        if (MCRAccessManager.checkPermission(results.getHit(j).getID(), "read")) {
                            nextObject=results.getHit(j);
                            break;
                        }
                    }
                    break;
                }
            }
            if (previousObject!=null){
                job.getRequest().setAttribute("XSL.previousObject",previousObject.getID());
            }
            if (nextObject!=null){
                job.getRequest().setAttribute("XSL.nextObject",nextObject.getID());
            }
        }
    }

    private static final String getObjectID(HttpServletRequest request) {
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
    
    private final String getEditorID(HttpServletRequest request) {
        String referer=request.getHeader("Referer");
        if (referer==null){
            return null;
        }
        if (-1 != referer.indexOf("MCRSearchServlet")){
            return getEditorIDFromSearch(referer);
        }
        return getEditorIDFromObjectID(request, referer);
    }
    
    protected static final String getEditorIDFromSearch(String referer){
        Matcher m=SEARCH_ID_PATTERN.matcher(referer);
        m.find();
        LOGGER.debug("Group count: "+m.groupCount());
        String editorID=m.group(1);
        return editorID;
    }

    protected final String getEditorIDFromObjectID(HttpServletRequest request, String referer){
        String servletPath=request.getServletPath();
        Pattern p=Pattern.compile(servletPath+"([^;\\?]*)");
        Matcher m=p.matcher(referer);
        if (m.find()){
            return resolveEditorID(getIDFromPathInfo(m.group(1)));
        }
        LOGGER.debug("Didn't found ID in referer: "+m.toString());
        return resolveEditorID(getObjectID(request));
    }
    
    protected final static String resolveEditorID(String objectID){
        Hashtable h=(Hashtable)MCRSessionMgr.getCurrentSession().get(EDITOR_ID_TABLE_KEY);
        if (h==null){
            return null;
        }
        Object o = h.get(objectID);
        return (o == null) ? null : o.toString();
    }

    protected final static void storeEditorID(String objectID, String editorID){
        Hashtable h=(Hashtable)MCRSessionMgr.getCurrentSession().get(EDITOR_ID_TABLE_KEY);
        if (h==null){
            h=new Hashtable();
            MCRSessionMgr.getCurrentSession().put(EDITOR_ID_TABLE_KEY,h);
        }
        LOGGER.debug("Storing editorID: "+editorID+" to MCRObjectID: "+objectID);
        h.put(objectID,editorID);
    }
}