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

package org.mycore.mods.access.facts.condition;

import java.util.List;
import java.util.Optional;

import org.jdom2.Element;
import org.mycore.access.facts.MCRFactsHolder;
import org.mycore.access.facts.condition.fact.MCRStringCondition;
import org.mycore.access.facts.fact.MCRObjectIDFact;
import org.mycore.access.facts.fact.MCRStringFact;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.mods.MCRMODSWrapper;

/**
 * condition for fact-based access system,
 * that checks if a certain mods:genre is set.
 * 
 * TODO Could probably be replaced with a more generic XPathCondition.
 * 
 * @author Robert Stephan
 *
 */
public class MCRMODSGenreCondition extends MCRStringCondition {

    private static final String XPATH_COLLECTION = "mods:genre[contains(@valueURI,'/mir_genres#')]";

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
            Optional<MCRObject> optMCRObject = idc.get().getObject();
            if (optMCRObject.isPresent()) {
                MCRMODSWrapper wrapper = new MCRMODSWrapper(optMCRObject.get());
                List<Element> e = wrapper.getElements(XPATH_COLLECTION);
                if ((e != null) && !(e.isEmpty())) {
                    String value = e.get(0).getAttributeValue("valueURI").split("#")[1];
                    if (value.equals(getTerm())) {
                        MCRStringFact fact = new MCRStringFact(getFactName(), getTerm());
                        fact.setValue(value);
                        facts.add(fact);
                        return Optional.of(fact);
                    }
                }
            }
        }
        return Optional.empty();
    }
}
