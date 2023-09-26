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

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRUserInformation;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.datamodel.classifications2.MCRCategLinkReference;
import org.mycore.datamodel.classifications2.MCRCategLinkService;
import org.mycore.datamodel.classifications2.MCRCategLinkServiceFactory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.common.MCRCreatorCache;
import org.mycore.datamodel.metadata.MCRObjectID;

/**
 *
 * First, a check is done if the user is in the submitter role, if the user is the creator of the object and if the
 * creator is permitted to perform the requested action in the object's current state (two groups of states, submitted,
 * and review, are checked). If not it will be tried to check the permission against the rule ID
 * <code>default_&lt;ObjectType&gt;</code> if it exists. If not the last
 * fallback is done against <code>default</code>.
 *
 * Specify classification and category for submitted and review states:
 * MCR.Access.Strategy.SubmittedCategories=state:submitted
 * MCR.Access.Strategy.ReviewCategories=state:review
 *
 * Specify permissions for submitted and review states:
 * MCR.Access.Strategy.CreatorSubmittedPermissions=writedb,deletedb
 * MCR.Access.Strategy.CreatorReviewPermissions=read
 *
 * You can also specify a comma separated list of categories like: <code>state:submitted,state:new</code>
 *
 * @author Thomas Scheffler (yagee)
 * @author Kathleen Neumann (mcrkrebs)
 *
 */
public class MCRCreatorRuleStrategy implements MCRCombineableAccessCheckStrategy {
    private static final Logger LOGGER = LogManager.getLogger(MCRCreatorRuleStrategy.class);

    private static final List<String> SUBMITTED_CATEGORY_IDS = MCRConfiguration2
        .getString("MCR.Access.Strategy.SubmittedCategories")
        .map(MCRConfiguration2::splitValue)
        .map(s -> s.collect(Collectors.toList()))
        .orElse(List.of("state:submitted"));

    private static final List<String> REVIEW_CATEGORY_IDS = MCRConfiguration2
        .getString("MCR.Access.Strategy.ReviewCategories")
        .map(MCRConfiguration2::splitValue)
        .map(s -> s.collect(Collectors.toList()))
        .orElse(List.of("state:review"));

    private static final List<String> CREATOR_ROLES = MCRConfiguration2
        .getOrThrow("MCR.Access.Strategy.CreatorRole", MCRConfiguration2::splitValue)
        .collect(Collectors.toList());

    private static final List<String> CREATOR_SUBMITTED_PERMISSIONS = MCRConfiguration2
        .getOrThrow("MCR.Access.Strategy.CreatorSubmittedPermissions", MCRConfiguration2::splitValue)
        .collect(Collectors.toList());

    private static final List<String> CREATOR_REVIEW_PERMISSIONS = MCRConfiguration2
        .getOrThrow("MCR.Access.Strategy.CreatorReviewPermissions", MCRConfiguration2::splitValue)
        .collect(Collectors.toList());

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
        return objectStatusIsAnyOf(mcrObjectID, SUBMITTED_CATEGORY_IDS);
    }

    private static boolean objectStatusIsReview(MCRObjectID mcrObjectID) {
        return objectStatusIsAnyOf(mcrObjectID, REVIEW_CATEGORY_IDS);
    }

    private static boolean objectStatusIsAnyOf(MCRObjectID mcrObjectID, List<String> categoryIds) {
        MCRCategLinkReference reference = new MCRCategLinkReference(mcrObjectID);
        if (categoryIds == null) {
            return false;
        }
        for (String categoryId : categoryIds) {
            MCRCategoryID category = MCRCategoryID.fromString(categoryId);
            if (LINK_SERVICE.isInCategory(reference, category)) {
                return true;
            }
        }
        return false;
    }

    private static boolean userHasCreatorRole(MCRUserInformation currentUser) {
        return CREATOR_ROLES.stream().anyMatch(currentUser::isUserInRole);
    }

    private static boolean userIsCreatorOf(MCRUserInformation currentUser, MCRObjectID mcrObjectID) {
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
        if (!MCRObjectID.isValid(id)) {
            return false;
        }
        try {
            MCRObjectID mcrObjectId = MCRObjectID.getInstance(id);
            MCRUserInformation currentUser = MCRSessionMgr.getCurrentSession().getUserInformation();
            if (userHasCreatorRole(currentUser) && userIsCreatorOf(currentUser, mcrObjectId)) {
                if (objectStatusIsSubmitted(mcrObjectId) && CREATOR_SUBMITTED_PERMISSIONS.contains(permission)) {
                    return true;
                }
                if (objectStatusIsReview(mcrObjectId) && CREATOR_REVIEW_PERMISSIONS.contains(permission)) {
                    return true;
                }
            }
        } catch (RuntimeException e) {
            LOGGER.warn("Error while checking permission.", e);
        }
        return false;
    }
}
