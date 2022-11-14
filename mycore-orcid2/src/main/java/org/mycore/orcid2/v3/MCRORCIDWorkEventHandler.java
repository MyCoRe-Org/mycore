/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
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

package org.mycore.orcid2.v3;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.content.MCRContent;
import org.mycore.datamodel.common.MCRXMLMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.orcid2.client.MCRORCIDClient;
import org.mycore.orcid2.user.MCRORCIDCredentials;
import org.mycore.orcid2.user.MCRORCIDUser;
import org.mycore.orcid2.v3.transformer.MCRORCIDWorkTransformer;
import org.orcid.jaxb.model.v3.release.record.summary.Works;
import org.orcid.jaxb.model.v3.release.record.Work;
import org.orcid.jaxb.model.v3.release.record.summary.WorkSummary;

public class MCRORCIDWorkEventHandler extends org.mycore.orcid2.MCRORCIDWorkEventHandler {

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    protected void publishToORCID(MCRObject object, MCRORCIDUser user) {
        final MCRORCIDCredentials credentials = user.getCredentials();
        if (credentials == null || credentials.getAccessToken() == null) {
            LOGGER.info("Skipping publish to orcid. User is not linked.");
            return;
        }
        final MCRORCIDClient memberClient = MCRORCIDAPIClientFactoryImpl.getInstance().createMemberClient(credentials);
        try {
            final Works works = memberClient.fetch(MCRORCIDSectionImpl.WORKS, Works.class);
            final List<WorkSummary> summaries
                = works.getWorkGroup().stream().flatMap(g -> g.getWorkSummary().stream()).toList();
            final MCRContent content = MCRXMLMetadataManager.instance().retrieveContent(object.getId());
            final Work transformedWork = MCRORCIDWorkTransformer.getInstance().transformToWork(content);
            final WorkSummary work = MCRORCIDWorkHelper.findMatchingSummaries(object, summaries).findFirst()
                .orElse(null);
            if (work != null) {
                // TODO check if need to set put code in model...
                memberClient.update(MCRORCIDSectionImpl.WORK, work.getPutCode(), transformedWork);
            } else {
                memberClient.create(MCRORCIDSectionImpl.WORK, transformedWork);
            }
        } catch (Exception ex) {
            LOGGER.warn("Could not publish {} in ORCID profile {} of user {}", object.getId(),
                credentials.getORCID(), user.getUser().getUserName(), ex);
        }
    }
}
