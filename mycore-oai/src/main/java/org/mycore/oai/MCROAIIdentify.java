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

import java.time.Instant;
import java.util.Collection;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.xml.MCRURIResolver;
import org.mycore.oai.pmh.DateUtils;
import org.mycore.oai.pmh.Description;
import org.mycore.oai.pmh.FriendsDescription;
import org.mycore.oai.pmh.Granularity;
import org.mycore.oai.pmh.Identify;
import org.mycore.oai.pmh.OAIIdentifierDescription;
import org.mycore.oai.pmh.SimpleIdentify;

/**
 * Simple MyCoRe implementation of a OAI-PMH {@link Identify} class. Uses the {@link MCRConfiguration} to retrieve
 * all important settings. Earliest date stamp is calculated with the 'restriction' query and sort by 'created'.
 * Also adds custom description elements from URIs configured by MCR.OAIDataProvider.OAI.DescriptionURI
 *
 * @author Matthias Eichner
 * @author Frank L\u00fctzenkirchen
 */
public class MCROAIIdentify extends SimpleIdentify {

    protected static final Logger LOGGER = LogManager.getLogger(MCROAIIdentify.class);

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
        String adminMail = this.config.getString(configPrefix + "AdminEmail", config.getString("MCR.Mail.Sender"));

        this.setEarliestDatestamp(calculateEarliestTimestamp());
        this.getAdminEmailList().add(adminMail);
        this.getDescriptionList().add(getIdentifierDescription());
        this.getDescriptionList().add(getFriendsDescription());

        addCustomDescriptions();
    }

    private void addCustomDescriptions() {
        for (final String descriptionURI : getDescriptionURIs())
            this.getDescriptionList().add(new CustomDescription(descriptionURI));
    }

    private Collection<String> getDescriptionURIs() {
        String descriptionConfig = getConfigPrefix() + "DescriptionURI";
        return MCRConfiguration.instance().getPropertiesMap(descriptionConfig).values();
    }

    class CustomDescription implements Description {

        private Element description;

        CustomDescription(String descriptionURI) {
            description = MCRURIResolver.instance().resolve(descriptionURI);
        }

        @Override
        public Element toXML() {
            return description.getChildren().get(0).clone();
        }

        @Override
        public void fromXML(Element description) {
            this.description = description;
        }
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
    protected Instant calculateEarliestTimestamp() {
        MCROAISearcher searcher = MCROAISearchManager.getSearcher(this, null, 1, null, null);
        return searcher.getEarliestTimestamp().orElse(DateUtils
            .parse(config.getString(this.configPrefix + "EarliestDatestamp", "1970-01-01")));
    }

}
