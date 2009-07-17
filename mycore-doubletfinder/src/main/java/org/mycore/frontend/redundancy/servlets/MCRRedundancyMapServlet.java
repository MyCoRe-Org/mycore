/*
 * $RCSfile$
 * $Revision: 817 $ $Date: 2009-04-21 13:22:33 +0200 (Di, 21 Apr 2009) $
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

package org.mycore.frontend.redundancy.servlets;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.servlet.ServletException;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.filter.ElementFilter;
import org.jdom.filter.Filter;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.frontend.redundancy.MCRRedundancyUtil;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;
import org.mycore.user.MCRUserMgr;

/**
 * @author Matthias Eichner
 */
public class MCRRedundancyMapServlet extends MCRServlet {

    private static final long serialVersionUID = 1L;

    private static Logger LOGGER = Logger.getLogger(MCRRedundancyMapServlet.class);;

    private int nonDoubletCount;

    private int doubletCount;

    private int notWorkedCount;

    private int errorCount;

    public void init() throws ServletException {
        super.init();
    }

    public synchronized void doGetPost(MCRServletJob job) throws JDOMException, IOException {
        // init params
        MCRSession session = MCRSessionMgr.getCurrentSession();
        String redMap = job.getRequest().getParameter("redunMap");
        String filename = redMap.substring(redMap.indexOf("/") + 1);
        String redMapPath = MCRRedundancyUtil.DIR + filename;

        // get redun list
        SAXBuilder builder = new SAXBuilder();
        Document redunMap = builder.build(redMapPath);
        LOGGER.debug("read duplicate list from file=" + redMapPath);

        // get duplicate entry
        String redunObject = job.getRequest().getParameter("redunObject").trim();

        String xPathExpr = "//redundancyObjects[@id=" + redunObject + "]";
        org.jdom.xpath.XPath xp = org.jdom.xpath.XPath.newInstance(xPathExpr);
        Element redundancyObjectsElement = (Element) xp.selectSingleNode(redunMap);

        initCounter(job, redundancyObjectsElement);
        // check if some inputs are invalid
        int errorId = validateInput();
        if (errorId != -1) {
            forwardExceptionToClient(job, errorId);
            return;
        }

        // update the xml structure
        updateXML(job, session, redundancyObjectsElement);

        // save redun list
        Format format = Format.getPrettyFormat();
        FileOutputStream fos = new FileOutputStream(new File(redMapPath));
        XMLOutputter xo = new XMLOutputter(format);
        xo.output(redunMap, fos);
        fos.flush();
        fos.close();
        LOGGER.debug("saved changed  dublicate list to file=" + redMapPath);

        int maxObjects = redunMap.getRootElement().getContent(new ElementFilter()).size();
        // send to client
        forwardToClient(job, maxObjects);
    }

    /**
     * Inits counters for the group.
     */
    private void initCounter(MCRServletJob job, Element redObjectsElement) {
        nonDoubletCount = 0;
        doubletCount = 0;
        notWorkedCount = 0;
        errorCount = 0;
        Filter elementAndObjectFilter = new ElementFilter("object");
        for (int i = 1; i <= redObjectsElement.getContent(elementAndObjectFilter).size(); i++) {
            String status = job.getRequest().getParameter("selection_" + i);
            if (status == null || status.equals("")) {
                notWorkedCount++;
            } else if (status.equals("doublet")) {
                doubletCount++;
            } else if (status.equals("nonDoublet")) {
                nonDoubletCount++;
            } else if (status.equals("error")) {
                errorCount++;
            }
        }
    }

    /**
     * Validates the input of the given combobox values. 
     * @return the error code
     */
    private int validateInput() {
        if (nonDoubletCount > 1 && doubletCount > 0)
            return 1;
        if (doubletCount > 0 && nonDoubletCount == 0)
            return 2;
        return -1;
    }

    /**
     * Updates the redundancy-{type}.xml files with the given combobox values.
     */
    private void updateXML(MCRServletJob job, MCRSession session, Element redObjectsElement) {
        boolean closed = true;
        int count = 1;
        Filter elementAndObjectFilter = new ElementFilter("object");
        for (Object o : redObjectsElement.getContent(elementAndObjectFilter)) {
            Element objectElement = (Element) o;
            String objectId = "object-id_" + count;
            String selection = job.getRequest().getParameter("selection_" + count);

            if (selection == null || selection.equals("")) {
                closed = false;
            }
            // edit doublet entry
            objectElement.setAttribute("status", selection);
            
            count++;
        }
        // add some general infos to the redObjectsElements
        String user = session.getCurrentUserID();
        String userRealName = MCRUserMgr.instance().retrieveUser(user).getUserContact().getFirstName() + " "
                + MCRUserMgr.instance().retrieveUser(user).getUserContact().getLastName();
        long time = System.currentTimeMillis();
        java.util.Date date = new java.util.Date(time);
        redObjectsElement.setAttribute("user", user);
        redObjectsElement.setAttribute("userRealName", userRealName);
        redObjectsElement.setAttribute("time", Long.toString(time));
        redObjectsElement.setAttribute("timePretty", date.toGMTString());
        if (closed)
            redObjectsElement.setAttribute("status", "closed");
        else
            redObjectsElement.removeAttribute("status");

        if (errorCount > 0)
            redObjectsElement.setAttribute("hasErrors", "true");
        else
            redObjectsElement.removeAttribute("hasErrors");
    }

    private synchronized void forwardToClient(MCRServletJob job, int maxObjects) throws IOException {
        String redunObject = job.getRequest().getParameter("redunObject").trim();
        String returnURL = getBaseURL(job);
        int nextNum = Integer.valueOf(redunObject) + 1;
        if (!(nextNum > maxObjects)) {
            // show next Element
            returnURL += "&XSL.redunObject=" + (Integer.valueOf(redunObject) + 1);
        }
        job.getResponse().sendRedirect(returnURL);
    }

    private synchronized void forwardExceptionToClient(MCRServletJob job, int id) throws IOException {
        String redunObject = job.getRequest().getParameter("redunObject").trim();
        String returnURL = getBaseURL(job);
        returnURL += "&XSL.redunObject=" + redunObject + "&XSL.exceptionId=" + id;
        job.getResponse().sendRedirect(returnURL);
    }

    private String getBaseURL(MCRServletJob job) {
        String redunMapURL = job.getRequest().getParameter("redunMap");
        String redunModeValue = job.getRequest().getParameter("redunMode");
        String redunMode = "XSL.redunMode.SESSION=" + redunModeValue;
        return MCRConfiguration.instance().getString("MCR.baseurl") + redunMapURL + "?" + redunMode;
    }
}