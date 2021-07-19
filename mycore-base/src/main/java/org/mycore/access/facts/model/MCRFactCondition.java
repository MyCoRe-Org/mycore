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

/**
 * This interface describes a rule (from rules.xml) which can evaluate existing facts
 * or create new facts if applicable.
 * 
 * By extending {@link MCRFactComputable} the rule knows how to create its facts
 * from the given object or environment.
 * 
 * New rules need to be registered in mycore.properties as follows:
 * 
 * MCR.Access.Facts.Condition.{type}={class}
 * e.g. MCR.Access.Facts.Condition.ip=org.mycore.access.facts.condition.MCRIPCondition
 * 
 * @author Robert Stephan
 *
 * @param <F> the fact to be generated
 */
public interface MCRFactCondition<F extends MCRFact<?>> extends MCRCondition, MCRFactComputable<F> {

}
