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
package org.mycore.access.xml.conditions;

import org.mycore.access.xml.MCRFacts;
import org.mycore.access.xml.MCRObjectCacheFactory;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;


public class MCRIDCondition extends MCRSimpleCondition {


    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public boolean matches(MCRFacts facts) {
        facts.require(this.type);
        return super.matches(facts);
    }

    /**
     * @return the object id instance for this Condition or null if its not a object
     */
    public MCRObjectID getObjectID(){
        if(!MCRObjectID.isValid(value)){
            return null;
        }
        return MCRObjectID.getInstance(value);
    }

    /**
     * @return the object for this Condition or null if it is not a object
     */
    public MCRObject getObject() {
        MCRObjectID objectID = getObjectID();
        if(objectID==null){
            return null;
        }
        return MCRObjectCacheFactory.instance().getObject(objectID);
    }
}
