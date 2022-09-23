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

package org.mycore.pi.access.facts.condition;

import java.util.List;
import java.util.Optional;

import org.jdom2.Element;
import org.mycore.access.facts.MCRFactsHolder;
import org.mycore.access.facts.condition.fact.MCRStringCondition;
import org.mycore.access.facts.fact.MCRObjectIDFact;
import org.mycore.access.facts.fact.MCRStringFact;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.pi.MCRPIManager;
import org.mycore.pi.MCRPIRegistrationInfo;
import org.mycore.pi.MCRPIServiceManager;

/**
 * condition for fact-based access system,
 * that checks if a persistent identifier was registered for the given object.
 * 
 * @author Robert Stephan
 *
 */
public class MCRPIHasRegisteredCondition extends MCRStringCondition {

    private String idFact = "objid";

    @Override
    public void parse(Element xml) {
        super.parse(xml);
        this.idFact = Optional.ofNullable(xml.getAttributeValue("idfact")).orElse("objid");
    }

    @Override
    public Optional<MCRStringFact> computeFact(MCRFactsHolder facts) {
        Optional<MCRObjectIDFact> oIdFact = facts.require(idFact);
        if (oIdFact.isPresent()) {
            MCRObjectID objectID = oIdFact.get().getValue();
            if (objectID != null) {
                boolean anyRegistered = MCRPIServiceManager.getInstance().getServiceList().stream()
                    .anyMatch(service -> {
                        final List<MCRPIRegistrationInfo> createdIdentifiers = MCRPIManager.getInstance()
                            .getCreatedIdentifiers(objectID, service.getType(), service.getServiceID());
                        return createdIdentifiers.stream()
                            .anyMatch(pid -> service.isRegistered(objectID, pid.getAdditional()));
                    });

                //only positive facts are added to the facts holder
                if (anyRegistered) {
                    MCRStringFact fact = new MCRStringFact(getFactName(), getTerm());
                    fact.setValue(Boolean.TRUE.toString());
                    facts.add(fact);
                    return Optional.of(fact);
                }
            }
        }
        return Optional.empty();
    }
}
