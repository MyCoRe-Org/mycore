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
package org.mycore.frontend.idmapper;

import java.util.Optional;

import org.mycore.datamodel.metadata.MCRObjectID;

/**
 * Default implementation of an MCRIDMapper
 * 
 * It simply take the id string and tries to create the MCRObjectID from it.
 *  
 * @author Robert Stephan
 */
public class MCRDefaultIDMapper implements MCRIDMapper {

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<MCRObjectID> mapMCRObjectID(String mcrid) {
        if (MCRObjectID.isValid(mcrid)) {
            return Optional.of(MCRObjectID.getInstance(mcrid));
        }
        return Optional.empty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<MCRObjectID> mapMCRDerivateID(MCRObjectID mcrObjId, String derid) {
        if (MCRObjectID.isValid(derid)) {
            return Optional.of(MCRObjectID.getInstance(derid));
        }
        return Optional.empty();
    }

}
