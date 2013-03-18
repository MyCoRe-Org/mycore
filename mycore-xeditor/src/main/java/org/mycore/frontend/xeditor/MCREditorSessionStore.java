package org.mycore.frontend.xeditor;

import org.mycore.common.MCRCache;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;

public class MCREditorSessionStore {

    private static final String XEDITORS_CACHE_KEY = "XEditorsCache";

    private static final int maxEditorsInSession = 50;

    private static MCRCache<String, MCREditorSession> getEditorSessionCache() {
        MCRSession session = MCRSessionMgr.getCurrentSession();
        MCRCache<String, MCREditorSession> cache = (MCRCache<String, MCREditorSession>) (session.get(XEDITORS_CACHE_KEY));
        return (cache != null ? cache : createCacheInSession(session));
    }

    private static MCRCache<String, MCREditorSession> createCacheInSession(MCRSession session) {
        MCRCache<String, MCREditorSession> cache = new MCRCache<String, MCREditorSession>(maxEditorsInSession, XEDITORS_CACHE_KEY);
        session.put(XEDITORS_CACHE_KEY, cache);
        return cache;
    }

    public static void storeInSession(MCREditorSession es) {
        getEditorSessionCache().put(es.getID(), es);
    }

    public static MCREditorSession getFromSession(String id) {
        return (MCREditorSession) (getEditorSessionCache().get(id));
    }
}
