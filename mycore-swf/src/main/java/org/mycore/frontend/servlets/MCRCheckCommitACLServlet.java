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
import static org.mycore.access.MCRAccessManager.PERMISSION_WRITE;

import java.util.List;

import org.apache.log4j.Logger;
import org.mycore.access.MCRAccessManager;
import org.mycore.common.MCRMailer;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.datamodel.common.MCRActiveLinkException;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.metadata.MCRObjectService;

/**
 * The servlet store the MCREditorServlet output XML in a file of a MCR type
 * dependencies directory, check it dependence of the MCR type and store the XML
 * in a file in this directory or if an error was occured start the editor again
 * with <b>todo </b> <em>repair</em>.
 * 
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 * @deprecated
 */
public class MCRCheckCommitACLServlet extends MCRCheckACLBase {

    private static final long serialVersionUID = 1L;
    private static Logger LOGGER = Logger.getLogger(MCRCheckCommitACLServlet.class);

    private static String storedrules = MCRConfiguration.instance().getString("MCR.Access.StorePermissions", "read,write,delete");

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
            if (ID.getTypeId().equals("class")) {
                sb.append("browse?mode=edit");
                return sb.toString();
            }
            if (ID.getTypeId().equals("derivate")) {
                MCRDerivate der = MCRMetadataManager.retrieveMCRDerivate(ID);
                String parent = der.getDerivate().getMetaLink().getXLinkHref();
                sb.append("receive/").append(parent);
                return sb.toString();
            }
            sb.append("receive/").append(ID.toString());
        } else {
            sb.append(MCRConfiguration.instance().getString("MCR.SWF.PageDir", "")).append(MCRConfiguration.instance().getString("MCR.SWF.PageErrorStore", "editor_error_store.xml"));
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
        List<String> addr = WFM.getMailAddress(ID.getTypeId(), "seditacl");

        if (addr.size() == 0) {
            return;
        }

        String sender = WFM.getMailSender();
        String appl = MCRConfiguration.instance().getString("MCR.NameOfProject", "DocPortal");
        String subject = "Automatically generated message from " + appl;
        StringBuilder text = new StringBuilder();
        text.append("The ACL data of the MyCoRe object of type ").append(ID.getTypeId()).append(" with the ID ").append(ID.toString()).append(" was changed in the server.");

        try {
            MCRMailer.send(sender, addr, subject, text.toString(), false);
            LOGGER.info("Send a mail about change ACLs to " + addr);
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
    public final boolean storeService(org.jdom2.Element outelm, MCRServletJob job, MCRObjectID ID) {

        MCRObjectService service = new MCRObjectService();
        service.setFromDOM(outelm);
        int rulesize = service.getRulesSize();
        if (rulesize == 0) {
            LOGGER.warn("The ACL conditions for this object was empty, no update!");
            return false;
        }
        while (0 < rulesize) {
            org.jdom2.Element conditions = service.getRule(0).getCondition();
            String permission = service.getRule(0).getPermission();
            if (storedrules.contains(permission)) {
                MCRAccessManager.updateRule(ID, permission, conditions, "");
            }
            service.removeRule(0);
            rulesize--;
        }

        LOGGER.info("Update ACLs for ID " + ID.toString() + "in server.");
        return true;
    }
    
    /**
     * check the access permission
     * @param ID the mycore ID
     * @return true if the access is set
     */
    protected boolean checkAccess(MCRObjectID ID) {
        return MCRAccessManager.checkPermission(ID, PERMISSION_WRITE);
    }

}
