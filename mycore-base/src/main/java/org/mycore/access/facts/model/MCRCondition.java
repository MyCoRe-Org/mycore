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
package org.mycore.access.facts.model;

import org.jdom2.Element;
import org.mycore.access.facts.MCRFactsHolder;

/**
 * This interface represents a rule of the fact-based access system. 
 * It is specified in the rules.xml file.
 * 
 * Sub interfaces are {@link MCRCombinedCondition}, which is used to build a boolean algebra (and, or, not, ...)
 * and {@link MCRFactCondition}, which validates existing facts or creates new ones.
 * 
 * New rules need to be registered in mycore.properties as follows:
 * 
 * MCR.Access.Facts.Condition.{type}={class}
 * e.g. MCR.Access.Facts.Condition.ip=org.mycore.access.facts.condition.fact.MCRIPCondition
 * 
 * @author Robert Stephan
 *
 */
public interface MCRCondition {

    /**
     * the type of the rule
     */
    String getType();

    boolean matches(MCRFactsHolder facts);

    void parse(Element xml);

    /**
     * This is primary for rules.xml debugging purposes
     * @return the part of the xml which this Condition represents
     */
    Element getBoundElement();

    boolean isDebug();

    void setDebug(boolean b);

}
