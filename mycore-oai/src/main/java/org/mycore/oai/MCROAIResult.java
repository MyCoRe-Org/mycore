package org.mycore.oai;

import java.util.List;

import org.mycore.oai.pmh.Header;

/**
 * The result of a searcher query.
 * 
 * @author Matthias Eichner
 */
public interface MCROAIResult {

    /**
     * Returns a list of mycore object identifiers.
     * 
     * @return list of mycore object identifiers
     */
    List<Header> list();

    /**
     * Number of all hits
     * 
     * @return number of hits
     */
    int getNumHits();

    /**
     * Next cursor or null
     * 
     * @return the next cursor
     */
    String nextCursor();

}
