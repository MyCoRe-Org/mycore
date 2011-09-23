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

import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.mycore.common.MCRConfiguration;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.metadata.MCRObjectService;
import org.mycore.oai.pmh.DateUtils;
import org.mycore.oai.pmh.FriendsDescription;
import org.mycore.oai.pmh.Granularity;
import org.mycore.oai.pmh.Identify;
import org.mycore.oai.pmh.OAIIdentifierDescription;
import org.mycore.oai.pmh.SimpleIdentify;
import org.mycore.parsers.bool.MCRCondition;
import org.mycore.services.fieldquery.MCRFieldDef;
import org.mycore.services.fieldquery.MCRQuery;
import org.mycore.services.fieldquery.MCRQueryManager;
import org.mycore.services.fieldquery.MCRQueryParser;
import org.mycore.services.fieldquery.MCRResults;
import org.mycore.services.fieldquery.MCRSortBy;

/**
 * Simple MyCoRe implementation of a OAI-PMH {@link Identify} class. Uses the {@link MCRConfiguration} to retrieve all important settings. Earliest date stamp
 * is calculated with query 'objectType like *' and sort by 'created'.
 * 
 * @author Matthias Eichner
 * @author Frank L\u00fctzenkirchen
 */
public class MCROAIIdentify extends SimpleIdentify {

    protected final static Logger LOGGER = Logger.getLogger(MCROAIIdentify.class);

    private MCRConfiguration config;

    private String configPrefix;

    public MCROAIIdentify(String baseURL, String configPrefix) {
        this.config = MCRConfiguration.instance();
        this.configPrefix = configPrefix;

        this.baseURL = baseURL;
        this.repositoryName = this.config.getString(configPrefix + "RepositoryName", "Undefined repository name");
        this.earliestDatestamp = calculateEarliestTimestamp();
        String sDelRecPol = this.config.getString(configPrefix + "DeletedRecord", "transient");
        this.deletedRecordPolicy = DeletedRecordPolicy.get(sDelRecPol);
        this.granularity = Granularity.YYYY_MM_DD;
        String adminMail = this.config.getString(configPrefix + "AdminEmail", config.getString("MCR.Mail.Address"));
        if (adminMail == null || adminMail.equals("")) {
            adminMail = "no_mail_defined@oai_provider.com";
        }
        this.adminEmailList.add(adminMail);
        this.descriptionList.add(getIdentifierDescription());
        this.descriptionList.add(getFriendsDescription());
    }

    public OAIIdentifierDescription getIdentifierDescription() {
        String reposId = this.config.getString(this.configPrefix + "RepositoryIdentifier");
        String sampleId = this.config.getString(this.configPrefix + "RecordSampleID");
        return new OAIIdentifierDescription(reposId, sampleId);
    }

    public FriendsDescription getFriendsDescription() {
        FriendsDescription desc = new FriendsDescription();
        Properties friends = this.config.getProperties(this.configPrefix + "Friends.");
        for (@SuppressWarnings("rawtypes")
        Iterator it = friends.values().iterator(); it.hasNext();) {
            String friend = (String) (it.next());
            desc.getFriendsList().add(friend);
        }
        return desc;
    }

    /**
     * Calculates the earliest date stamp.
     * 
     * @return the create date of the oldest document within the repository
     */
    private Date calculateEarliestTimestamp() {
        /* default value */
        Date datestamp = DateUtils.parseUTC(config.getString(this.configPrefix + "EarliestDatestamp", "1970-01-01"));
        try {
            MCRCondition condition = new MCRQueryParser().parse("objectType like *");
            List<MCRSortBy> sortByList = new Vector<MCRSortBy>();
            MCRSortBy sortBy = new MCRSortBy(MCRFieldDef.getDef("created"), MCRSortBy.ASCENDING);
            sortByList.add(sortBy);
            MCRQuery q = new MCRQuery(condition, sortByList, 1);
            MCRResults result = MCRQueryManager.search(q);
            if (result.getNumHits() > 0) {
                MCRObject obj = MCRMetadataManager.retrieveMCRObject(MCRObjectID.getInstance(result.getHit(0).getID()));
                datestamp = obj.getService().getDate(MCRObjectService.DATE_TYPE_CREATEDATE);
            }
        } catch (Exception ex) {
            LOGGER.error("Error occured while examining create date of first created object", ex);
        }
        return datestamp;
    }
}
