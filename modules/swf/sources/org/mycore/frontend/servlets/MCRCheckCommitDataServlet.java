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

import java.util.List;

import org.mycore.common.MCRMailer;
import org.mycore.datamodel.common.MCRActiveLinkException;
import org.mycore.datamodel.metadata.MCRObjectID;
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
public class MCRCheckCommitDataServlet extends MCRCheckDataBase {

    private static final long serialVersionUID = 1L;

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
        // commit to the server
        MCRSimpleWorkflowManager wfm = MCRSimpleWorkflowManager.instance();
        okay = wfm.commitMetadataObject(ID.getTypeId(), ID.getId());

        StringBuffer sb = new StringBuffer();
        if (okay) {
            // then delete the data
            wfm.deleteMetadataObject(ID.getTypeId(), ID.getId());
            // return all is ready
            sb.append("receive/").append(ID.getId());
        } else {
            sb.append(CONFIG.getString("MCR.SWF.PageDir", "")).append(CONFIG.getString("MCR.SWF.PageErrorStore", "editor_error_store.xml"));
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
        MCRSimpleWorkflowManager wfm = MCRSimpleWorkflowManager.instance();
        List addr = wfm.getMailAddress(ID.getTypeId(), "wcommit");

        if (addr.size() == 0) {
            return;
        }

        String sender = wfm.getMailSender();
        String appl = CONFIG.getString("MCR.SWF.Mail.ApplicationID", "DocPortal");
        String subject = "Automatically generated message from " + appl;
        StringBuffer text = new StringBuffer();
        text.append("An Object with type ").append(ID.getTypeId()).append(" and ID ").append(ID.getId()).append(" was stored in the system.");
        LOGGER.info(text.toString());

        try {
            MCRMailer.send(sender, addr, subject, text.toString(), false);
        } catch (Exception ex) {
            LOGGER.error("Can't send a mail to " + addr);
        }
    }
}
