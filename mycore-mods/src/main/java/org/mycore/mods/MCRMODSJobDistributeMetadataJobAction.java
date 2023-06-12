package org.mycore.mods;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Content;
import org.jdom2.Element;
import org.jdom2.filter.Filter;
import org.jdom2.filter.Filters;
import org.mycore.access.MCRAccessException;
import org.mycore.common.MCRConstants;
import org.mycore.common.MCRException;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRSystemUserInformation;
import org.mycore.datamodel.common.MCRLinkTableManager;
import org.mycore.datamodel.metadata.MCRMetaLinkID;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.frontend.support.MCRObjectIDLockTable;
import org.mycore.services.queuedjob.MCRJob;
import org.mycore.services.queuedjob.MCRJobAction;

public class MCRMODSJobDistributeMetadataJobAction extends MCRJobAction {

    public static final String OBJECT_ID_PARAMETER = "id";

    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * The constructor of the job action with specific {@link MCRJob}.
     *
     * @param job the job holding the parameters for the action
     */
    public MCRMODSJobDistributeMetadataJobAction(MCRJob job) {
        super(job);
    }

    public String getID() {
        return job.getParameter(OBJECT_ID_PARAMETER);
    }

    @Override
    public boolean isActivated() {
        return true;
    }

    @Override
    public String name() {
        return "Distribute Metadata of " + getID();
    }

    @Override
    public void execute() throws ExecutionException {
        MCRSession session = MCRSessionMgr.getCurrentSession();
        try {
            session.setUserInformation(MCRSystemUserInformation.getGuestInstance());
            session.setUserInformation(MCRSystemUserInformation.getJanitorInstance());
            MCRObjectID id = MCRObjectID.getInstance(getID());
            MCRObject holder = MCRMetadataManager.retrieveMCRObject(id);
            MCRMODSWrapper holderWrapper = new MCRMODSWrapper(holder);
            List<MCRMetaLinkID> children = holder.getStructure().getChildren();
            MCRMODSMetadataShareAgent agent = new MCRMODSMetadataShareAgent();
            distributeInheritedMetadata(holderWrapper, children, agent);
            distributeLinkedMetadata(holder, holderWrapper, agent);
        } finally {
            session.setUserInformation(MCRSystemUserInformation.getGuestInstance());
            session.setUserInformation(MCRSystemUserInformation.getSystemUserInstance());
        }
    }

    private void distributeLinkedMetadata(MCRObject holder, MCRMODSWrapper holderWrapper,
        MCRMODSMetadataShareAgent agent) {
        Collection<String> recipientIdsStr = MCRLinkTableManager.instance().getSourceOf(holder.getId(),
            MCRLinkTableManager.ENTRY_TYPE_REFERENCE);
        List<MCRObjectID> recipientIds = recipientIdsStr.stream()
            .map(MCRObjectID::getInstance)
            .filter(MCRMODSWrapper::isSupported).toList();
        runWithLockedObject(recipientIds, (recipientId) -> {
            LOGGER.info("distribute metadata to {}", recipientId);
            MCRObject recipient = MCRMetadataManager.retrieveMCRObject(recipientId);
            MCRMODSWrapper recipientWrapper = new MCRMODSWrapper(recipient);
            for (Element relatedItem : recipientWrapper.getLinkedRelatedItems()) {
                String holderId = relatedItem.getAttributeValue("href", MCRConstants.XLINK_NAMESPACE);
                if (holder.getId().toString().equals(holderId)) {
                    @SuppressWarnings("unchecked")
                    Filter<Content> sharedMetadata = (Filter<Content>) Filters.element("part",
                        MCRConstants.MODS_NAMESPACE).negate();
                    relatedItem.removeContent(sharedMetadata);
                    List<Content> newRelatedItemContent = holderWrapper.getMODS().cloneContent();
                    newRelatedItemContent.stream()
                        .filter(c -> c instanceof Element && ((Element) c).getName().equals("relatedItem"))
                        .map(Element.class::cast)
                        .forEach(Element::removeContent);
                    relatedItem.addContent(newRelatedItemContent);
                    LOGGER.info("Saving: {}", recipientId);
                    try {
                        agent.checkHierarchy(recipientWrapper);
                        MCRMetadataManager.update(recipient);
                    } catch (MCRPersistenceException | MCRAccessException e) {
                        throw new MCRPersistenceException("Error while updating shared metadata", e);
                    }
                }
            }
        });
    }

    private void distributeInheritedMetadata(MCRMODSWrapper holderWrapper, List<MCRMetaLinkID> children,
        MCRMODSMetadataShareAgent agent) {
        if (!children.isEmpty()) {
            LOGGER.info("Update inherited metadata");
            List<MCRObjectID> childIds = children.stream()
                .map(MCRMetaLinkID::getXLinkHrefID)
                .filter(MCRMODSWrapper::isSupported)
                .collect(Collectors.toList());
            runWithLockedObject(childIds, (childId) -> {
                LOGGER.info("Update: {}", childId);
                MCRObject child = MCRMetadataManager.retrieveMCRObject(childId);
                MCRMODSWrapper childWrapper = new MCRMODSWrapper(child);
                agent.inheritToChild(holderWrapper, childWrapper);
                LOGGER.info("Saving: {}", childId);
                try {
                    agent.checkHierarchy(childWrapper);
                    MCRMetadataManager.update(child);
                } catch (MCRPersistenceException | MCRAccessException e) {
                    throw new MCRPersistenceException("Error while updating inherited metadata", e);
                }
            });
        }
    }

    public void runWithLockedObject(List<MCRObjectID> objects, Consumer<MCRObjectID> lockedObjectConsumer) {
        try {
            // wait to get the lock for the object
            int maxTries = 10;
            List<MCRObjectID> notLocked = new ArrayList<>(objects);
            do {
                LOGGER.info("Try to lock {} objects", notLocked.size());
                objects.forEach(MCRObjectIDLockTable::lock);
                notLocked.removeIf(MCRObjectIDLockTable::isLockedByCurrentSession);
                if(!notLocked.isEmpty()){
                    LOGGER.info("Wait 1 minute for lock");
                    Thread.sleep(TimeUnit.MINUTES.toMillis(1));
                }
            } while (!notLocked.isEmpty() && maxTries-- > 0);
            objects.forEach(lockedObjectConsumer);
        } catch (InterruptedException e) {
            throw new MCRException("Error while waiting for object lock", e);
        } finally {
            objects.stream()
                    .filter(MCRObjectIDLockTable::isLockedByCurrentSession)
                    .forEach(MCRObjectIDLockTable::unlock);
        }
    }

    @Override
    public void rollback() {

    }
}
