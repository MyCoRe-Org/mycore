package org.mycore.services.queuedjob;

import org.mycore.common.MCRException;

import java.util.Random;
import java.util.concurrent.ExecutionException;

public class MCRTestJobAction extends MCRJobAction {

    public MCRTestJobAction(MCRJob job) {
        super(job);
    }

    @Override
    public boolean isActivated() {
        return true;
    }

    @Override
    public String name() {
        return "Test job";
    }

    @Override
    public void execute() throws ExecutionException {
        Random random = new Random();

        try {
            Thread.sleep(random.nextInt(10000));
        } catch (InterruptedException e) {
            throw new MCRException(e);
        }
        if(random.nextBoolean()){
            throw new MCRException("Test exception");
        }
    }

    @Override
    public void rollback() {
        // nothing to do
    }
}
