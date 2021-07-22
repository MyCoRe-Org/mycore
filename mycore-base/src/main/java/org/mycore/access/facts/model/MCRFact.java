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
 * A fact is simple piece of information.
 * It is retrieved by an @MCRFactComputer from a certain MyCoReObject, Derivate, ...
 * and other information from the application (e.g. MCRSession) or from other already retrieved facts.
 * 
 * It consists of a name and a typed value.
 * 
 * Additional the query string, used to retrieve the fact is, stored.
 * This can be useful for facts, that are retrieved via regular expression or wildcard expressions. 
 *  
 * @author Robert Stephan
 *
 * @param <V> the class of the value.
 */
public interface MCRFact<V> {

    void setName(String name);

    String getName();

    void setTerm(String term);

    String getTerm();

    void setValue(V value);

    V getValue();

}
