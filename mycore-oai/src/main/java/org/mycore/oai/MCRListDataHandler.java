/*
 * $Revision$ $Date$
 * 
 * This file is part of *** M y C o R e *** See http://www.mycore.de/ for
 * details.
 * 
 * This program is free software; you can use it, redistribute it and / or
 * modify it under the terms of the GNU General Public License (GPL) as
 * published by the Free Software Foundation; either version 2 of the License or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program, in a file called gpl.txt or license.txt. If not, write to the
 * Free Software Foundation Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307 USA
 */

package org.mycore.oai;

import static org.mycore.oai.MCROAIConstants.ARG_FROM;
import static org.mycore.oai.MCROAIConstants.ARG_METADATA_PREFIX;
import static org.mycore.oai.MCROAIConstants.ARG_RESUMPTION_TOKEN;
import static org.mycore.oai.MCROAIConstants.ARG_SET;
import static org.mycore.oai.MCROAIConstants.ARG_UNTIL;
import static org.mycore.oai.MCROAIConstants.DATESTAMP_PATTERN;
import static org.mycore.oai.MCROAIConstants.ERROR_BAD_ARGUMENT;
import static org.mycore.oai.MCROAIConstants.ERROR_BAD_RESUMPTION_TOKEN;
import static org.mycore.oai.MCROAIConstants.ERROR_CANNOT_DISSEMINATE_FORMAT;
import static org.mycore.oai.MCROAIConstants.ERROR_NO_RECORDS_MATCH;
import static org.mycore.oai.MCROAIConstants.ERROR_NO_SET_HIERARCHY;
import static org.mycore.oai.MCROAIConstants.GRANULARITY;
import static org.mycore.oai.MCROAIConstants.V_EXCLUSIVE;
import static org.mycore.oai.MCROAIConstants.V_OPTIONAL;
import static org.mycore.oai.MCROAIConstants.V_REQUIRED;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;

import org.mycore.backend.hibernate.MCRHIBConnection;
import org.mycore.common.MCRConfiguration;
import org.mycore.parsers.bool.MCRAndCondition;
import org.mycore.services.fieldquery.MCRFieldDef;
import org.mycore.services.fieldquery.MCRHit;
import org.mycore.services.fieldquery.MCRQuery;
import org.mycore.services.fieldquery.MCRQueryCondition;
import org.mycore.services.fieldquery.MCRQueryManager;
import org.mycore.services.fieldquery.MCRResults;
import org.mycore.services.fieldquery.MCRSortBy;

/**
 * Provides common functionality for the ListRecords and ListIdentifiers
 * implementation.
 * 
 * @author Frank L\u00fctzenkirchen
 */
abstract class MCRListDataHandler extends MCRVerbHandler {
    void setAllowedParameters(Properties p) {
        p.setProperty(ARG_METADATA_PREFIX, V_REQUIRED);
        p.setProperty(ARG_FROM, V_OPTIONAL);
        p.setProperty(ARG_UNTIL, V_OPTIONAL);
        p.setProperty(ARG_SET, V_OPTIONAL);
        p.setProperty(ARG_RESUMPTION_TOKEN, V_EXCLUSIVE);
    }

    MCRListDataHandler(MCROAIDataProvider provider) {
        super(provider);
    }

    void handleRequest() {
        MCROAIResults oaires = null;

        String resumptionToken = parms.getProperty(ARG_RESUMPTION_TOKEN);
        if (resumptionToken != null) {
            oaires = MCROAIResults.getResults(resumptionToken);
            if (oaires == null)
                addError(ERROR_BAD_RESUMPTION_TOKEN, "Bad resumption token: " + resumptionToken);
            else
                oaires.addHits(this, resumptionToken);
        } else {
            String metadataPrefix = parms.getProperty(ARG_METADATA_PREFIX);
            MCRMetadataFormat metadataFormat = MCRMetadataFormat.getFormat(metadataPrefix);
            if ((metadataFormat == null) || (!provider.getAdapter().getMetadataFormats().contains(metadataFormat))) {
                String msg = "This metadata format is not supported by the repository: " + metadataPrefix;
                addError(ERROR_CANNOT_DISSEMINATE_FORMAT, msg);
                return;
            }

            MCRConfiguration config = MCRConfiguration.instance();

            MCRAndCondition queryCondition = new MCRAndCondition();
            if (restriction != null)
                queryCondition.addChild(restriction);

            String set = parms.getProperty(ARG_SET);
            if ((set != null)) {
                if (setURIs.isEmpty()) {
                    addError(ERROR_NO_SET_HIERARCHY, "This repository does not provide sets");
                    return;
                } else {
                    queryCondition.addChild(provider.getAdapter().buildSetCondition(set));
                }
            }

            String fieldFromUntil = config.getString(provider.getPrefix() + "Search.FromUntil", "modified");
            MCRFieldDef dateField = MCRFieldDef.getDef(fieldFromUntil);

            String from = parms.getProperty(ARG_FROM);
            if ((from != null) && checkDate(from))
                queryCondition.addChild(new MCRQueryCondition(dateField, ">=", from));

            String until = parms.getProperty(ARG_UNTIL);
            if ((until != null) && checkDate(until))
                queryCondition.addChild(new MCRQueryCondition(dateField, "<=", until));

            if ((from != null) && (until != null) && (from.compareTo(until) > 0))
                addError(ERROR_BAD_ARGUMENT, "The 'from' date must be less or equal the 'until' date");

            if (hasErrors())
                return;

            LOGGER.info("Searching for " + queryCondition.toString());

            MCRQuery query = new MCRQuery(queryCondition);

            List<MCRSortBy> sortBy = new ArrayList<MCRSortBy>();
            String searchSortBy = config.getString(provider.getPrefix() + "Search.SortBy", "modified descending, id descending");
            for (StringTokenizer st = new StringTokenizer(searchSortBy, ",;:"); st.hasMoreTokens();) {
                String token = st.nextToken().trim();
                MCRFieldDef field = MCRFieldDef.getDef(token.split(" ")[0]);
                boolean order = "ascending".equalsIgnoreCase(token.split(" ")[1]);
                sortBy.add(new MCRSortBy(field, order));
            }
            query.setSortBy(sortBy);

            MCRResults results = MCRQueryManager.search(query);
            LOGGER.info("Adding identifiers of deleted records to the result set");
            results = addDeletedItems(results, from, until);

            oaires = new MCROAIResults(results, metadataFormat, provider);
            if (!hasErrors())
                oaires.addHits(this);
        }
    }

    /**
     * Adds the identifiers of the deleted items to the given result.
     * 
     * @param source
     * @param from
     * @param until
     */
    private MCRResults addDeletedItems(MCRResults source, String from, String until) {
        MCRResults all = new MCRResults();

        for (MCRHit hit : source) {
            all.addHit(hit);
        }

        List<String> deletedItems = getDeletedItems(from, until);
        for (String deletedItemId : deletedItems) {
            all.addHit(new MCRHit(deletedItemId));
        }

        return all;
    }

    /**
     * @param from
     * @param until
     * @return returns the identifiers of the deleted items matching the given
     *         date boundaries
     */
    @SuppressWarnings("unchecked")
    public List<String> getDeletedItems(String from, String until) {
        LOGGER.info("Getting identifiers of deleted items");
        List<String> deletedItems = new Vector<String>();
        try {
            MCRHIBConnection conn = MCRHIBConnection.instance();
            String q = "SELECT DISTINCT identifier FROM mcrdeleteditems WHERE ";
            if (from != null && until != null) {
                q += "date_deleted >= '" + from + "' and date_deleted <= '" + until + "'";
            } else if (from != null) {
                q += "date_deleted >= '" + from + "'";
            } else if (until != null) {
                q += "date_deleted <= '" + until + "'";
            } else {
                q += "true";
            }
            deletedItems = conn.getSession().createSQLQuery(q).list();
        } catch (Exception ex) {
            LOGGER.warn("Could not retrieve identifiers of deleted objects", ex);
        }
        return deletedItems;
    }

    protected abstract void addHit(String ID, MCRMetadataFormat format);

    protected boolean checkDate(String value) {
        if (value.length() != GRANULARITY.length()) {
            addError(ERROR_BAD_ARGUMENT, "Bad date syntax: " + value);
            return false;
        }

        try {
            new SimpleDateFormat(DATESTAMP_PATTERN).parse(value);
        } catch (Exception ex) {
            addError(ERROR_BAD_ARGUMENT, "Cannot parse date, bad syntax: " + value);
            return false;
        }

        String earliest = provider.getAdapter().getEarliestDatestamp();
        if (value.compareTo(earliest) < 0) {
            addError(ERROR_NO_RECORDS_MATCH, "Earliest datestamp is " + earliest);
            return false;
        }

        return true;
    }
}
