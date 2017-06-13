package org.mycore.oai.set;

public interface MCROAISetConfiguration<Q, R, K> {

    public String getId();

    public String getURI();

    public MCROAISetHandler<Q, R, K> getHandler();

}
