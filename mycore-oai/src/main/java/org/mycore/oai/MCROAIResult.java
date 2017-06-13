package org.mycore.oai;

import java.util.List;
import java.util.Optional;

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
     * @return the next cursor
     */
    Optional<String> nextCursor();

}
