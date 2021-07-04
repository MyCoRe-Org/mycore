package org.mycore.access.facts.model;

import java.util.Optional;

import org.mycore.access.facts.MCRFactsHolder;

public interface MCRFactComputer<F extends MCRFact<?>> extends MCRCondition {
    String getFactName();
    Optional<F> computeFact(MCRFactsHolder facts);
}
