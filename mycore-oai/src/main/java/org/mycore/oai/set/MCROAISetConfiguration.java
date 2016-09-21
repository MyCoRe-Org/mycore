package org.mycore.oai.set;

public interface MCROAISetConfiguration<T> {

    public String getId();

    public String getURI();

    public MCROAISetHandler<T> getHandler();

}
