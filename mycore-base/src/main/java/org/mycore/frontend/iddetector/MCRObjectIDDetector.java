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
package org.mycore.frontend.iddetector;

import java.util.Optional;

import org.mycore.datamodel.metadata.MCRObjectID;

/**
 * Interface for an object id detector 
 * It is used in rest api and elsewhere to retrieve 
 * the MCRObjectID of an object or derivate for a given id string
 * 
 * This feature can be used to retrieve information on MyCoRe objects
 * by their alternative identifiers like DOI or URN.
 *
 * @author Robert Stephan
 *
 */
public interface MCRObjectIDDetector {
    String MCR_PROPERTY_CLASS = "MCR.Object.IDDetector.Class";

    /**
     * Detection of the MyCoRe object id  
     * @param mcrid - the id string that should be evaluated
     * @return a MCRObjectID instance for the object id
     */
    Optional<MCRObjectID> detectMCRObjectID(String mcrid);

    /**
     * Detection of the MyCoRe derivate id
     * @param mcrObjId - the MCRObjectID of the MyCoRe object to which the derivate should belong to  
     * @param derid - the id string that should be evaluated
     * @return a MCRObjectID instance for the derivate id
     */
    Optional<MCRObjectID> detectMCRDerivateID(MCRObjectID mcrObjId, String derid);
}
