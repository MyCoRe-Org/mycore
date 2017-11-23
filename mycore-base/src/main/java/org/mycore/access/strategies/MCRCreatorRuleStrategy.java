/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mycore.access.strategies;

import java.util.concurrent.ExecutionException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.access.MCRAccessManager;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRUserInformation;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.datamodel.classifications2.MCRCategLinkReference;
import org.mycore.datamodel.classifications2.MCRCategLinkService;
import org.mycore.datamodel.classifications2.MCRCategLinkServiceFactory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.common.MCRCreatorCache;
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
 * MCR.Access.Strategy.SubmittedCategory=state:submitted
 *
 * You can also specify a comma separated list of categories like: <code>state:submitted,state:new</code>
 *
 * @author Thomas Scheffler (yagee)
 * @author Kathleen Neumann (mcrkrebs)
 *
 * @version $Revision$ $Date$
 */
public class MCRCreatorRuleStrategy implements MCRCombineableAccessCheckStrategy {
    private static final Logger LOGGER = LogManager.getLogger(MCRCreatorRuleStrategy.class);

    private static final String SUBMITTED_CATEGORY = MCRConfiguration.instance()
        .getString("MCR.Access.Strategy.SubmittedCategory", "state:submitted");

    private static final String CREATOR_ROLE = MCRConfiguration.instance().getString("MCR.Access.Strategy.CreatorRole",
        "submitter");

    private static final MCRCategLinkService LINK_SERVICE = MCRCategLinkServiceFactory.getInstance();

    private static final MCRObjectTypeStrategy BASE_STRATEGY = new MCRObjectTypeStrategy();

    /*
     * (non-Javadoc)
     *
     * @see org.mycore.access.strategies.MCRAccessCheckStrategy#checkPermission(java.lang.String,
     *      java.lang.String)
     */
    public boolean checkPermission(String id, String permission) {
        LOGGER.debug("check permission {} for MCRBaseID {}", permission, id);
        if (id == null || id.length() == 0 || permission == null || permission.length() == 0) {
            return false;
        }
        //our decoration for write permission
        return BASE_STRATEGY.checkPermission(id, permission) || isCreatorRuleAvailable(id, permission);
    }

    private static boolean objectStatusIsSubmitted(MCRObjectID mcrObjectID) {
        MCRCategLinkReference reference = new MCRCategLinkReference(mcrObjectID);
        boolean isSubmitted = false;
        if (SUBMITTED_CATEGORY == null) {
            return false;
        }
        String[] submittedCategoriesSplitted = SUBMITTED_CATEGORY.split(",");
        for (String submittedCategoryID : submittedCategoriesSplitted) {
            String categoryId = submittedCategoryID.trim();
            MCRCategoryID submittedCategory = MCRCategoryID.fromString(categoryId);
            if (LINK_SERVICE.isInCategory(reference, submittedCategory)) {
                isSubmitted = true;
            }
        }
        return isSubmitted;
    }

    private static boolean isCurrentUserCreator(MCRObjectID mcrObjectID, MCRUserInformation currentUser) {
        try {
            String creator = MCRCreatorCache.getCreator(mcrObjectID);
            return currentUser.getUserID().equals(creator);
        } catch (ExecutionException e) {
            LOGGER.error("Error while getting creator information.", e);
            return false;
        }
    }

    @Override
    public boolean hasRuleMapping(String id, String permission) {
        return BASE_STRATEGY.hasRuleMapping(id, permission) || isCreatorRuleAvailable(id, permission);
    }

    public boolean isCreatorRuleAvailable(String id, String permission) {
        if (MCRAccessManager.PERMISSION_WRITE.equals(permission)) {
            MCRObjectID mcrObjectId = null;
            try {
                mcrObjectId = MCRObjectID.getInstance(id);
                MCRUserInformation currentUser = MCRSessionMgr.getCurrentSession().getUserInformation();
                if (currentUser.isUserInRole(CREATOR_ROLE) && objectStatusIsSubmitted(mcrObjectId)) {
                    if (isCurrentUserCreator(mcrObjectId, currentUser)) {
                        return true;
                    }
                }
            } catch (RuntimeException e) {
                if (mcrObjectId == null) {
                    LOGGER.debug("id is not a valid object ID", e);
                } else {
                    LOGGER.warn("Eror while checking permission.", e);
                }
            }
        }
        return false;
    }
}
