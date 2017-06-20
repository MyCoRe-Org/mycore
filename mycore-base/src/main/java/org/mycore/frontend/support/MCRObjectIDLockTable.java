package org.mycore.frontend.support;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.events.MCRSessionEvent;
import org.mycore.common.events.MCRSessionListener;
import org.mycore.datamodel.metadata.MCRObjectID;

public class MCRObjectIDLockTable implements MCRSessionListener {

    private static final MCRObjectIDLockTable SINGLETON = new MCRObjectIDLockTable();

    private static Logger LOGGER = LogManager.getLogger(MCRObjectIDLockTable.class);

    private ConcurrentMap<MCRObjectID, MCRSession> lockMap;

    private static MCRObjectIDLockTable getInstance() {
        return SINGLETON;
    }

    private MCRObjectIDLockTable() {
        this.lockMap = new ConcurrentHashMap<MCRObjectID, MCRSession>();
        MCRSessionMgr.addSessionListener(this);
    }

    public void clearTable(MCRSession session) {
        for (MCRObjectID objectID : lockMap.keySet()) {
            lockMap.remove(objectID, session);
        }
    }

    public static void unlock(MCRObjectID objectId) {
        getInstance().lockMap.remove(objectId);
    }

    public static void lock(MCRObjectID objectId) {
        getInstance().lockMap.putIfAbsent(objectId, MCRSessionMgr.getCurrentSession());
    }

    public static boolean isLockedByCurrentSession(MCRObjectID objectId) {
        MCRSession session = MCRSessionMgr.getCurrentSession();
        return session.equals(getInstance().lockMap.get(objectId));
    }

    public static MCRSession getLocker(MCRObjectID objectId) {
        return getInstance().lockMap.get(objectId);
    }

    @Override
    public void sessionEvent(MCRSessionEvent event) {
        switch (event.getType()) {
            case destroyed:
                clearTable(event.getSession());
                break;
            default:
                LOGGER.debug("Skipping event: " + event.getType());
                break;
        }
    }

    public static boolean isLockedByCurrentSession(String objectId) {
        MCRObjectID objId = MCRObjectID.getInstance(objectId);
        return MCRObjectIDLockTable.isLockedByCurrentSession(objId);
    }

    public static String getLockingUserName(String objectId) {
        MCRObjectID objId = MCRObjectID.getInstance(objectId);
        MCRSession locker = MCRObjectIDLockTable.getLocker(objId);
        if (locker == null) {
            return null;
        }
        return locker.getUserInformation().getUserID();
    }
}
