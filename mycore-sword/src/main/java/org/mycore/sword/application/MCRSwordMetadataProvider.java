package org.mycore.sword.application;

import org.apache.abdera.Abdera;
import org.apache.abdera.model.Entry;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.frontend.MCRFrontendUtil;
import org.mycore.sword.MCRSwordConstants;
import org.mycore.sword.MCRSwordUtil;
import org.swordapp.server.DepositReceipt;
import org.swordapp.server.SwordError;

public abstract class MCRSwordMetadataProvider implements MCRSwordLifecycle {
    private MCRSwordLifecycleConfiguration lifecycleConfiguration;

    public abstract DepositReceipt provideMetadata(MCRObject object) throws SwordError;

    /**
     * @param id    the id of the MyCoReObject as String
     */
    public Entry provideListMetadata(MCRObjectID id) throws SwordError {
        Entry feedEntry = Abdera.getInstance().newEntry();

        feedEntry.setId(id.toString());
        MCRSwordUtil.BuildLinkUtil.getEditMediaIRIStream(lifecycleConfiguration.getCollection(), id.toString())
            .forEach(feedEntry::addLink);
        feedEntry.addLink(MCRFrontendUtil.getBaseURL() + MCRSwordConstants.SWORD2_EDIT_IRI
            + lifecycleConfiguration.getCollection() + "/" + id.toString(), "edit");
        return feedEntry;
    }

    @Override
    public void init(MCRSwordLifecycleConfiguration lifecycleConfiguration) {
        this.lifecycleConfiguration = lifecycleConfiguration;
    }
}
