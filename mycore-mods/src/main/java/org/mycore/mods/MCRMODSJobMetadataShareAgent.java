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
        MCRJob job = new MCRJob(MCRMODSJobDistributeMetadataJobAction.class);
        job.setParameter(MCRMODSJobDistributeMetadataJobAction.OBJECT_ID_PARAMETER, holder.getId().toString());

        Map<String, String> parameters = job.getParameters();
        MCRJobDAO jobDAO = MCRJobQueueManager.getInstance().getJobDAO();
        int count = jobDAO.getJobCount(MCRMODSJobDistributeMetadataJobAction.class, parameters,
            List.of(MCRJobStatus.NEW, MCRJobStatus.ERROR));

        if (count > 0) {
            LOGGER.info("Job for " + holder.getId() + " already exists. Skipping.");
            return;
        }

        MCRJobQueueManager.getInstance().getJobQueue(MCRMODSJobDistributeMetadataJobAction.class).add(job);
    }

}
