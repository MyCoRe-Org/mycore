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
import org.mycore.access.facts.fact.MCRSimpleFact;
import org.mycore.access.facts.model.MCRFact;

/**
 * This condition checks if the given id or another fact matches a regular expression.
 *   
 * Examples:
 * &lt;rege&gt;webpage:/content/search/.*_intern.xed&lt;/regex&gt;
 * &lt;regex basefact='user'&gt;.*admin&lt;/regex&gt;
 * &lt;regex basefact='category'&gt;ddc:.*&lt;/regex&gt;
 * 
 * @author Robert Stephan
 *
 */
public class MCRRegExCondition extends MCRSimpleCondition {

    private String baseFactName;

    @Override
    public void parse(Element xml) {
        super.parse(xml);
        baseFactName = Optional.ofNullable(xml.getAttributeValue("basefact")).orElse("id");
        setFactName(Optional.ofNullable(xml.getAttributeValue("fact")).orElse(getType() + "-" + baseFactName));
    }

    @Override
    public Optional<MCRSimpleFact> computeFact(MCRFactsHolder facts) {
        Optional<MCRFact<?>> baseFact = facts.require(baseFactName);
        if (baseFact.isPresent()) {
            String v = baseFact.get().getValue().toString();
            if (v.matches(getTerm())) {
                MCRSimpleFact fact = new MCRSimpleFact(getFactName(), getTerm());
                fact.setValue(v);
                facts.add(fact);
                return Optional.of(fact);
            }
        }
        return Optional.empty();
    }

}
