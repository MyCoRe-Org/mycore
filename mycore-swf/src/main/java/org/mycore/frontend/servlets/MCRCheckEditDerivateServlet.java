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

import static org.jdom2.Namespace.XML_NAMESPACE;
import static org.mycore.common.MCRConstants.XLINK_NAMESPACE;
import static org.mycore.common.MCRConstants.XSI_NAMESPACE;

import java.io.File;
import java.util.List;

import org.apache.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.xpath.XPath;
import org.mycore.access.MCRAccessManager;
import org.mycore.common.MCRMailer;
import org.mycore.common.MCRUtils;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.datamodel.common.MCRActiveLinkException;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.frontend.MCRFrontendUtil;

/**
 * The servlet store the MCREditorServlet output XML in a file of a MCR type
 * dependencies directory, check it dependence of the MCR type and store the XML
 * in a file in this directory or if an error was occurred start the editor again
 * with <b>todo </b> <em>repair</em>.
 * 
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 */
public class MCRCheckEditDerivateServlet extends MCRCheckBase {

    private static final long serialVersionUID = 1L;
    private static Logger LOGGER = Logger.getLogger(MCRCheckEditDerivateServlet.class);

    /**
     * This method overrides doGetPost of MCRServlet. <br />
     */
    public void doGetPost(MCRServletJob job) throws Exception {
        // read the XML data
        Document indoc = readEditorOutput(job);

        // create a metadata object and prepare it
        MCRObjectID derID = null;
        boolean okay = false;
        String url = getNextURL(derID, okay);
        try {
            Element root = indoc.getRootElement();
            derID = MCRObjectID.getInstance(root.getAttributeValue("ID"));
            root.setAttribute("noNamespaceSchemaLocation", "datamodel-derivate.xsd", XSI_NAMESPACE);
            root.addNamespaceDeclaration(XLINK_NAMESPACE);
            root.addNamespaceDeclaration(XSI_NAMESPACE);
            XPath titles = XPath.newInstance("/mycorederivate/derivate/titles/title");
            for (Object node : titles.selectNodes(indoc)) {
                Element e = (Element) node;
                if (e.getAttribute("lang") != null) {
                    e.getAttribute("lang").setNamespace(XML_NAMESPACE);
                }
            }
            XPath linkmetas = XPath.newInstance("/mycorederivate/derivate/linkmetas/linkmeta");
            for (Object node : linkmetas.selectNodes(indoc)) {
                Element e = (Element) node;
                if (e.getAttribute("href") != null) {
                    e.getAttribute("href").setNamespace(XLINK_NAMESPACE);
                }
                if (e.getAttribute("title") != null) {
                    e.getAttribute("title").setNamespace(XLINK_NAMESPACE);
                }
                if (e.getAttribute("type") != null) {
                    e.getAttribute("type").setNamespace(XLINK_NAMESPACE);
                }
            }
            if (LOGGER.isDebugEnabled()) {
                MCRUtils.writeJDOMToSysout(indoc);
            }
            byte[] xml = MCRUtils.getByteArray(indoc);

            // read data
            Element e = (Element) linkmetas.selectSingleNode(indoc);
            @SuppressWarnings("unused")
            MCRDerivate der = new MCRDerivate(xml, true);
            MCRObjectID objID = MCRObjectID.getInstance(e.getAttributeValue("href", XLINK_NAMESPACE));

            // check access
            if (!checkAccess(objID)) {
                job.getResponse().sendRedirect(MCRFrontendUtil.getBaseURL() + usererrorpage);
                return;
            }

            // update data
            StringBuilder sb = new StringBuilder();
            sb.append(WFM.getDirectoryPath(derID.getBase())).append(File.separator).append(derID.toString()).append(".xml");
            MCRUtils.writeJDOMToFile(indoc, new File(sb.toString()));
            okay = true;
            url = getNextURL(objID, okay);
            sendMail(derID);
        } catch (Exception e) {
            e.printStackTrace();
        }
        // return
        job.getResponse().sendRedirect(job.getResponse().encodeRedirectURL(MCRFrontendUtil.getBaseURL() + url));
    }

    /**
     * The method is a dummy and return an URL with the next working step.
     * 
     * @param ID
     *            the MCRObjectID of the MCRObject
     * @param okay
     *            the return value of the store operation
     * @return the next URL as String
     */
    public final String getNextURL(MCRObjectID ID, boolean okay) throws MCRActiveLinkException {
        StringBuilder sb = new StringBuilder();
        if (okay) {
            // return all is ready
            sb.append(WFM.getWorkflowFile(getServletContext(), pagedir, ID.getBase()));
        } else {
            sb.append(pagedir).append(MCRConfiguration.instance().getString("MCR.SWF.PageErrorStore", "editor_error_store.xml"));
        }
        return sb.toString();
    }

    /**
     * The method send a message to the mail address for the MCRObjectType.
     * 
     * @param ID
     *            the MCRObjectID of the MCRObject
     */
    public final void sendMail(MCRObjectID ID) {
        List<String> addr = WFM.getMailAddress(ID.getTypeId(), "weditder");
        if (addr.size() == 0) {
            return;
        }
        String sender = WFM.getMailSender();
        String appl = MCRConfiguration.instance().getString("MCR.NameOfProject", "MyCoRe");
        String subject = "Automatically generated message from " + appl;
        StringBuilder text = new StringBuilder();
        text.append("The title of the derivate with the ID ").append(ID.toString()).append(" was changed in the workflow.");
        LOGGER.info(text.toString());
        try {
            MCRMailer.send(sender, addr, subject, text.toString(), false);
        } catch (Exception ex) {
            LOGGER.error("Can't send a mail to " + addr);
        }
    }

    /**
     * check the access permission
     * @param ID the mycore ID
     * @return true if the access is set
     */
    protected boolean checkAccess(MCRObjectID ID) {
        if (MCRAccessManager.checkPermission("create-" + ID.getBase())) {
            return true;
        }
        return MCRAccessManager.checkPermission("create-" + ID.getTypeId());
    }

}
