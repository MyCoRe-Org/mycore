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

package org.mycore.services.packaging;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.access.MCRAccessException;
import org.mycore.common.MCRException;
import org.mycore.common.MCRUsageException;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.services.queuedjob.MCRJob;
import org.mycore.services.queuedjob.MCRJobQueue;

/**
 * <p>Used to pack packages in a specific format, using {@link MCRJobQueue}.</p>
 * <p>You have to define a packer id and assign a packer class which extends {@link MCRPacker}.</p>
 * <code>
 * MCR.Packaging.Packer.MyPackerID.Class = org.mycore.packaging.MyPackerClass
 * </code>
 * <p>
 * <p>You now have to pass properties required by the packer:</p>
 * <code>
 * MCR.Packaging.Packer.MyPackerID.somePropertyForPacker = value
 * </code>
 * <p>
 *
 * @author Sebastian Hofmann (mcrshofm)
 */
public class MCRPackerManager {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final MCRJobQueue PACKER_JOB_QUEUE = initializeJobQueue();

    private static MCRJobQueue initializeJobQueue() {
        LOGGER.info("Initializing jobQueue for Packaging!");
        return MCRJobQueue.getInstance(MCRPackerJobAction.class);
    }

    /**
     * Creates and starts a new PackagingJob.
     * <p>The rights you need to start a Packer depends on the implementation!</p>
     * @param jobParameters the parameters which will be passed to the job. (Should include a packer)
     * @return the created MCRJob
     * @throws MCRUsageException if invalid parameters are passed to the packer
     * @throws MCRAccessException if the current user doesn't have the rights to use the packer(on a specific  object).
     */
    public static MCRJob startPacking(Map<String, String> jobParameters) throws MCRUsageException, MCRAccessException {
        String packer = jobParameters.get("packer");
        if (packer == null) {
            LOGGER.error("No Packer parameter found!");
            return null;
        }

        checkPacker(packer, jobParameters);

        MCRJob mcrJob = new MCRJob(MCRPackerJobAction.class);
        mcrJob.setParameters(jobParameters);

        if (!PACKER_JOB_QUEUE.offer(mcrJob)) {
            throw new MCRException("Could not add Job to Queue!");
        }

        return mcrJob;
    }

    private static void checkPacker(String packer, Map<String, String> jobParameters)
        throws MCRUsageException, MCRAccessException {
        MCRPacker instance = MCRConfiguration.instance().getInstanceOf("MCR.Packaging.Packer." + packer + ".Class");
        instance.setParameter(jobParameters);
        instance.setConfiguration(MCRPackerJobAction.getConfiguration(packer));
        instance.checkSetup();
    }
}
