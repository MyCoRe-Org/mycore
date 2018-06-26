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
import java.util.Optional;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.servlet.ServletContext;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.jdom2.Element;
import org.mycore.access.MCRAccessException;
import org.mycore.common.MCRSystemUserInformation;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.events.MCRStartupHandler;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.solr.MCRSolrClientFactory;
import org.mycore.util.concurrent.MCRFixedUserCallable;

/**
 * This event handler updates the embargo dates in the Database.
 */
public class MCRMODSEmbargoCronjob extends TimerTask implements MCRStartupHandler.AutoExecutable {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final int TIMER_TASK_PERIOD = getTimerPause();// * 60 * 3;

    private static final int RELEASE_THREAD_COUNT = 3;

    private static final ExecutorService EXECUTOR_SERVICE = Executors.newFixedThreadPool(RELEASE_THREAD_COUNT);

    private static int getTimerPause() {
        return MCRConfiguration.instance().getInt("MCR.MODS.Embargo.Job.Schedule.WaitInMinutes") * 1000 * 60;
    }

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
        final SolrClient solrClient = MCRSolrClientFactory.getMainSolrClient();
        final ModifiableSolrParams params = new ModifiableSolrParams();
        final LocalDate today = LocalDate.now();
        final String todayString = today.format(DateTimeFormatter.ISO_DATE);

        params.set("start", 0);
        params.set("rows", Integer.MAX_VALUE - 1);
        params.set("fl", "id");
        params.set("q", "mods.embargo.date:[* TO " + todayString + "]");

        try {
            final QueryResponse response = solrClient.query(params);
            Set<MCRFixedUserCallable<Boolean>> releaseCallables = response.getResults().stream()
                .map(result -> (String) result.get("id"))
                .map(MCRObjectID::getInstance)
                .map(id -> new MCRFixedUserCallable<>(() -> {
                    objectReleaser.accept(id);
                    return true;
                }, MCRSystemUserInformation.getSuperUserInstance())).collect(Collectors.toSet());

            EXECUTOR_SERVICE.invokeAll(releaseCallables);
        } catch (SolrServerException | IOException | InterruptedException e) {
            LOGGER.error("Error while searching embargo documents!", e);
        }
    }

    private void releaseDocument(MCRObjectID objectID) {
        try {
            LOGGER.info("Release object {}", objectID);
            final MCRObject object = MCRMetadataManager.retrieveMCRObject(objectID);
            final MCRMODSWrapper modsWrapper = new MCRMODSWrapper(object);
            final String embargoXPATH = "mods:accessCondition[@type='embargo']";

            Optional<Element> element = Optional.ofNullable(modsWrapper.getElement(embargoXPATH));
            if (element.isPresent()) {
                element.get().setAttribute("type", "expiredEmbargo");
                MCRMetadataManager.update(object);
            }
        } catch (MCRAccessException e) {
            LOGGER.error("Error while releasing embargo document!", e);
        }
    }

    @Override
    public void run() {
        LOGGER.info("Running " + getName() + "..");
        searchDocumentsToRelease(this::releaseDocument);
    }
}

