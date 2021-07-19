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

import java.util.Optional;

import org.jdom2.Element;
import org.mycore.access.facts.MCRFactsHolder;
import org.mycore.access.facts.fact.MCRObjectIDFact;
import org.mycore.access.facts.fact.MCRStringFact;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.metadata.MCRObjectService;

/**
 * This implementation of a condition
 * checks if the given MyCoRe object or derivate
 * has the given state entry in service flags.
 * 
 * Examples:
 * 
 * &lt;status&gt;review&lt;/status&gt;  
 * &lt;status basefact="derid"&gt;published&lt;/status&gt;  
 * 
 * @author Robert Stephan
 *
 */
public class MCRStateCondition extends MCRStringCondition {

    /**
     * id of the fact that contains the ID of the MyCoRe-Object or Derivate
     * possible values are "objid" or "derivateid".
     */
    private String idFact = "objid";

    @Override
    public void parse(Element xml) {
        super.parse(xml);
        this.idFact = Optional.ofNullable(xml.getAttributeValue("idfact")).orElse("objid");
    }

    @Override
    public Optional<MCRStringFact> computeFact(MCRFactsHolder facts) {
        Optional<MCRObjectIDFact> idc = facts.require(idFact);
        if (idc.isPresent()) {
            MCRObjectService service = idc.get().getObject().getService();
            MCRCategoryID state = service.getState();
            if (getTerm().equals(state.getID())) {
                MCRStringFact fact = new MCRStringFact(getFactName(), getTerm());
                fact.setValue(state.getID());
                facts.add(fact);
                return Optional.of(fact);
            }
        }
        return Optional.empty();
    }
}
