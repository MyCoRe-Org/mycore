/**
 * 
 */
package org.mycore.frontend.indexbrowser.lucene;

import java.util.List;

/**
 * @author shermann
 *
 */
public interface MCRIIndexBrowserSearcher {

    /**
     * Starts the search and returns a result list.
     * 
     * @return the result list
     */
    public List<MCRIndexBrowserEntry> doSearch();

    /**
     * Returns the created result list.
     * 
     * @return a list of MCRIndexBrowserEntries
     */
    public List<MCRIndexBrowserEntry> getResultList();
}
