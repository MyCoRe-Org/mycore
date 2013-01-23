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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import org.apache.log4j.Logger;
import org.mycore.access.MCRAccessManager;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRException;
import org.mycore.common.MCRMailer;
import org.mycore.common.MCRUtils;
import org.mycore.datamodel.common.MCRActiveLinkException;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.xml.sax.SAXParseException;

/**
 * The servlet store the MCREditorServlet output XML in a file of a MCR type
 * dependencies directory, check it dependence of the MCR type and store the XML
 * in a file in this directory or if an error was occured start the editor again
 * with <b>todo </b> <em>repair</em>.
 * 
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 */
public class MCRCheckEditACLServlet extends MCRCheckACLBase {

    private static final long serialVersionUID = 1L;

    private static Logger LOGGER = Logger.getLogger(MCRCheckEditACLServlet.class);

    /**
     * The method return an URL with the next working step. If okay flag is
     * true, the object will present else it shows the error page.
     * 
     * @param ID
     *            the MCRObjectID of the MCRObject
     * @param okay
     *            the return value of the store operation
     * @return the next URL as String
     */
    protected String getNextURL(MCRObjectID ID, boolean okay) throws MCRActiveLinkException {
        StringBuilder sb = new StringBuilder();
        if (okay) {
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
        List<String> addr = WFM.getMailAddress(ID.getBase(), "weditacl");

        if (addr.size() == 0) {
            return;
        }

        String sender = WFM.getMailSender();
        String appl = MCRConfiguration.instance().getString("MCR.NameOfProject", "DocPortal");
        String subject = "Automatically generated message from " + appl;
        StringBuilder text = new StringBuilder();
        text.append("The ACL data of the MyCoRe object of type ").append(ID.getTypeId()).append(" with the ID ").append(ID.toString())
                .append(" in the workflow was changes.");
        LOGGER.info(text.toString());

        try {
            MCRMailer.send(sender, addr, subject, text.toString(), false);
        } catch (Exception ex) {
            LOGGER.error("Can't send a mail to " + addr);
        }
    }

    /**
     * The method store the incoming service data from the ACL editor to the
     * workflow.
     * 
     * @param outelm
     *            the service subelement of an MCRObject
     * @param job
     *            the MCRServletJob instance
     * @param ID
     *            the MCRObjectID
     * @throws SAXParseException 
     * @throws MCRException 
     */
    public final boolean storeService(org.jdom2.Element outelm, MCRServletJob job, MCRObjectID ID) throws MCRException, SAXParseException, IOException {
        File impex = new File(WFM.getDirectoryPath(ID.getBase()), ID.toString() + ".xml");
        MCRObject obj = new MCRObject(impex.toURI());
        obj.getService().setFromDOM(outelm);

        // Save the prepared MCRObject/MCRDerivate to a file
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(impex);
            out.write(MCRUtils.getByteArray(obj.createXML()));
            out.flush();
        } catch (IOException ex) {
            LOGGER.error(ex.getMessage());
            LOGGER.error("Exception while store to file " + impex);
            try {
                errorHandlerIO(job);
            } catch (Exception ioe) {
                ioe.printStackTrace();
            }

            return false;
        } finally {
            if (out != null)
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }

        LOGGER.info("Object " + ID.toString() + " stored under " + impex + ".");
        return true;
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
