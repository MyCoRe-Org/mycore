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

import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;

/**
 * This normalizer is used to assign a new object id to the MCRObject if the current id is 0.
 */
public class MCRObjectIDNormalizer extends MCRObjectNormalizer {

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public void normalize(MCRObject mcrObject) {
        MCRObjectID objectId = Objects.requireNonNull(mcrObject.getId(), "ObjectID must not be null");

        // assign new id if necessary
        if (objectId.getNumberAsInteger() == 0) {
            MCRObjectID oldId = objectId;
            objectId = MCRMetadataManager.getMCRObjectIDGenerator().getNextFreeId(objectId.getBase());
            mcrObject.setId(objectId);
            LOGGER.info("Assigned new object id {}", objectId);

            // if label was id with 00000000, set label to new id
            if (Objects.equals(mcrObject.getLabel(), oldId.toString())) {
                mcrObject.setLabel(objectId.toString());
            }
        }
    }
}
