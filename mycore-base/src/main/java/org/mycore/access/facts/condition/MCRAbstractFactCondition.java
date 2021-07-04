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
import org.mycore.access.facts.model.MCRFact;
import org.mycore.access.facts.model.MCRFactCondition;

public abstract class MCRAbstractFactCondition<V, F extends MCRFact<V>> implements MCRFactCondition<F> {

    static final String UNDEFINED = "undefined";
    
    private String factName;
    
    private String term;
    
    private Element boundElement = null;
    
    private String type;
    
    private boolean debug;

    /** 
     * implementors need to call super.parse(xml) to bind the Element to the condition
     */
    public void parse(Element xml) {
        boundElement = xml;
        type = xml.getName();
        term = xml.getTextTrim();
        factName = Optional.ofNullable(xml.getAttributeValue("fact")).orElse(type);
    }

    @Override
    public boolean matches(MCRFactsHolder facts) {
        if (facts.isFact(getFactName(), getTerm())) {
            return true;
        }
        Optional<F> computed = computeFact(facts);
        return computed.isPresent();
    }

    @Override
    public Element getBoundElement() {
        return boundElement;
    }
    
    public String getType() {
        return type;
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

    @Override
    public boolean isDebug() {
        return debug;
    }

    @Override
    public void setDebug(boolean b) {
        this.debug =b;
        
    }
}
