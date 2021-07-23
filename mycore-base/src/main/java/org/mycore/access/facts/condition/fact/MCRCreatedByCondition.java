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
package org.mycore.access.facts.condition.fact;

import java.util.List;
import java.util.Optional;

import org.jdom2.Element;
import org.mycore.access.facts.MCRFactsHolder;
import org.mycore.access.facts.fact.MCRObjectIDFact;
import org.mycore.access.facts.fact.MCRStringFact;
import org.mycore.datamodel.metadata.MCRObject;

/**
 * This condition check if the given object has the specified createdby service flag.
 * 
 * @author Robert Stephan
 *
 */
public class MCRCreatedByCondition extends MCRStringCondition {

    private String idFact = "objid";

    @Override
    public void parse(Element xml) {
        super.parse(xml);
        idFact = Optional.ofNullable(xml.getAttributeValue("idfact")).orElse("objid");
    }

    @Override
    public Optional<MCRStringFact> computeFact(MCRFactsHolder facts) {
        Optional<MCRObjectIDFact> idc = facts.require(idFact);
        if (idc.isPresent()) {
            Optional<MCRObject> optMcrObj = idc.get().getObject();
            if (optMcrObj.isPresent()) {
                List<String> flags = optMcrObj.get().getService().getFlags("createdby");
                for (String flag : flags) {
                    if (flag.equals(getTerm())) {
                        MCRStringFact fact = new MCRStringFact(getFactName(), getTerm());
                        facts.add(fact);
                        return Optional.of(fact);
                    }
                }
            }
        }
        return Optional.empty();
    }
}
