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

import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRPersistenceException;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.services.queuedjob.MCRJob;
import org.mycore.services.queuedjob.MCRJobDAO;
import org.mycore.services.queuedjob.MCRJobQueueManager;
import org.mycore.services.queuedjob.MCRJobStatus;

public class MCRMODSJobMetadataShareAgent extends MCRMODSMetadataShareAgent {

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public void distributeMetadata(MCRObject holder) throws MCRPersistenceException {
        MCRJob job = new MCRJob(MCRMODSDistributeMetadataJobAction.class);
        job.setParameter(MCRMODSDistributeMetadataJobAction.OBJECT_ID_PARAMETER, holder.getId().toString());

        Map<String, String> parameters = job.getParameters();
        MCRJobDAO jobDAO = MCRJobQueueManager.getInstance().getJobDAO();
        int count = jobDAO.getJobCount(MCRMODSDistributeMetadataJobAction.class, parameters,
            List.of(MCRJobStatus.NEW, MCRJobStatus.ERROR));

        if (count > 0) {
            LOGGER.info("Job for " + holder.getId() + " already exists. Skipping.");
            return;
        }

        MCRJobQueueManager.getInstance().getJobQueue(MCRMODSDistributeMetadataJobAction.class).add(job);
    }

}
