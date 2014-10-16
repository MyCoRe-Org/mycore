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
import java.util.Map;

import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.criterion.Projections;
import org.mycore.backend.hibernate.MCRHIBConnection;
import org.mycore.backend.hibernate.tables.MCRDELETEDITEMS;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.oai.pmh.DateUtils;
import org.mycore.oai.pmh.FriendsDescription;
import org.mycore.oai.pmh.Granularity;
import org.mycore.oai.pmh.Identify;
import org.mycore.oai.pmh.OAIIdentifierDescription;
import org.mycore.oai.pmh.SimpleIdentify;

/**
 * Simple MyCoRe implementation of a OAI-PMH {@link Identify} class. Uses the {@link MCRConfiguration} to retrieve all important settings. Earliest date stamp
 * is calculated with the 'restriction' query and sort by 'created'.
 * 
 * @author Matthias Eichner
 * @author Frank L\u00fctzenkirchen
 */
public class MCROAIIdentify extends SimpleIdentify {

    protected final static Logger LOGGER = Logger.getLogger(MCROAIIdentify.class);

    private MCRConfiguration config;

    protected String configPrefix;

    public MCROAIIdentify(String baseURL, String configPrefix) {
        this.config = MCRConfiguration.instance();
        this.configPrefix = configPrefix;

        this.setBaseURL(baseURL);
        this.setRepositoryName(this.config.getString(configPrefix + "RepositoryName", "Undefined repository name"));
        String deletedRecordPolicy = this.config.getString(configPrefix + "DeletedRecord",
            DeletedRecordPolicy.Transient.name());
        this.setDeletedRecordPolicy(DeletedRecordPolicy.get(deletedRecordPolicy));
        String granularity = this.config.getString(configPrefix + "Granularity", Granularity.YYYY_MM_DD.name());
        this.setGranularity(Granularity.valueOf(granularity));
        String adminMail = this.config.getString(configPrefix + "AdminEmail",
            config.getString("MCR.Mail.Address", "no_mail_defined@oai_provider.com"));

        this.setEarliestDatestamp(calculateEarliestTimestamp());
        this.getAdminEmailList().add(adminMail);
        this.getDescriptionList().add(getIdentifierDescription());
        this.getDescriptionList().add(getFriendsDescription());
    }

    public OAIIdentifierDescription getIdentifierDescription() {
        String reposId = this.config.getString(this.configPrefix + "RepositoryIdentifier");
        String sampleId = this.config.getString(this.configPrefix + "RecordSampleID");
        return new OAIIdentifierDescription(reposId, sampleId);
    }

    public FriendsDescription getFriendsDescription() {
        FriendsDescription desc = new FriendsDescription();
        Map<String, String> friends = this.config.getPropertiesMap(this.configPrefix + "Friends.");
        desc.getFriendsList().addAll(friends.values());
        return desc;
    }

    public String getConfigPrefix() {
        return configPrefix;
    }

    /**
     * Calculates the earliest date stamp.
     * 
     * @return the create date of the oldest document within the repository
     */
    protected Date calculateEarliestTimestamp() {
        // default value
        Date datestamp = DateUtils.parseUTC(config.getString(this.configPrefix + "EarliestDatestamp", "1970-01-01"));
        try {
            // existing items
            datestamp = MCROAISearchManager.getSearcher(this.configPrefix, null, null, 1).getEarliestTimestamp();
            // deleted items
            if (DeletedRecordPolicy.Persistent.equals(this.getDeletedRecordPolicy())) {
                MCRHIBConnection conn = MCRHIBConnection.instance();
                Criteria criteria = conn.getSession().createCriteria(MCRDELETEDITEMS.class);
                criteria.setProjection(Projections.min("id.dateDeleted"));
                Date earliestDeletedDate = (Date) criteria.uniqueResult();
                if (earliestDeletedDate != null && earliestDeletedDate.compareTo(datestamp) < 0) {
                    datestamp = earliestDeletedDate;
                }
            }
        } catch (Exception ex) {
            LOGGER.warn("Error occured while examining create date of first created object. Use default value.", ex);
        }
        return datestamp;
    }

}
