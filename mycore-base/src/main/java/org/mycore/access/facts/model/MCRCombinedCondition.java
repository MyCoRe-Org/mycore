package org.mycore.access.facts.model;

import java.util.Set;

public interface MCRCombinedCondition extends MCRCondition {

    void add(MCRCondition c);

    Set<MCRCondition> getChildConditions();

    void debugInfoForMatchingChildElement(MCRCondition c, boolean matches);

}
