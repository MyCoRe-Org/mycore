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
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectService;

import java.util.List;

public class MCRCreatedByCondition extends MCRSimpleCondition {

    @Override
    public boolean matches(MCRFacts facts) {
        String createdBy = facts.require(this.type).value;
        String userID = value.equals("currentUser") ? facts.require("user").value : value;
        if (userID.equals(createdBy)) {
            facts.add(this);
        }
        return super.matches(facts);
    }

    @Override
    public void setCurrentValue(MCRFacts facts) {
        MCRIDCondition idc = (MCRIDCondition) (facts.require("id"));
        MCRObject object = idc.getObject();
        if (object != null) {
            MCRObjectService service = object.getService();
            List<String> flags = service.getFlags("createdby");
            if ((flags != null) && !flags.isEmpty()) {
                this.value = flags.get(0);
            }
        } else {
            super.setCurrentValue(facts);
        }
    }
}
