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

package org.mycore.mods.enrichment;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.jdom2.Element;

/**
 * Tracks all identifiers found in a given publication.
 * Each resolving and merge step may add new identifiers
 * returned by data sources. For example,
 * the publication may initially only have a PubMed ID,
 * but after resolving that ID the data source may have returned
 * the DOI of the publication. So we keep a list to distinguish
 * newly found identifiers from already known and resolved identifiers.
 *
 * @author Frank L\u00FCtzenkirchen
 */
class MCRIdentifierPool {

    /** Set of already known identifiers resolved in the last round */
    private Set<MCRIdentifier> oldIdentifiers = new HashSet<MCRIdentifier>();

    /** Set of new identifiers returned with data from external sources in the current resolving round */
    private Set<MCRIdentifier> newIdentifiers = new HashSet<MCRIdentifier>();

    MCRIdentifierPool() {
    }

    int size() {
        return oldIdentifiers.size();
    }

    /** Add all new identifiers that can be found in the given MODS object */
    synchronized void addIdentifiersFrom(Element object) {
        for (MCRIdentifierType type : MCRIdentifierTypeFactory.instance().getTypes()) {
            newIdentifiers.addAll(type.getIdentifiers(object));
        }
        newIdentifiers.removeAll(oldIdentifiers);
    }

    /** Returns true, if new identifiers were added in the current resolving round */
    boolean hasNewIdentifiers() {
        return !newIdentifiers.isEmpty();
    }

    void buildNewIdentifiersIn(Element object) {
        newIdentifiers.forEach(id -> id.buildElement(object));
    }

    /** Merges all new identifies into the set of old identifiers */
    void continueWithNewIdentifiers() {
        oldIdentifiers.clear();
        oldIdentifiers.addAll(newIdentifiers);
        newIdentifiers.clear();
    }

    List<MCRIdentifier> getIdentifiersOfType(MCRIdentifierType type) {
        return oldIdentifiers.stream().filter(id -> id.getType().equals(type)).collect(Collectors.toList());
    }
}
