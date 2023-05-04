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

package org.mycore.services.queuedjob;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.backend.jpa.MCREntityManagerProvider;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.events.MCRStartupHandler;

import jakarta.servlet.ServletContext;
import org.mycore.util.concurrent.MCRTransactionableRunnable;

/**
 * Initializes the job queue on startup.
 * @author Sebastian Hofmann
 */
public class MCRJobQueueInitializer implements MCRStartupHandler.AutoExecutable {

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public String getName() {
        return getClass().getName();
    }

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public void startUp(ServletContext servletContext) {
        if (MCRConfiguration2.getBoolean("MCR.Persistence.Database.Enable").orElse(true)
            && MCREntityManagerProvider.getEntityManagerFactory() != null) {

            new MCRTransactionableRunnable(()-> {
                MCRJobQueueManager manager = MCRJobQueueManager.getInstance();

                manager.getJobDAO().getActions().stream().peek(q -> LOGGER.info("Initialize job queue {}", q))
                        .forEach(manager::getJobQueue);
            }).run();

        }
    }
}
