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
