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
package org.mycore.access.facts;

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;

import org.mycore.access.facts.model.MCRFact;
import org.mycore.access.facts.model.MCRFactComputer;

public class MCRFactsHolder {

    private Collection<MCRFactComputer<MCRFact<?>>> computers;

    private final Set<MCRFact<?>> facts = new HashSet<MCRFact<?>>();

    public MCRFactsHolder(Collection<MCRFactComputer<MCRFact<?>>> computers) {
        this.computers = computers;
    }

    public void add(MCRFact<?> fact) {
        facts.add(fact);
    }

    public boolean isFact(String factName, String inquiry) {
        Optional<MCRFact<?>> osc = facts.stream()
            .filter(f -> factName.equals(f.getName()))
            .filter(f -> inquiry.equals(inquiry))
            .findFirst();
        return osc.isPresent();
    }

    public Optional<MCRFact<?>> require(String factName, MCRFactComputer<MCRFact<?>> factComputer) {
        Optional<MCRFact<?>> osc = facts.stream().filter(f -> factName.equals(f.getName())).findFirst();
        if (osc.isPresent()) {
            return osc;
        } else {
            Optional<MCRFact<?>> fact = factComputer.computeFact(this);
            if (fact.isPresent()) {
                facts.add(fact.get());
                return fact;
            }
        }
        return Optional.empty();
    }

    @SuppressWarnings("unchecked")
    public <F extends MCRFact<?>> Optional<F> require(String factName) {
        Optional<F> osc = (Optional<F>) facts.stream().filter(f -> factName.equals(f.getName())).findFirst();
        if (osc.isPresent()) {
            return osc;
        } else {
            Optional<MCRFactComputer<MCRFact<?>>> theComputer = computers.stream()
                .filter(c -> factName.equals(c.getFactName())).findFirst();
            if (theComputer.isPresent()) {
                Optional<MCRFact<?>> fact = theComputer.get().computeFact(this);
                if (fact.isPresent()) {
                    facts.add(fact.get());
                    return (Optional<F>) fact;
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public String toString() {
        StringJoiner sj = new StringJoiner(" & ");
        facts.stream().forEach(f -> sj.add(f.toString()));
        return sj.toString();
    }
}
