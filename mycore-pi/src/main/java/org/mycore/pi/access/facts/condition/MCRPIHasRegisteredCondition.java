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

package org.mycore.pi.access.xml.condition;

import org.mycore.access.xml.MCRFacts;
import org.mycore.access.xml.conditions.MCRIDCondition;
import org.mycore.access.xml.conditions.MCRSimpleCondition;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.pi.MCRPIManager;
import org.mycore.pi.MCRPIRegistrationInfo;
import org.mycore.pi.MCRPIServiceManager;

import java.util.List;

public class MCRPIHasRegisteredCondition extends MCRSimpleCondition {

    @Override
    public boolean matches(MCRFacts facts) {
        facts.require(this.type);
        return super.matches(facts);
    }

    @Override
    public void setCurrentValue(MCRFacts facts) {
        MCRIDCondition idc = (MCRIDCondition) (facts.require("id"));
        MCRObjectID id = idc.getObjectID();
        if (id == null) {
            super.setCurrentValue(facts);
        } else {
            boolean anyRegistered = MCRPIServiceManager.getInstance().getServiceList().stream().anyMatch(service -> {
                final List<MCRPIRegistrationInfo> createdIdentifiers = MCRPIManager.getInstance()
                        .getCreatedIdentifiers(id, service.getType(), service.getServiceID());
                return createdIdentifiers.stream().anyMatch(pid -> service.isRegistered(id, pid.getAdditional()));
            });
            this.setValue(Boolean.toString(anyRegistered));
        }
    }
}
