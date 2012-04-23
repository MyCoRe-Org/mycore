/*
 * $Id$
 * $Revision: 5697 $ $Date: 20.04.2012 $
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

package org.mycore.wfc.actionmapping;

import java.text.MessageFormat;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.mycore.common.MCRException;
import org.mycore.datamodel.classifications2.MCRCategLinkReference;
import org.mycore.datamodel.metadata.MCRObjectID;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public final class MCRURLRetriever {
    private MCRURLRetriever() {
    };

    private static final Logger LOGGER = Logger.getLogger(MCRURLRetriever.class);

    private static MCRActionMappings ACTION_MAPPINGS = initActionsMappings();

    private static HashMap<String, MCRCollection> COLLECTION_MAP;

    private static MCRActionMappings initActionsMappings() {
        try {
            MCRActionMappings actionMappings = MCRActionMappingsManager.getActionMappings();
            HashMap<String, MCRCollection> collectionMap = new HashMap<String, MCRCollection>();
            for (MCRCollection collection : actionMappings.getCollections()) {
                collectionMap.put(collection.getName(), collection);
            }
            COLLECTION_MAP = collectionMap;
            return actionMappings;
        } catch (Exception e) {
            throw new MCRException(e);
        }
    }

    public static String getURLforID(String action, String mcrID) {
        final MCRObjectID objID = MCRObjectID.getInstance(mcrID);
        String collection = MCRClassificationUtils.getCollection(mcrID);
        WorkflowDataProvider wfDataProvider = new WorkflowDataProvider() {
            @Override
            public MCRWorkflowData getWorkflowData() {
                MCRWorkflowData workflowData = new MCRWorkflowData();
                MCRCategLinkReference categoryReference = new MCRCategLinkReference(objID);
                workflowData.setCategoryReference(categoryReference);
                return workflowData;
            }
        };
        return getURL(action, collection, wfDataProvider);
    }

    public static String getURLforCollection(String action, String collection) {
        WorkflowDataProvider wfDataProvider = new WorkflowDataProvider() {
            @Override
            public MCRWorkflowData getWorkflowData() {
                MCRWorkflowData workflowData = new MCRWorkflowData();
                return workflowData;
            }
        };
        return getURL(action, collection, wfDataProvider);
    }

    public static String getURL(String action, String collectionName, WorkflowDataProvider wfDataProvider) {
        MCRCollection collection = COLLECTION_MAP.get(collectionName);
        if (collection == null) {
            LOGGER.warn("Could not find collection: " + collectionName);
            return null;
        }
        for (MCRAction act : collection.getActions()) {
            if (act.getAction().equals(action)) {
                MCRWorkflowData workflowData = wfDataProvider.getWorkflowData();
                if (LOGGER.isDebugEnabled()) {
                    MCRCategLinkReference categoryReference = workflowData.getCategoryReference();
                    String mcrId = categoryReference == null ? null : categoryReference.getObjectID();
                    LOGGER.debug(MessageFormat.format("Collection: {0}, Action: {1}, Object: {2}", collection, action, mcrId));
                }
                return act.getURL(workflowData);
            }
        }
        LOGGER.warn(MessageFormat.format("Could not find action ''{0}'' in collection: {1}", action, collectionName));
        return null;
    }

    private static interface WorkflowDataProvider {
        public MCRWorkflowData getWorkflowData();
    }
}
