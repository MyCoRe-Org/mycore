package org.mycore.services.fieldquery;

public interface MCRQueryEngine {
    /**
     * Executes a query and returns the query results. If the query contains
     * fields from different indexes or should span across multiple hosts, the
     * results of multiple searchers are combined.
     * 
     * @param query
     *            the query
     * 
     * @return the query results
     */
    public MCRResults search(MCRQuery query);

    /**
     * Executes a query and returns the query results. If the query contains
     * fields from different indexes or should span across multiple hosts, the
     * results of multiple searchers are combined.
     * 
     * @param query
     *            the query
     * @param comesFromRemoteHost
     *            if true, this query is originated from a remote host, so no
     *            sorting of results is done for performance reasons
     * 
     * @return the query results
     */
    public MCRResults search(MCRQuery query, boolean comesFromRemoteHost);
}
