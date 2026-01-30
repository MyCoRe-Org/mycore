package org.mycore.solr;

/**
 * An extension of {@link MCRSolrIndexManager} which allows to add and remove indices at runtime.
 */
public interface MCRModifiableSolrIndexManager extends MCRSolrIndexManager {

    /**
     * Adds a new index to the manager. If an index with the same id already exists, it will
     * be replaced.
     * The index will not be created in solr. It will be returned by the manager after adding.
     * @param indexId the id of the index, must not be null
     * @param index the index to add, must not be null
     */
    void addIndex(String indexId, MCRSolrIndex index);

    /**
      * Removes the index with the given id from the manager. If no index with the given id exists,
     * this method does nothing.
      * The index will not be deleted from solr, but will not be returned by the manager anymore.
      * @param indexId the id of the index to remove, must not be null
      */
    void removeIndex(String indexId);

}
