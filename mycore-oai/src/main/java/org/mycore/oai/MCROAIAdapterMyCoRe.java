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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.mycore.backend.hibernate.MCRHIBConnection;
import org.mycore.backend.hibernate.tables.MCRDELETEDITEMS;
import org.mycore.datamodel.common.MCRXMLMetadataManager;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.metadata.MCRObjectService;
import org.mycore.parsers.bool.MCRCondition;
import org.mycore.services.fieldquery.MCRFieldDef;
import org.mycore.services.fieldquery.MCRQuery;
import org.mycore.services.fieldquery.MCRQueryCondition;
import org.mycore.services.fieldquery.MCRQueryManager;
import org.mycore.services.fieldquery.MCRQueryParser;
import org.mycore.services.fieldquery.MCRResults;
import org.mycore.services.fieldquery.MCRSortBy;

public class MCROAIAdapterMyCoRe extends MCROAIAdapter {
    private final static Logger LOGGER = Logger.getLogger(MCROAIAdapterMyCoRe.class);

    @Override
    void init(String prefix) {
        super.init(prefix);
        EARLIEST_DATESTAMP = calculateEarliestTimestamp();
    }

    public boolean exists(String id) {
        try {
            MCRObjectID oid = MCRObjectID.getInstance(id);
            return MCRXMLMetadataManager.instance().exists(oid);
        } catch (Exception ex) {
            String msg = "Exception while checking existence of object " + id;
            LOGGER.warn(msg, ex);
            return false;
        }
    }

    public MCRCondition buildSetCondition(String setSpec) {
        String categID = setSpec.substring(setSpec.lastIndexOf(':') + 1).trim();
        String classID = setSpec.substring(0, setSpec.indexOf(':')).trim();
        String id = classID + ":" + categID;
        return new MCRQueryCondition(MCRFieldDef.getDef("category"), "=", id);
    }

    @Override
    public String formatURI(String uri, String id, MCRMetadataFormat format) {
        String objectType = MCRObjectID.getIDParts(id)[1];
        boolean exists = MCRMetadataManager.exists(MCRObjectID.getInstance(id));
        String value = super.formatURI(uri, id, format).replace("{objectType}", objectType)
                .replace(":{flag}", exists == false ? ":deletedMcrObject" : "");
        return value;
    }

    /**
     * Method returns a list with identifiers of the deleted objects within the
     * given date boundary. If the record policy indicates that there is not
     * support for tracking deleted items empty list is returned.
     * 
     * @return a list with identifiers of the deleted objects
     */
    @Override
    @SuppressWarnings("unchecked")
    List<String> getDeletedObjectsIdentifiers(String from, String until) {
        String policy = getDeletedRecordPolicy();
        if (MCROAIConstants.DELETED_RECORD_POLICY_NO.equalsIgnoreCase(policy)
                || MCROAIConstants.DELETED_RECORD_POLICY_TRANSIENT.equalsIgnoreCase(policy)) {
            return super.getDeletedObjectsIdentifiers(from, until);
        }
        LOGGER.info("Getting identifiers of deleted items");
        List<String> deletedItems = new Vector<String>();
        try {
            // the date formatter for parsing the from and until values
            SimpleDateFormat dateParser = new SimpleDateFormat("yyyy-MM-dd");

            // building the query
            MCRHIBConnection conn = MCRHIBConnection.instance();
            Criteria criteria = conn.getSession().createCriteria(MCRDELETEDITEMS.class);
            criteria.setProjection(Projections.property("id.identifier"));

            if (from != null && until != null) {
                Criterion lowerBound = Restrictions.ge("id.dateDeleted", dateParser.parse(from));
                Criterion upperBound = Restrictions.le("id.dateDeleted", modifyUntilDate(dateParser.parse(until)));
                criteria.add(Restrictions.and(lowerBound, upperBound));
            } else if (from != null) {
                criteria.add(Restrictions.ge("id.dateDeleted", dateParser.parse(from)));
            } else if (until != null) {
                criteria.add(Restrictions.le("id.dateDeleted", modifyUntilDate(dateParser.parse(until))));
            }

            deletedItems = criteria.list();
        } catch (Exception ex) {
            LOGGER.warn("Could not retrieve identifiers of deleted objects", ex);
        }
        return deletedItems;
    }

    /**
     * Calculates the earlies datestamp.
     * 
     * @return the create date of the oldest document within the repository
     */
    private String calculateEarliestTimestamp() {
        /* default value */
        String datestamp = "2000-01-01";
        try {
            MCRCondition condition = new MCRQueryParser().parse("objectType like *");
            List<MCRSortBy> sortByList = new Vector<MCRSortBy>();
            MCRSortBy sortBy = new MCRSortBy(MCRFieldDef.getDef("created"), MCRSortBy.ASCENDING);
            sortByList.add(sortBy);
            MCRQuery q = new MCRQuery(condition, sortByList, 1);
            MCRResults result = MCRQueryManager.search(q);
            if (result.getNumHits() > 0) {
                MCRObject obj = MCRMetadataManager.retrieveMCRObject(MCRObjectID.getInstance(result.getHit(0).getID()));
                Date dateCreated = obj.getService().getDate(MCRObjectService.DATE_TYPE_CREATEDATE);
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                sdf.setTimeZone(TimeZone.getDefault());
                datestamp = sdf.format(dateCreated);
            }
        } catch (Exception ex) {
            LOGGER.error("Error occured while examining create date of first created object", ex);
        }
        return datestamp;
    }
}
