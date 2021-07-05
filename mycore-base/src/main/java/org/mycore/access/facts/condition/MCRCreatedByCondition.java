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
package org.mycore.access.facts.condition;

import java.util.List;
import java.util.Optional;

import org.jdom2.Element;
import org.mycore.access.facts.MCRFactsHolder;
import org.mycore.access.facts.fact.MCRObjectIDFact;
import org.mycore.access.facts.fact.MCRSimpleFact;
import org.mycore.datamodel.metadata.MCRObjectService;

public class MCRCreatedByCondition extends MCRSimpleCondition {

    private String idFact = "objid";

    @Override
    public void parse(Element xml) {
        super.parse(xml);
        idFact = Optional.ofNullable(xml.getAttributeValue("idfact")).orElse("objid");
    }

    @Override
    public Optional<MCRSimpleFact> computeFact(MCRFactsHolder facts) {
        Optional<MCRObjectIDFact> idc = facts.require(idFact);
        if (idc.isPresent()) {
            MCRObjectService service = idc.get().getObject().getService();
            List<String> flags = service.getFlags("createdby");
            for (String flag : flags) {
                if (flag.equals(getTerm())) {
                    MCRSimpleFact fact = new MCRSimpleFact(getFactName(), getTerm());
                    facts.add(fact);
                    return Optional.of(fact);
                }
            }
        }
        return Optional.empty();
    }
}
