/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mycore.mods;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.jdom2.Element;
import org.mycore.common.MCRSystemUserInformation;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.mcr.cronjob.MCRCronjob;
import org.mycore.solr.MCRSolrCoreManager;
import org.mycore.solr.auth.MCRSolrAuthenticationLevel;
import org.mycore.solr.auth.MCRSolrAuthenticationManager;
import org.mycore.util.concurrent.MCRFixedUserFailableRunnable;

/**
 * This Cronjob updates the embargo dates in the Database.
 */
public class MCRMODSEmbargoReleaseCronjob extends MCRCronjob {

    private static final Logger LOGGER = LogManager.getLogger();

    protected static final MCRSolrAuthenticationManager SOLR_AUTHENTICATION_MANAGER =
        MCRSolrAuthenticationManager.obtainInstance();

    @Override
    public String getDescription() {
        return "Embargo Object Updater";
    }

    @Override
    public void runJob() {

        if (MCRConfiguration2.getString("MCR.Solr.ServerURL").isEmpty()) {
            return;
        }

        try {

            new MCRFixedUserFailableRunnable<>(() -> {

                LOGGER.info("Searching embargoed objects");

                String today = LocalDate.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_DATE);
                String query = "mods.embargo.date:[* TO " + today + "]";

                LOGGER.info(() -> "Searching with query " + query);

                ModifiableSolrParams params = new ModifiableSolrParams();
                params.set("start", 0);
                params.set("rows", 100);
                params.set("fl", "id");
                params.set("q", query);

                QueryRequest queryRequest = new QueryRequest(params);
                SOLR_AUTHENTICATION_MANAGER.applyAuthentication(queryRequest, MCRSolrAuthenticationLevel.SEARCH);
                QueryResponse solrResponse = queryRequest.process(MCRSolrCoreManager.getMainSolrClient());
                solrResponse
                    .getResults()
                    .stream()
                    .map(result -> (String) result.get("id"))
                    .peek(id -> LOGGER.info("Found embargoed object " + id))
                    .forEach(this::releaseDocument);

            }, MCRSystemUserInformation.SUPER_USER).run();

        } catch (Exception e) {
            LOGGER.error("Failed to search embargoed objects", e);
        }

    }

    private void releaseDocument(String id) {

        try {

            LOGGER.info("Releasing embargoed object {}", id);

            MCRObjectID objectID = MCRObjectID.getInstance(id);
            MCRObject object = MCRMetadataManager.retrieveMCRObject(objectID);
            MCRMODSWrapper modsWrapper = new MCRMODSWrapper(object);
            String embargoXPATH = "mods:accessCondition[@type='embargo']";

            Optional<Element> element = Optional.ofNullable(modsWrapper.getElement(embargoXPATH));
            if (element.isPresent()) {
                element.get().setAttribute("type", "expiredEmbargo");
                MCRMetadataManager.update(object);
            }

        } catch (Exception e) {
            LOGGER.error(() -> "Failed to release embargoed object " + id, e);
        }

    }

}
