package org.mycore.solr.index.cs.strategy;

/**
 * Base interface for all solr specific index strategies.
 * 
 * @author Matthias Eichner
 *
 * @param <T>
 */
public interface MCRSolrIndexStrategy<T> {

    public boolean check(T t);

}
