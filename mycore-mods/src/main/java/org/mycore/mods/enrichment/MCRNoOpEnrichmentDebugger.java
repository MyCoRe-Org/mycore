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

import java.util.Set;

import org.jdom2.Element;

/**
 * Default debugger for enrichment process. Does nothing.
 * 
 * @see MCREnricher#setDebugger(MCREnrichmentDebugger)
 * 
 * @author Frank Lützenkirchen
 */
public class MCRNoOpEnrichmentDebugger implements MCREnrichmentDebugger {

    public void startIteration() {
        // Do nothing here
    }

    public void endIteration() {
        // Do nothing here
    }

    public void debugPublication(String label, Element publication) {
        // Do nothing here
    }

    public void debugNewIdentifiers(Set<MCRIdentifier> ids) {
        // Do nothing here
    }

    public void debugResolved(String token, Element result) {
        // Do nothing here
    }
}
