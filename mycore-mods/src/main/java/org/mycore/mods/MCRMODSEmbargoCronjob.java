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

package org.mycore.mods;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;

import javax.servlet.ServletContext;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.mycore.access.MCRAccessException;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRSystemUserInformation;
import org.mycore.common.events.MCRStartupHandler;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.solr.MCRSolrClientFactory;

/**
 * This event handler updates the embargo dates in the Database.
 */
public class MCRMODSEmbargoCronjob extends TimerTask implements MCRStartupHandler.AutoExecutable {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final int TIMER_TASK_PERIOD = 1000 * 60;// * 60 * 3;

    @Override
    public String getName() {
        return "Embargo Object Updater";
    }

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public void startUp(ServletContext servletContext) {
        final Timer t = new Timer();
        t.scheduleAtFixedRate(this, 0, TIMER_TASK_PERIOD);
    }

    private void searchDocumentsToRelease(Consumer<MCRObjectID> objectReleaser) {
        final SolrClient solrClient = MCRSolrClientFactory.getSolrClient();
        final ModifiableSolrParams params = new ModifiableSolrParams();
        final LocalDate today = LocalDate.now();
        params.set("start", 0);
        params.set("rows", Integer.MAX_VALUE - 1);
        params.set("fl", "id");

        final String todayString = today.format(DateTimeFormatter.ISO_DATE);

        params.set("q", "mods.embargo.date:[* TO " + todayString + "]");
        try {
            final QueryResponse response = solrClient.query(params);
            response.getResults().stream()
                .map(result -> (String) result.get("id"))
                .map(MCRObjectID::getInstance)
                .forEach(objectReleaser);
        } catch (SolrServerException | IOException e) {
            LOGGER.error("Error while searching embargo documents!", e);
        }
    }

    private void releaseDocument(MCRObjectID objectID) {
        final MCRSession session = MCRSessionMgr.getCurrentSession();
        try {
            LOGGER.info("Release object {}", objectID);
            session.setUserInformation(MCRSystemUserInformation.getSuperUserInstance());
            final MCRObject object = MCRMetadataManager.retrieveMCRObject(objectID);
            final MCRMODSWrapper modsWrapper = new MCRMODSWrapper(object);
            final String embargoXPATH = "mods:accessCondition[@type='embargo']";
            final String embargoString = modsWrapper.getElement(embargoXPATH).getTextNormalize();
            modsWrapper.removeElements(embargoXPATH);
            modsWrapper.addElement("mods:accessCondition")
                .setAttribute("type", "expiredEmbargo")
                .setText(embargoString);
            MCRMetadataManager.update(object);
        } catch (MCRAccessException e) {
            LOGGER.error("Error while releasing embargo document!", e);
        } finally {
            session.close();
        }
    }

    @Override
    public void run() {
        LOGGER.info("Running " + getName() + "..");
        searchDocumentsToRelease(this::releaseDocument);
    }
}

