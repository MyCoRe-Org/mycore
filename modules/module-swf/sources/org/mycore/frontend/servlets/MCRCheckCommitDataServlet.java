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

import java.util.ArrayList;
import java.util.List;

import org.mycore.common.MCRMailer;
import org.mycore.datamodel.metadata.MCRActiveLinkException;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.frontend.workflow.MCRWorkflowManager;

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
	/**
	 * The method check the privileg of this action.
	 * 
	 * @param privs
	 *            the ArrayList of privilegs
	 * @return true if the privileg exist, else return false
	 */
	public final boolean hasPrivileg(ArrayList privs, String type) {
		if (!(privs.contains("modify-" + type) && privs.contains("commit-" + type))) {
			return false;
		}

		return true;
	}

	/**
	 * The method is a dummy and return an URL with the next working step.
	 * 
	 * @param ID
	 *            the MCRObjectID of the MCRObject
	 * @return the next URL as String
	 */
	public final String getNextURL(MCRObjectID ID) throws MCRActiveLinkException {
		// commit to the server
		MCRWorkflowManager wfm = MCRWorkflowManager.instance();
		boolean b = wfm.commitMetadataObject(ID.getTypeId(), ID.getId());

		if (b) {
			// then delete the data
			wfm.deleteMetadataObject(ID.getTypeId(), ID.getId());

			// return all is ready
			StringBuffer sb = (new StringBuffer("MCR.type_")).append(ID.getTypeId()).append("_in");
			String searchtype = CONFIG.getString(sb.toString(), ID.getTypeId());
			sb = new StringBuffer("servlets/MCRQueryServlet?mode=ObjectMetadata&type=");
			sb.append(searchtype).append("&hosts=local&query=%2Fmycoreobject[%40ID%3D\'").append(ID.getId()).append("\']");

			return sb.toString();
		}

		StringBuffer sb = new StringBuffer();
		sb.append(CONFIG.getString("MCR.editor_page_dir", "")).append(CONFIG.getString("MCR.editor_page_error_store", "editor_error_store.xml"));

		return sb.toString();
	}

	/**
	 * The method send a message to the mail address for the MCRObjectType.
	 * 
	 * @param ID
	 *            the MCRObjectID of the MCRObject
	 */
	public final void sendMail(MCRObjectID ID) {
		MCRWorkflowManager wfm = MCRWorkflowManager.instance();
		List addr = wfm.getMailAddress(ID.getTypeId(), "wcommit");

		if (addr.size() == 0) {
			return;
		}

		String sender = wfm.getMailSender();
		String appl = CONFIG.getString("MCR.editor_mail_application_id", "DocPortal");
		String subject = "Automaticaly message from " + appl;
		StringBuffer text = new StringBuffer();
		text.append("Es wurde ein Objekt vom Typ ").append(ID.getTypeId()).append(" mit der ID ").append(ID.getId()).append(
				" aus dem Workflow in das System geladen.");
		logger.info(text.toString());

		try {
			MCRMailer.send(sender, addr, subject, text.toString(), false);
		} catch (Exception ex) {
			logger.error("Can't send a mail to " + addr);
		}
	}
}
