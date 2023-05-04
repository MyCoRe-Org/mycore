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

/**
 * The default implementation for {@link MCRJobStatusListener}
 *
 * @author shermann
 * */
public class MCRDefaultJobStatusListener implements MCRJobStatusListener {

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public void onProcessing(MCRJob job) {
        LOGGER.debug("Processing {}", job.getAction().getName());
    }

    @Override
    public void onSuccess(MCRJob job) {
        LOGGER.debug("Finished {}", job.getAction().getName());
    }

    @Override
    public void onError(MCRJob job) {
        LOGGER.debug("Error {}", job.getAction().getName());
    }
}
