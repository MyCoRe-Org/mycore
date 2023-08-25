package org.mycore.services.queuedjob.staticcontent;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

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
        HashMap<String, String> parameters = new HashMap<>();
        parameters.put(MCRStaticContentGeneratorJobAction.CONFIG_ID_PARAMETER, configID);
        parameters.put(MCRStaticContentGeneratorJobAction.OBJECT_ID_PARAMETER, object.getId().toString());

        MCRJobQueueManager jobQueueManager = MCRJobQueueManager.getInstance();

        List<MCRJob> jobs = jobQueueManager
            .getJobDAO()
            .getJobs(MCRStaticContentGeneratorJobAction.class, parameters,
                List.of(MCRJobStatus.NEW, MCRJobStatus.ERROR, MCRJobStatus.PROCESSING), null, null);

        if (jobs.isEmpty()) {
            MCRJob job = new MCRJob(MCRStaticContentGeneratorJobAction.class);
            job.setParameters(parameters);
            jobQueueManager.getJobQueue(MCRStaticContentGeneratorJobAction.class).add(job);
        } else {
            LOGGER.info("There is already a generator job for the object {} and the config {}. Skipping generation.",
                object.getId(), configID);
        }
    }
}
