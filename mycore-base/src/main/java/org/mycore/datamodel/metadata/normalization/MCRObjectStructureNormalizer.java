/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
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

package org.mycore.datamodel.metadata.normalization;

import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.metadata.MCRObjectStructure;

/**
 * This normalizer normalizes the structure of an MCRObject.
 * It removes child elements from the structure because they are already present in the child elements.
 * The information can be retained from the Database using
 * {@link org.mycore.datamodel.metadata.MCRMetadataManager#getDerivateIds(MCRObjectID)},
 * {@link org.mycore.datamodel.metadata.MCRMetadataManager#getChildren(MCRObjectID)} or
 * {@link org.mycore.common.MCRExpandedObjectManager#getExpandedObject(MCRObject)}.
 */
public class MCRObjectStructureNormalizer extends MCRObjectNormalizer {

    @Override
    public void normalize(MCRObject mcrObject) {
        MCRObjectStructure normalizedStructure = new MCRObjectStructure();
        normalizedStructure.setFromDOM(mcrObject.getStructure().createXML());
        mcrObject.getStructure().clear();
        mcrObject.getStructure().setFromDOM(normalizedStructure.createXML());
    }

}
