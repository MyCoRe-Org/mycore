/**
 * 
 */
package org.mycore.services.fieldquery;

import java.util.List;

import org.mycore.backend.hibernate.MCRHIBConnection;
import org.mycore.common.events.MCREvent;
import org.mycore.datamodel.ifs.MCRFile;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.parsers.bool.MCRCondition;

/**
 * @author shermann
 * 
 */
public class MCRDerivateWithURNSearcher extends MCRSearcher {

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.mycore.services.fieldquery.MCRSearcher#search(org.mycore.parsers.
     * bool.MCRCondition, int, java.util.List, boolean)
     */
    @Override
    public MCRResults search(MCRCondition condition, int maxResults, List<MCRSortBy> sortBy,
            boolean addSortData) {
        if (!(condition instanceof MCRQueryCondition)) {
            LOGGER.warn("Retrieved of type " + condition.getClass() + " but only type"
                    + MCRQueryCondition.class.getName() + " is currently supported.");
            return new MCRResults();
        }

        String fieldName = ((MCRQueryCondition) condition).getField().getName();

        if (fieldName.equals("objectURN")) {
            return handleObjectsURN(condition);
        }

        if (fieldName.equals("objectsWithURN")) {
            return handleObjectsWithURN(condition);
        }
        return new MCRResults();
    }

    @SuppressWarnings("unchecked")
    private MCRResults handleObjectsURN(MCRCondition condition) {
        String value = null;
        if (condition instanceof MCRQueryCondition) {
            value = ((MCRQueryCondition) condition).getValue();
        }
        List<String> resultList = null;

        MCRHIBConnection conn = MCRHIBConnection.instance();

        if (value == null) {
            return new MCRResults();
        }
        /*
         * get objects matching the given urn (only non derivates)
         */
        resultList = conn
                .getSession()
                .createSQLQuery(
                        "SELECT DISTINCT A.mcrid FROM MCRXMLTABLE A,MCRLINKHREF B "
                                + "WHERE (A.mcrid = B.mcrfrom and B.mcrto in (SELECT U.mcrid FROM MCRURN U where U.mcrurn = '"
                                + value
                                + "')) OR (A.mcrid in (SELECT I.mcrid FROM MCRURN I WHERE I.mcrurn = '"
                                + value + "') AND NOT (A.mcrtype='derivate'))").list();

        MCRResults toReturn = new MCRResults();
        for (String entry : resultList) {
            MCRHit aHit = new MCRHit(entry);
            toReturn.addHit(aHit);
        }

        return toReturn;
    }

    @SuppressWarnings("unchecked")
    private MCRResults handleObjectsWithURN(MCRCondition condition) {
        String value = null;
        if (condition instanceof MCRQueryCondition) {
            value = ((MCRQueryCondition) condition).getValue();
        }
        List<String> resultList = null;

        MCRHIBConnection conn = MCRHIBConnection.instance();

        if (value != null && value.equalsIgnoreCase("false")) {
            /* all objects without urn */
            resultList = conn
                    .getSession()
                    .createSQLQuery(
                            "SELECT MCRID FROM MCRXMLTABLE WHERE MCRID NOT IN (SELECT DISTINCT urn.MCRID FROM MCRURN urn)")
                    .list();
        } else {
            /* all objects with urn */
            resultList = conn.getSession().createSQLQuery(
                    "SELECT DISTINCT mcrid FROM MCRURN ORDER BY 1 ASC").list();
        }

        MCRResults toReturn = new MCRResults();
        for (String entry : resultList) {
            MCRHit aHit = new MCRHit(entry);
            toReturn.addHit(aHit);
        }

        return toReturn;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.mycore.services.fieldquery.MCRSearcher#handleFileCreated(org.mycore
     * .common.events.MCREvent, org.mycore.datamodel.ifs.MCRFile)
     */
    @Override
    protected void handleFileCreated(MCREvent evt, MCRFile file) {
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.mycore.services.fieldquery.MCRSearcher#handleFileDeleted(org.mycore
     * .common.events.MCREvent, org.mycore.datamodel.ifs.MCRFile)
     */
    @Override
    protected void handleFileDeleted(MCREvent evt, MCRFile file) {
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.mycore.services.fieldquery.MCRSearcher#handleFileRepaired(org.mycore
     * .common.events.MCREvent, org.mycore.datamodel.ifs.MCRFile)
     */
    @Override
    protected void handleFileRepaired(MCREvent evt, MCRFile file) {
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.mycore.services.fieldquery.MCRSearcher#handleFileUpdated(org.mycore
     * .common.events.MCREvent, org.mycore.datamodel.ifs.MCRFile)
     */
    @Override
    protected void handleFileUpdated(MCREvent evt, MCRFile file) {
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.mycore.services.fieldquery.MCRSearcher#handleObjectCreated(org.mycore
     * .common.events.MCREvent, org.mycore.datamodel.metadata.MCRObject)
     */
    @Override
    protected void handleObjectCreated(MCREvent evt, MCRObject obj) {
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.mycore.services.fieldquery.MCRSearcher#handleObjectDeleted(org.mycore
     * .common.events.MCREvent, org.mycore.datamodel.metadata.MCRObject)
     */
    @Override
    protected void handleObjectDeleted(MCREvent evt, MCRObject obj) {
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.mycore.services.fieldquery.MCRSearcher#handleObjectRepaired(org.mycore
     * .common.events.MCREvent, org.mycore.datamodel.metadata.MCRObject)
     */
    @Override
    protected void handleObjectRepaired(MCREvent evt, MCRObject obj) {
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.mycore.services.fieldquery.MCRSearcher#handleObjectUpdated(org.mycore
     * .common.events.MCREvent, org.mycore.datamodel.metadata.MCRObject)
     */
    @Override
    protected void handleObjectUpdated(MCREvent evt, MCRObject obj) {
    }
}