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

package org.mycore.mods;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRException;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRSystemUserInformation;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.frontend.support.MCRObjectIDLockTable;
import org.mycore.services.queuedjob.MCRJob;
import org.mycore.services.queuedjob.MCRJobAction;

public class MCRMODSDistributeMetadataJobAction extends MCRJobAction {

    public static final String OBJECT_ID_PARAMETER = "id";

    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * The constructor of the job action with specific {@link MCRJob}.
     *
     * @param job the job holding the parameters for the action
     */
    public MCRMODSDistributeMetadataJobAction(MCRJob job) {
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
    public void execute() {
        MCRSession session = MCRSessionMgr.getCurrentSession();
        try {
            session.setUserInformation(MCRSystemUserInformation.getGuestInstance());
            session.setUserInformation(MCRSystemUserInformation.getJanitorInstance());
            MCRObjectID id = MCRObjectID.getInstance(getID());
            MCRObject holder = MCRMetadataManager.retrieveMCRObject(id);
            MCRMODSWrapper holderWrapper = new MCRMODSWrapper(holder);
            MCRMODSMetadataShareAgent agent = new MCRMODSMetadataShareAgent() {
                @Override
                protected void runWithLockedObject(List<MCRObjectID> objects,
                    Consumer<MCRObjectID> lockedObjectConsumer) {
                    MCRMODSDistributeMetadataJobAction.this.runWithLockedObject(objects, lockedObjectConsumer);
                }
            };

            agent.distributeInheritedMetadata(holderWrapper);
            agent.distributeLinkedMetadata(holderWrapper);
        } finally {
            session.setUserInformation(MCRSystemUserInformation.getGuestInstance());
            session.setUserInformation(MCRSystemUserInformation.getSystemUserInstance());
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
                if (!notLocked.isEmpty()) {
                    LOGGER.info("Wait 1 minute for lock");
                    Thread.sleep(TimeUnit.MINUTES.toMillis(1));
                }
            } while (!notLocked.isEmpty() && maxTries-- > 0);
            objects.forEach(lockedObjectConsumer);
        } catch (InterruptedException e) {
            throw new MCRException("Error while waiting for object lock", e);
        } finally {
            objects.stream().filter(MCRObjectIDLockTable::isLockedByCurrentSession)
                .forEach(MCRObjectIDLockTable::unlock);
        }
    }

    @Override
    public void rollback() {
        // nothing to do
    }
}
