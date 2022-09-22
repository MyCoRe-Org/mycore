package org.mycore.mods.enrichment;

import java.util.Set;

import org.jdom2.Element;

/**
 * Allows debugging enrichment resolving steps.
 * 
 * @see MCREnricher#setDebugger(MCREnrichmentDebugger)
 * 
 * @author Frank L\u00FCtzenkirchen
 */
public interface MCREnrichmentDebugger {

    void startIteration();

    void endIteration();

    void debugPublication(String label, Element publication);

    void debugNewIdentifiers(Set<MCRIdentifier> ids);

    void debugResolved(String token, Element result);
}
