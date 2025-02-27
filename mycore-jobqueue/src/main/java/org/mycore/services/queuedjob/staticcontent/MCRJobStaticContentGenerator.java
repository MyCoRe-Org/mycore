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

package org.mycore.services.queuedjob.staticcontent;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.services.queuedjob.MCRJob;
import org.mycore.services.queuedjob.MCRJobQueueManager;
import org.mycore.services.queuedjob.MCRJobStatus;
import org.mycore.services.staticcontent.MCRObjectStaticContentGenerator;

public class MCRJobStaticContentGenerator extends MCRObjectStaticContentGenerator {

    private static final Logger LOGGER = LogManager.getLogger();

    public MCRJobStaticContentGenerator(String configID) {
        super(configID);
    }

    @Override
    public void generate(MCRObject object) throws IOException {
        Map<String, String> parameters = new HashMap<>();
        parameters.put(MCRStaticContentGeneratorJobAction.CONFIG_ID_PARAMETER, configID);
        parameters.put(MCRStaticContentGeneratorJobAction.OBJECT_ID_PARAMETER, object.getId().toString());

        MCRJobQueueManager jobQueueManager = MCRJobQueueManager.getInstance();

        int jobCount = jobQueueManager.getJobDAO().getJobCount(MCRStaticContentGeneratorJobAction.class, parameters,
            List.of(MCRJobStatus.NEW, MCRJobStatus.ERROR));

        if (jobCount == 0) {
            MCRJob job = new MCRJob(MCRStaticContentGeneratorJobAction.class);
            job.setParameters(parameters);
            jobQueueManager.getJobQueue(MCRStaticContentGeneratorJobAction.class).add(job);
        } else {
            LOGGER.info("There is already a generator job for the object {} and the config {}. Skipping generation.",
                object::getId, () -> configID);
        }
    }
}
