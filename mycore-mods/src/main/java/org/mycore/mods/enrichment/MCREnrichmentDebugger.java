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

    public void startIteration();

    public void endIteration();

    public void debugPublication(String label, Element publication);

    public void debugNewIdentifiers(Set<MCRIdentifier> ids);

    public void debugResolved(String token, Element result);
}
