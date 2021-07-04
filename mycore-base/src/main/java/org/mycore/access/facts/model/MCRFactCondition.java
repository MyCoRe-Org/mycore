package org.mycore.access.facts.model;

public interface MCRFactCondition<F extends MCRFact<?>> extends MCRCondition, MCRFactComputer<F>{
    String getTerm();
    String getFactName();
}
