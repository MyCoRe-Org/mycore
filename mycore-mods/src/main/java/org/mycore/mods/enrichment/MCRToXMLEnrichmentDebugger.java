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
 * Allows debugging enrichment resolving steps.
 * Writes debug output to an XML element.
 * Run enrich() on the resolver, 
 * afterwards call getDebugXML() on this debugger.
 * 
 * @see MCREnricher#setDebugger(MCREnrichmentDebugger)
 * @see MCRToXMLEnrichmentDebugger#getDebugXML()
 * 
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRToXMLEnrichmentDebugger implements MCREnrichmentDebugger {

    Element debugElement = new Element("debugEnrichment");

    public void startIteration() {
        Element enrichmentIteration = new Element("enrichmentIteration");
        debugElement.addContent(enrichmentIteration);
        debugElement = enrichmentIteration;
    }

    public void endIteration() {
        debugElement = debugElement.getParentElement();
    }

    public void debugPublication(String label, Element publication) {
        debugElement.addContent(new Element(label).addContent(publication.clone()));
    }

    public void debugNewIdentifiers(Set<MCRIdentifier> ids) {
        Element e = new Element("newIdentifiersFound");
        ids.forEach(id -> id.mergeInto(e));
        debugElement.addContent(e);
    }

    public void debugResolved(String dataSourceID, Element result) {
        Element resolved = new Element("resolved").setAttribute("from", dataSourceID);
        debugElement.addContent(resolved);
        resolved.addContent(result.clone());
    }

    public Element getDebugXML() {
        return debugElement;
    }
}
