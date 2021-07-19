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

import java.util.Objects;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;
import org.mycore.access.facts.MCRFactsHolder;
import org.mycore.access.facts.fact.MCRCategoryIDFact;
import org.mycore.access.facts.fact.MCRObjectIDFact;
import org.mycore.datamodel.classifications2.MCRCategLinkReference;
import org.mycore.datamodel.classifications2.MCRCategLinkService;
import org.mycore.datamodel.classifications2.MCRCategLinkServiceFactory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.metadata.MCRObjectID;

/**
 * This implementation checks if the given object belongs to a certain classification entry.
 * 
 * As default the object is read from the fact "objid".
 * Using the attribute 'idfact' another fact containing an object id can be specified
 * 
 * Example:
 * &lt;category idfact='the_other_object' fact='published'&gt;state:published&lt;/category&gt;
 * 
 * This rule will check if the object already stored as fact 'the_other_object' belongs to
 * the category 'state:published'.
 * If true, a fact 'published' will be stored in the facts holder.
 * 
 * 
 * @author Robert Stephan
 *
 */
public class MCRCategoryCondition extends MCRAbstractFactCondition<MCRCategoryID, MCRCategoryIDFact> {

    private static Logger LOGGER = LogManager.getLogger();

    private String idFact = "objid";

    @Override
    public void parse(Element xml) {
        super.parse(xml);
        if (xml.getAttributeValue("id") != null) {
            LOGGER.warn("Attribute 'id' is deprecated - use 'idfact' instead!");
        }
        this.idFact = Optional.ofNullable(xml.getAttributeValue("idfact")).orElse("objid");
    }

    @Override
    public boolean matches(MCRFactsHolder facts) {
        if (super.matches(facts)) {
            return true;
        }

        Optional<MCRCategoryIDFact> computed = computeFact(facts);
        return computed.isPresent();
    }

    @Override
    public Optional<MCRCategoryIDFact> computeFact(MCRFactsHolder facts) {
        Optional<MCRObjectIDFact> idc = facts.require(idFact);
        if (idc.isPresent()) {
            MCRObjectID objectID = idc.get().getValue();
            if (objectID != null) {
                MCRCategoryID categoryID = MCRCategoryID.fromString(getTerm());
                MCRCategLinkService linkService = MCRCategLinkServiceFactory.getInstance();
                if (linkService.isInCategory(new MCRCategLinkReference(objectID), categoryID)) {
                    MCRCategoryIDFact result = new MCRCategoryIDFact(getFactName(), getTerm());
                    result.setValue(categoryID);
                    facts.add(result);
                    return Optional.of(result);
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        MCRCategoryCondition that = (MCRCategoryCondition) o;
        return idFact.equals(that.idFact);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), idFact);
    }

}
