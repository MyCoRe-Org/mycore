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

import java.util.Optional;

import org.jdom2.Element;
import org.mycore.access.facts.MCRFactsHolder;
import org.mycore.access.facts.condition.MCRAbstractCondition;
import org.mycore.access.facts.model.MCRFact;
import org.mycore.access.facts.model.MCRFactCondition;

/**
 * This is the base implementation for a condition which evaluates or produces facts
 * 
 * Subclasses should call super.parse(xml) to bind the XML element to the condition.
 * 
 * If you specify the attribute 'fact' on the condition XML. It will be used as name for
 * the newly created fact. Otherwise the name of the condition will be used as name for the fact.
 * 
 * @author Robert Stephan
 *
 * @param <V> the class of the value
 * @param <F> the class of the fact
 */
public abstract class MCRAbstractFactCondition<F extends MCRFact<?>> extends MCRAbstractCondition implements MCRFactCondition<F> {

    static final String UNDEFINED = "undefined";

    private String factName;

    private String term;
    /** 
     * implementors of this method should call super.parse(xml) to bind the XML element to the condition
     */
    public void parse(Element xml) {
        super.parse(xml);
        term = xml.getTextTrim();
        factName = Optional.ofNullable(xml.getAttributeValue("fact")).orElse(getType());
    }

    @Override
    public boolean matches(MCRFactsHolder facts) {
        if (facts.isFact(getFactName(), getTerm())) {
            return true;
        }
        Optional<F> computed = computeFact(facts);
        return computed.isPresent();
    }

    public String getFactName() {
        return factName;
    }

    public void setFactName(String factName) {
        this.factName = factName;
    }

    public String getTerm() {
        return term;
    }

    public void setTerm(String term) {
        this.term = term;
    }
}
