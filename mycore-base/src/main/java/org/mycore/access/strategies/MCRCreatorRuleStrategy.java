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

package org.mycore.access.strategies;

import java.util.List;

import org.apache.log4j.Logger;
import org.mycore.access.MCRAccessManager;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRUserInformation;
import org.mycore.datamodel.classifications2.MCRCategLinkReference;
import org.mycore.datamodel.classifications2.MCRCategLinkService;
import org.mycore.datamodel.classifications2.MCRCategLinkServiceFactory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.common.MCRXMLMetadataManager;
import org.mycore.datamodel.ifs2.MCRMetadataVersion;
import org.mycore.datamodel.metadata.MCRObjectID;

/**
 *
 * First a check is done if user is in role "submitter", the given object is in
 * status "submitted" and current user is creator. If not
 * it will be tried to check the permission against the rule ID
 * <code>default_&lt;ObjectType&gt;</code> if it exists. If not the last
 * fallback is done against <code>default</code>.
 *
 * Specify classification and category for status "submitted":
 * MCR.Access.Strategy.SubmittedCategory=status:submitted
 *
 * @author Thomas Scheffler (yagee)
 * @author Kathleen Neumann (mcrkrebs)
 *
 * @version $Revision$ $Date$
 */
public class MCRCreatorRuleStrategy implements MCRAccessCheckStrategy {
    private static final Logger LOGGER = Logger.getLogger(MCRCreatorRuleStrategy.class);

    private static final String SUBMITTED_CATEGORY = MCRConfiguration.instance().getString(
            "MCR.Access.Strategy.SubmittedCategory", "status:submitted");

    private static final MCRCategLinkService LINK_SERVICE = MCRCategLinkServiceFactory.getInstance();


    /*
     * (non-Javadoc)
     *
     * @see org.mycore.access.strategies.MCRAccessCheckStrategy#checkPermission(java.lang.String,
     *      java.lang.String)
     */
    public boolean checkPermission(String id, String permission) {
        LOGGER.debug("check permission " + permission + " for MCRBaseID " + id);
        if (id == null || id.length() == 0 || permission == null || permission.length() == 0)
            return false;
        if (MCRAccessManager.getAccessImpl().hasRule(id, permission)) {
            LOGGER.debug("using access rule defined for object.");
            return MCRAccessManager.getAccessImpl().checkPermission(id, permission);
        }
        MCRUserInformation currentUser = MCRSessionMgr.getCurrentSession().getUserInformation();
        if (currentUser.isUserInRole("submitter") && objectStatusIsSubmitted(id)) {
            return isCurrentUserCreator(id, currentUser);
        }
        return MCRObjectTypeStrategy.checkObjectTypePermission(id, permission);
    }

    private static boolean objectStatusIsSubmitted(String id) {
        MCRObjectID mcrObjectID = MCRObjectID.getInstance(id);
        MCRCategLinkReference reference = new MCRCategLinkReference(mcrObjectID);
        MCRCategoryID submittedID;
        try {
            submittedID = MCRCategoryID.fromString(SUBMITTED_CATEGORY);
        } catch (Exception e) {
            LOGGER.debug("Category '" + SUBMITTED_CATEGORY + "' is not a valid category id.");
            return false;
        }
        return LINK_SERVICE.isInCategory(reference, submittedID);
    }

    private static boolean isCurrentUserCreator(String id, MCRUserInformation currentUser) {
        MCRObjectID mcrObjectID = MCRObjectID.getInstance(id);
        MCRXMLMetadataManager metadataManager = MCRXMLMetadataManager.instance();
        try {
            List<MCRMetadataVersion> versions = metadataManager.listRevisions(mcrObjectID);
            if (versions != null && !versions.isEmpty()) {
                LOGGER.debug("check if current user " + currentUser.getUserID() + " is equals " + versions.get(0).getUser());
                return currentUser.getUserID().equals(versions.get(0).getUser());
            } else {
                LOGGER.debug("Could not get creator information.");
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }
}
