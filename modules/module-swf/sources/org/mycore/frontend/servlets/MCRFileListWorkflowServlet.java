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

// package
package org.mycore.frontend.servlets;

import java.io.File;
import java.util.ArrayList;

import javax.servlet.RequestDispatcher;

import org.apache.log4j.Logger;
import org.jdom.Namespace;
import org.mycore.common.MCRDefaults;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.xml.MCRXMLContainer;

/**
 * This servlet create a MyCoRe classification JDOM tree based of a content in a
 * directory selected by the <em>type</em> string in the file name and push
 * them to the MCRLayoutServlet. To select the directory it needs a property
 * value named <b>MCR.editor_ <em>type</em> _directory </b>. The type is a
 * part of the MCRObjectID of the datasets. <br />
 * The categories are linear. Each category ID is the file name and the text
 * attribute is the file name without the extention <em>.xml</em>.<br />
 * Only users with the privileg <b>update object in datastore </b> get the file
 * list. For others the classification is empty. <br />
 * <br />
 * Call this servlet with
 * <b>.../servlets/MCRFileListWorkflowServlet/XSL.Style=xml&type=... </b>
 * 
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 */
public class MCRFileListWorkflowServlet extends MCRServlet {
    private static Logger LOGGER = Logger.getLogger(MCRFileListWorkflowServlet.class.getName());

    /**
     * This method overrides doGetPost of MCRServlet and put the generated DOM
     * in the LayoutServlet.
     */
    public void doGetPost(MCRServletJob job) throws Exception {
        // get the type
        String type = getProperty(job.getRequest(), "type").trim();
        LOGGER.debug("MCRFileListWorkflowServlet : type = " + type);

        // check the privileg
        boolean haspriv = false;
        MCRSession mcrSession = MCRSessionMgr.getCurrentSession();
        String userid = mcrSession.getCurrentUserID();


/*
        if (privs.contains("modify-" + type)) {
            haspriv = true;
        }
*/

        // read directory
        String dirname = CONFIG.getString("MCR.editor_" + type + "_directory", null);
        ArrayList workfiles = new ArrayList();

        if ((dirname != null) && haspriv) {
            File dir = new File(dirname);
            String[] dirl = null;

            if (dir.isDirectory()) {
                dirl = dir.list();
            }

            if (dirl != null) {
                for (int i = 0; i < dirl.length; i++) {
                    LOGGER.debug(dirl[i]);

                    if (dirl[i].indexOf(type) != -1) {
                        workfiles.add(dirl[i]);
                    }
                }
            }
        }

        java.util.Collections.sort(workfiles);

        // create a classification
        String cid = "DocPortalWorkflow_class_00000";
        org.jdom.Element elm = new org.jdom.Element("mycoreclass");
        elm.addNamespaceDeclaration(org.jdom.Namespace.getNamespace("xsi", MCRDefaults.XSI_URL));

        String mcr_schema_path = "MCRClassification.xsd";
        elm.setAttribute("noNamespaceSchemaLocation", mcr_schema_path, org.jdom.Namespace.getNamespace("xsi", MCRDefaults.XSI_URL));
        elm.setAttribute("ID", cid);

        org.jdom.Element rootlabel = new org.jdom.Element("label");
        rootlabel.setAttribute("lang", "en", Namespace.XML_NAMESPACE);
        rootlabel.setAttribute("text", "Papyri Workflow Files");
        rootlabel.setAttribute("description", "");
        elm.addContent(rootlabel);

        org.jdom.Element categories = new org.jdom.Element("categories");

        for (int i = 0; i < workfiles.size(); i++) {
            String fname = (String) workfiles.get(i);
            String objid = fname.substring(0, fname.length() - 4);
            if(AI.checkPermission(objid, "writewf")) {
                org.jdom.Element category = new org.jdom.Element("category");
                category.setAttribute("ID", objid);

                org.jdom.Element label = new org.jdom.Element("label");
                label.setAttribute("lang", "en", Namespace.XML_NAMESPACE);
                label.setAttribute("text", objid);
                label.setAttribute("description", "");
                category.addContent(label);
                categories.addContent(category);            	
            }
        }

        elm.addContent(categories);

        // put it in a result container
        MCRXMLContainer res = new MCRXMLContainer();
        res.add("local", cid, 0, elm);

        // return the JDOM
        job.getRequest().setAttribute("MCRLayoutServlet.Input.JDOM", res.exportAllToDocument());

        RequestDispatcher rd = getServletContext().getNamedDispatcher("MCRLayoutServlet");
        rd.forward(job.getRequest(), job.getResponse());
    }
}
