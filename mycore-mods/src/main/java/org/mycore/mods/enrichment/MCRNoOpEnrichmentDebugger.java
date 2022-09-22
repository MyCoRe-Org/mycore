package org.mycore.mods.enrichment;

import java.util.Set;

import org.jdom2.Element;

/**
 * Default debugger for enrichment process. Does nothing.
 * 
 * @see MCREnricher#setDebugger(MCREnrichmentDebugger)
 * 
 * @author Frank L\u00FCtzenkirchen
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
