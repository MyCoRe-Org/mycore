package org.mycore.services.packaging;


import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRException;
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
 * <p><b>The user needs the permission packer-MyPackerID to create a package!</b></p>
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
     *
     * @param jobParameters the parameters which will be passed to the job. (Should include a packer)
     * @return the created MCRJob
     */
    public static MCRJob startPacking(Map<String, String> jobParameters) {
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

    private static void checkPacker(String packer, Map<String, String> jobParameters) {
        MCRPacker instance = MCRConfiguration.instance().getInstanceOf("MCR.Packaging.Packer." + packer + ".Class");
        instance.setParameter(jobParameters);
        //TODO: verify parameters, before creating an MCRJob!
    }
}
