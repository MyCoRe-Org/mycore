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
package org.mycore.access.facts.fact;

import java.util.Objects;

import org.mycore.access.facts.model.MCRFact;

public abstract class MCRAbstractFact<T> implements MCRFact<T> {

    private T value = null;

    private String name;
    
    private String term;
    
    public MCRAbstractFact(String name, String term) {
        this.name = name;
        this.term = term;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public void setTerm(String term) {
        this.term = term;
    }

    @Override
    public String getTerm() {
        return term;
    }

    @Override
    public void setValue(T value) {
        this.value = value;

    }

    @Override
    public T getValue() {
        return value;
    }

    @Override
    public String toString() {
        return name + "=" + value.toString();
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, term);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof MCRAbstractFact)) {
            return false;
        }
        @SuppressWarnings("rawtypes")
        MCRAbstractFact other = (MCRAbstractFact) obj;
        return Objects.equals(name, other.name) && Objects.equals(term, other.term);
    }
    
}
