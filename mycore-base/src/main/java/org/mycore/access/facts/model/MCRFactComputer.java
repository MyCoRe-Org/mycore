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

import java.util.Optional;

import org.mycore.access.facts.MCRFactsHolder;

/**
 * This interface can be used to implement a class which can generated new 
 * facts based on the current fact list.
 * 
 * Usually the fact list already contains a fact with an MyCoRe ObjectID
 * which can be used to retrieve more facts from the given object.
 * 
 * @author Robert Stephan
 *
 * @param <F> the class of the fact that can be created by this fact computer
 */
public interface MCRFactComputer<F extends MCRFact<?>> extends MCRCondition {
    String getFactName();

    Optional<F> computeFact(MCRFactsHolder facts);
}
