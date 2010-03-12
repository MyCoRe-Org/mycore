/**
 * 
 */
package org.mycore.services.fieldquery;

import java.util.List;

import org.apache.log4j.Logger;
import org.mycore.backend.hibernate.MCRHIBConnection;
import org.mycore.parsers.bool.MCRCondition;

/**
 * @author shermann
 * 
 */
public class MCRDerivateSearcher extends MCRDerivateWithURNSearcher {
    /** The logger for this class */
    protected static Logger LOGGER = Logger.getLogger(MCRDerivateSearcher.class);

    @Override
    public MCRResults search(MCRCondition condition, int maxResults, List<MCRSortBy> sortBy, boolean addSortData) {
        MCRResults toReturn = new MCRResults();
        try {
            String query = "select a.mcrid from mcrxmltable a where a.mcrtype = 'derivate' and a.mcrid in (select mcrid from mcrurn) and modified ";

            String[] tokens = condition.toString().split(" ");
            if (tokens != null && tokens.length >= 4) {
                String op = tokens[1];
                String val = tokens[2].replaceAll("\"", "'") + " " + tokens[3].replaceAll("\"", "'");
                query += op + val;

                MCRHIBConnection conn = MCRHIBConnection.instance();
                List<String> resultList = conn.getSession().createSQLQuery(query).list();

                for (String entry : resultList) {
                    MCRHit aHit = new MCRHit(entry);
                    toReturn.addHit(aHit);
                }
            } else {
                LOGGER.warn("The query with condition " + condition.toString() + " is invalid");
            }
        } catch (Exception ex) {
            LOGGER.error("The query part " + condition.toString() + " was invalid, thus returning an empty result", ex);
        }
        /* maybe empty, if errors occured */
        return toReturn;
    }
}