package org.mycore.solr.search;

import java.util.List;

import org.mycore.parsers.bool.MCRCondition;
import org.mycore.services.fieldquery.MCRResults;
import org.mycore.services.fieldquery.MCRSearcher;
import org.mycore.services.fieldquery.MCRSortBy;
import org.mycore.solr.legacy.MCRLuceneSolrAdapter;
import org.mycore.solr.logging.MCRSolrLogLevels;

public class MCRSolrSearcher extends MCRSearcher {

    @Override
    public boolean isIndexer() {
        return false;
    }

    /**
     * Handles legacy lucene searches.
     * */
    @SuppressWarnings("rawtypes")
    public MCRResults search(MCRCondition condition, int maxResults, List<MCRSortBy> sortBy, boolean addSortData) {
        LOGGER.log(MCRSolrLogLevels.SOLR_INFO, "Processing legacy query \"" + condition.toString() + "\"");
        MCRLuceneSolrAdapter adapter = new MCRLuceneSolrAdapter();
        MCRResults result = adapter.search(condition, maxResults, sortBy, addSortData);
        return result;
    }

}
