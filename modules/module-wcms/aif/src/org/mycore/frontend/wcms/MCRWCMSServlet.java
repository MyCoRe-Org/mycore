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

package org.mycore.frontend.wcms;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.mycore.access.MCRAccessInterface;
import org.mycore.access.MCRAccessManager;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.frontend.MCRWebsiteWriteProtection;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;

/**
 * @author Andreas Trappe, Thomas Scheffler
 * 
 * Need to insert some things here
 * 
 */
public abstract class MCRWCMSServlet extends MCRServlet {
    protected static final String OUTPUT_ENCODING = "UTF-8";

    protected static final String VALIDATOR = "JTidy";

    protected static final String LOGINSERVLET_URL = getServletBaseURL() + "MCRLoginServlet";

    protected void doGetPost(MCRServletJob job) throws Exception {
        MCRWebsiteWriteProtection.verifyAccess(job.getRequest(), job.getResponse());
        if (accessGeneral()) {
            // set some global required params
            MCRSession session = MCRSessionMgr.getCurrentSession();
            session.put("status", "loggedIn");
            session.put("userID", session.getCurrentUserID());
            session.put("userRealName", session.getCurrentUserID());
            session.put("userClass", "admin");
            session.put("rootNodes", new ArrayList());
            // forward
            processRequest(job.getRequest(), job.getResponse());
        } else
            job.getResponse().sendRedirect(LOGINSERVLET_URL);
    }

    protected final boolean accessGeneral() {
        return (MCRWCMSUtilities.writeAccessGeneral() || hasRight2Manage());
    }

    private boolean hasRight2Manage() {
        return (MCRWCMSUtilities.manageReadAccess() || MCRWCMSUtilities.manageWCMSAccess());
    }

    public Element getTemplates() {
        Element templates = new Element("templates");

        // master
        File[] masterTemplates = new File(CONFIG.getString("MCR.templatePath") + "master/".replace('/', File.separatorChar)).listFiles();
        Element master = new Element("master");

        for (int i = 0; i < masterTemplates.length; i++) {
            if (masterTemplates[i].isDirectory() && (masterTemplates[i].getName().compareToIgnoreCase("cvs") != 0)) {
                master.addContent(new Element("template").setText(masterTemplates[i].getName()));
            }
        }

        // templates.addContent(content);
        templates.addContent(master);

        return templates;
    }

    final Document XMLFile2JDOM(String pathOfFile) throws IOException, JDOMException {
        File XMLFile = new File(pathOfFile);
        SAXBuilder builder = new SAXBuilder();
        Document doc = builder.build(XMLFile);

        return doc;
    }

    /*
     * final void WriteJDOM2XMLFile(Document doc, String pathOfFile) { }
     */
    protected abstract void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException;

    /**
     * Attaches on the given JDOM-Element an JDOM-Element with the configuration
     * of available multimedia objects (images, misc. documents).
     * 
     * @param root
     * @return
     */
    public Element getMultimediaConfig(Element root) {

        File[] imageList = null;
        File[] documentList = null;

        Element templates;
        templates = new Element("templates");

        Element images = new Element("images");
        root.addContent(images);

        File imagePath = new File((CONFIG.getString("MCR.WCMS.imagePath").replace('/', File.separatorChar)));

        if (!imagePath.exists()) {
            imagePath.mkdirs();
        }

        imageList = new File(imagePath.toString()).listFiles();

        for (int i = 0; i < imageList.length; i++) {
            if (!imageList[i].isDirectory()) {
                images.addContent(new Element("image").setText(imageList[i].getName()));
            }
        }

        root.addContent(new Element("imagePath").setText(CONFIG.getString("MCR.WCMS.imagePath").substring(
                        CONFIG.getString("MCR.WCMS.imagePath").lastIndexOf("webapps"))));

        Element documents = new Element("documents");
        root.addContent(documents);

        File documentPath = new File((CONFIG.getString("MCR.WCMS.documentPath").replace('/', File.separatorChar)));

        if (!documentPath.exists()) {
            documentPath.mkdirs();
        }

        documentList = new File(documentPath.toString()).listFiles();

        for (int i = 0; i < documentList.length; i++) {
            if (!documentList[i].isDirectory()) {
                documents.addContent(new Element("document").setText(documentList[i].getName()));
            }
        }

        root.addContent(new Element("documentPath").setText(CONFIG.getString("MCR.WCMS.documentPath").substring(
                        CONFIG.getString("MCR.WCMS.documentPath").lastIndexOf("webapps"))));
        return templates;
    }
}
