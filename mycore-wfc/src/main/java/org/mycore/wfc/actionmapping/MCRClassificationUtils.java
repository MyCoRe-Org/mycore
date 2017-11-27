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

import java.util.Collection;

import org.mycore.datamodel.classifications2.MCRCategLinkReference;
import org.mycore.datamodel.classifications2.MCRCategLinkService;
import org.mycore.datamodel.classifications2.MCRCategLinkServiceFactory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.common.MCRLinkTableManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.wfc.MCRConstants;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRClassificationUtils {

    private static final MCRCategLinkService LINK_SERVICE = MCRCategLinkServiceFactory.getInstance();

    private static final MCRLinkTableManager LINK_TABLE = MCRLinkTableManager.instance();

    public static boolean isInCollection(String mcrId, String collection) {
        MCRObjectID mcrObjectID = MCRObjectID.getInstance(mcrId);
        MCRCategoryID collectionID = new MCRCategoryID(MCRConstants.COLLECTION_CLASS_ID.getRootID(), collection);
        MCRCategLinkReference reference = new MCRCategLinkReference(mcrObjectID);
        return LINK_SERVICE.isInCategory(reference, collectionID);
    }

    public static String getCollection(String mcrId) {
        MCRObjectID mcrObjectID = MCRObjectID.getInstance(mcrId);
        if (mcrObjectID.getTypeId().equals("derivate")) {
            return getCollectionFromDerivate(mcrObjectID);
        }
        return getCollectionFromObject(mcrObjectID);
    }

    private static String getCollectionFromObject(MCRObjectID mcrObjectID) {
        MCRCategLinkReference reference = new MCRCategLinkReference(mcrObjectID);
        return LINK_SERVICE.getLinksFromReference(reference)
            .stream()
            .filter(categID -> categID.getRootID()
                .equals(MCRConstants.COLLECTION_CLASS_ID.getRootID()))
            .findFirst()
            .map(MCRCategoryID::getID)
            .orElse(null);
    }

    private static String getCollectionFromDerivate(MCRObjectID mcrObjectID) {
        Collection<String> sourceOf = LINK_TABLE.getSourceOf(mcrObjectID, MCRLinkTableManager.ENTRY_TYPE_DERIVATE);
        if (sourceOf.isEmpty()) {
            return null;
        }
        MCRObjectID metaObjectID = MCRObjectID.getInstance(sourceOf.iterator().next());
        return getCollectionFromObject(metaObjectID);
    }
}
