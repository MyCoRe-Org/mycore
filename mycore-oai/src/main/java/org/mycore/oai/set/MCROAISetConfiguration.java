package org.mycore.oai.set;

public interface MCROAISetConfiguration<Q, R, K> {

    String getId();

    String getURI();

    MCROAISetHandler<Q, R, K> getHandler();

}
