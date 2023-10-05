package org.mycore.services.queuedjob.staticcontent;

import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.services.queuedjob.MCRJob;
import org.mycore.services.queuedjob.MCRJobAction;
import org.mycore.services.staticcontent.MCRObjectStaticContentGenerator;

import java.util.concurrent.ExecutionException;

public class MCRStaticContentGeneratorJobAction extends MCRJobAction {

    public static final String CONFIG_ID_PARAMETER = "configID";

    public static final String OBJECT_ID_PARAMETER = "objectID";

    /**
     * The constructor of the job action with specific {@link MCRJob}.
     *
     * @param job the job holding the parameters for the action
     */
    public MCRStaticContentGeneratorJobAction(MCRJob job) {
        super(job);
    }

    @Override
    public boolean isActivated() {
        return true;
    }

    @Override
    public String name() {
        return "Static Content - " + getConfigID();
    }

    private String getConfigID() {
        return this.job.getParameters().get(CONFIG_ID_PARAMETER);
    }

    private MCRObjectID getObjectID() {
        String objectIDStr = this.job.getParameters().get(OBJECT_ID_PARAMETER);
        return MCRObjectID.getInstance(objectIDStr);
    }

    @Override
    public void execute() throws ExecutionException {
        try {
            MCRObject object = MCRMetadataManager.retrieveMCRObject(getObjectID());
            MCRObjectStaticContentGenerator generator = new MCRObjectStaticContentGenerator(getConfigID());
            generator.generate(object);
        } catch (Exception e) {
            throw new ExecutionException(e);
        }
    }

    @Override
    public void rollback() {
        // do nothing
    }
}
