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
package org.mycore.access.facts.fact;

import java.util.Optional;

import org.mycore.access.facts.MCRObjectCacheFactory;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;

/**
 * This fact implementation can store a MyCoRe object id
 * and allows access to the corresponding MyCoRe object.
 * 
 * @author Robert Stephan
 *
 */
public class MCRObjectIDFact extends MCRAbstractFact<MCRObjectID> {

    public MCRObjectIDFact(String name, String term) {
        super(name, term);
    }

    public MCRObjectIDFact(String name, String term, MCRObjectID value) {
        super(name, term);
        setValue(value);
    }

    /**
     * @return the object for this condition or null if it is not a object
     */
    public Optional<MCRObject> getObject() {
        MCRObjectID objectID = getValue();
        if (objectID == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(MCRObjectCacheFactory.getInstance().getObject(objectID));
    }

}
