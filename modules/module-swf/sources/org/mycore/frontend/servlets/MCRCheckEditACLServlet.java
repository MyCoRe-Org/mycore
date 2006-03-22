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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import org.mycore.common.MCRMailer;
import org.mycore.common.MCRUtils;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.metadata.MCRObjectService;
import org.mycore.frontend.workflow.MCRSimpleWorkflowManager;

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

    /**
     * The method is a dummy and return an URL with the next working step.
     * 
     * @param ID
     *            the MCRObjectID of the MCRObject
     * @return the next URL as String
     */
    public final String getNextURL(MCRObjectID ID) {
        // return all is ready
        StringBuffer sb = new StringBuffer();
        sb.append(CONFIG.getString("MCR.editor_page_dir", "")).append("editor_").append(ID.getTypeId()).append("_editor.xml");

        return sb.toString();
    }

    /**
     * The method send a message to the mail address for the MCRObjectType.
     * 
     * @param ID
     *            the MCRObjectID of the MCRObject
     */
    public final void sendMail(MCRObjectID ID) {
        List addr = WFM.getMailAddress(ID.getTypeId(), "weditacl");

        if (addr.size() == 0) {
            return;
        }

        String sender = WFM.getMailSender();
        String appl = CONFIG.getString("MCR.editor_mail_application_id", "DocPortal");
        String subject = "Automaticaly message from " + appl;
        StringBuffer text = new StringBuffer();
        text.append("The ACL data of the MyCoRe object of type ").append(ID.getTypeId()).append(" with the ID ").append(ID.getId()).append(" in the workflow was changes.");
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
     */
    public final void storeService(org.jdom.Element outelm, MCRServletJob job, MCRObjectID ID) {
        String fn = WFM.getDirectoryPath(ID.getTypeId()) + File.separator + ID.getId() + ".xml";
        MCRObject obj = new MCRObject();
        obj.setFromURI(fn);
        MCRObjectService service = new MCRObjectService();
        service.setFromDOM(outelm);
        obj.setService(service);

        // Save the prepared MCRObject/MCRDerivate to a file
        try {
            FileOutputStream out = new FileOutputStream(fn);
            out.write(MCRUtils.getByteArray(obj.createXML()));
            out.flush();
        } catch (IOException ex) {
            LOGGER.error(ex.getMessage());
            LOGGER.error("Exception while store to file " + fn);
            try {
                errorHandlerIO(job);
            } catch (Exception ioe) {
                ioe.printStackTrace();
            }

            return;
        }

        LOGGER.info("Object " + ID.getId() + " stored under " + fn + ".");
    }
}
