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

package org.mycore.wfc.actionmapping;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRException;
import org.mycore.datamodel.classifications2.MCRCategLinkReference;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryDAO;
import org.mycore.datamodel.classifications2.MCRCategoryDAOFactory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.frontend.MCRFrontendUtil;
import org.mycore.wfc.MCRConstants;

/**
 * @author Thomas Scheffler (yagee)
 */
public final class MCRURLRetriever {
    private static final Logger LOGGER = LogManager.getLogger(MCRURLRetriever.class);

    private static final MCRCategoryDAO CATEGORY_DAO = MCRCategoryDAOFactory.getInstance();

    private static Map<String, MCRCollection> COLLECTION_MAP = initActionsMappings();

    private MCRURLRetriever() {
    }

    private static Map<String, MCRCollection> initActionsMappings() {
        try {
            MCRActionMappings actionMappings = MCRActionMappingsManager.getActionMappings();
            return Arrays
                .stream(actionMappings.getCollections())
                .collect(Collectors.toMap(MCRCollection::getName, c -> c));
        } catch (Exception e) {
            throw new MCRException(e);
        }
    }

    public static String getURLforID(String action, String mcrID, boolean absolute) {
        final MCRObjectID objID = MCRObjectID.getInstance(mcrID);
        String collectionName = MCRClassificationUtils.getCollection(mcrID);
        return getURL(action, collectionName, new MCRCategLinkReference(objID), absolute);
    }

    public static String getURLforCollection(String action, String collection, boolean absolute) {
        return getURL(action, collection, null, absolute);
    }

    private static String getURL(String action, String collectionName, MCRCategLinkReference reference,
        boolean absolute) {
        MCRCollection defaultCollection = reference != null ? getCollectionWithAction(reference.getType(), action, null)
            : null;
        MCRCollection collection = getCollectionWithAction(collectionName, action, defaultCollection);
        if (collection == null) {
            LOGGER.warn(MessageFormat
                .format("Could not find action ''{0}'' in collection: {1}", action, collectionName));
            return null;
        }
        return getURL(action, collection, reference, absolute);
    }

    private static String getURL(String action, MCRCollection collection, MCRCategLinkReference categoryReference,
        boolean absolute) {
        for (MCRAction act : collection.getActions()) {
            if (act.getAction().equals(action)) {
                if (LOGGER.isDebugEnabled()) {
                    String mcrId = categoryReference == null ? null : categoryReference.getObjectID();
                    LOGGER.debug(MessageFormat.format("Collection: {0}, Action: {1}, Object: {2}",
                        collection.getName(), action, mcrId));
                }
                String url = act.getURL(new MCRWorkflowData(categoryReference));
                if (absolute && url != null && url.startsWith("/")) {
                    url = MCRFrontendUtil.getBaseURL() + url.substring(1);
                }
                return url;
            }
        }
        return null;
    }

    private static MCRCollection getCollectionWithAction(String collection, String action,
        MCRCollection defaultCollection) {
        MCRCollection mcrCollection = COLLECTION_MAP.get(collection);
        if (mcrCollection != null) {
            Optional<MCRAction> firstAction = Arrays
                .stream(mcrCollection.getActions())
                .filter(a -> a.getAction().equals(action))
                .findFirst();
            if (firstAction.isPresent()) {
                return mcrCollection;
            }
        }
        //did not find a collection with that action, checking parent
        String parentCollection = getParentCollection(collection);
        String defaultCollectionName = defaultCollection == null ? null : defaultCollection.getName();
        if (parentCollection == null) {
            LOGGER.info("Using default collection '{}' for action: {}", defaultCollectionName, action);
            return defaultCollection;
        }
        LOGGER.info("Checking parent collection '{}' for action: {}", parentCollection, action);
        MCRCollection collectionWithAction = getCollectionWithAction(parentCollection, action, defaultCollection);
        if (collectionWithAction == null) {
            LOGGER.info("Using default collection '{}' for action: {}", defaultCollectionName, action);
            return defaultCollection;
        }
        if (mcrCollection == null) {
            mcrCollection = new MCRCollection();
            mcrCollection.setName(collection);
            mcrCollection.setActions();
        }
        for (MCRAction act : collectionWithAction.getActions()) {
            if (act.getAction().equals(action)) {
                int oldLength = mcrCollection.getActions().length;
                mcrCollection.setActions(Arrays.copyOf(mcrCollection.getActions(), oldLength + 1));
                mcrCollection.getActions()[oldLength] = act;
            }
        }
        //store in cache
        COLLECTION_MAP.put(collection, mcrCollection);
        return mcrCollection;
    }

    private static String getParentCollection(String collection) {
        MCRCategoryID categoryId = new MCRCategoryID(MCRConstants.COLLECTION_CLASS_ID.getRootID(), collection);
        List<MCRCategory> parents = CATEGORY_DAO.getParents(categoryId);
        if (parents == null || parents.isEmpty()) {
            return null;
        }
        return parents.iterator().next().getId().getID();
    }
}
