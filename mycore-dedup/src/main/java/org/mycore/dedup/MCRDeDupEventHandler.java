package org.mycore.dedup;


import org.jdom2.Element;
import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventHandlerBase;

import org.mycore.datamodel.metadata.MCRObject;


import org.mycore.dedup.jpa.MCRDeduplicationKeyManager;
import org.mycore.mods.MCRMODSWrapper;


public class MCRDeDupEventHandler extends MCREventHandlerBase {
    private final MCRDeDupCriteriaBuilder builder = new MCRDeDupCriteriaBuilder();

    @Override
    protected void handleObjectCreated(MCREvent evt, MCRObject obj) {
        updateDeDupCriteria(obj);
    }

    @Override
    protected void handleObjectUpdated(MCREvent evt, MCRObject obj) {
        updateDeDupCriteria(obj);
    }

    @Override
    protected void handleObjectRepaired(MCREvent evt, MCRObject obj) {

        updateDeDupCriteria(obj);
    }

    @Override
    protected void handleObjectDeleted(MCREvent evt, MCRObject obj) {
        MCRDeduplicationKeyManager.getInstance().clearDeduplicationKeys(obj.getId().toString());
        MCRDeduplicationKeyManager.getInstance().clearNoDuplicates(obj.getId().toString());
    }

    public void updateDeDupCriteria(MCRObject obj) {
        Element mods = new MCRMODSWrapper(obj).getMODS();
        MCRDeduplicationKeyManager.getInstance().updateDeDupCriteria(mods, obj.getId(), builder);
    }
}
