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

package org.mycore.mods.access.xml.condition;

import org.mycore.access.xml.MCRFacts;
import org.mycore.access.xml.conditions.MCRIDCondition;
import org.mycore.access.xml.conditions.MCRSimpleCondition;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.mods.MCRMODSEmbargoUtils;

public class MCRMODSEmbargoCondition extends MCRSimpleCondition {

    @Override
    public boolean matches(MCRFacts facts) {
        facts.require(this.type);
        return super.matches(facts);
    }

    @Override
    public void setCurrentValue(MCRFacts facts) {
        MCRIDCondition idc = (MCRIDCondition) (facts.require("id"));
        MCRObjectID objectID = idc.getObjectID();
        if (objectID == null) {
            super.setCurrentValue(facts);
        } else {
            String embargo = MCRMODSEmbargoUtils.getEmbargo(objectID);
            this.value = Boolean.toString(embargo != null);
        }
    }
}
