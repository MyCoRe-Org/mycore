package org.mycore.services.queuedjob;

import java.util.EventListener;

public interface MCRJobQueueEventListener extends EventListener {

    /**
     * Called when a job is added to the queue. If a job is rejected, this method is not called.
     * @param job the job that was added
     */
    void onJobAdded(MCRJob job);

}
