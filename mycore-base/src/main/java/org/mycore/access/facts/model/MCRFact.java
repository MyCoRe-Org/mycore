package org.mycore.access.facts.model;

public interface MCRFact<V> {
    
    void setName(String name);
    String getName();
    
    void setTerm(String term);
    String getTerm();
    
    void setValue(V value);
    V getValue();

}
