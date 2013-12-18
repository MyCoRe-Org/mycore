package org.mycore.frontend.editor;

import org.mycore.common.MCRCache;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.events.MCRSessionEvent;
import org.mycore.common.events.MCRSessionListener;

/**
 * For each user session, the state of all editor forms most recently used is
 * kept in a cache. The number of editor form data that is kept is controlled by
 * the property MCR.EditorFramework.MaxEditorsInSession.
 */
public class MCREditorSessionCache implements MCRSessionListener {

    private final static String maxEditorsInSessionProperty = "MCR.EditorFramework.MaxEditorsInSession";

    private final static int maxEditorsInSessionDefault = 10;

    private int maxEditorsInSession;

    private final static String EDITOR_SESSIONS_KEY = "editorSessions";

    private final static MCREditorSessionCache INSTANCE = new MCREditorSessionCache();

    public static MCREditorSessionCache instance() {
        return INSTANCE;
    }

    private MCREditorSessionCache() {
        MCRSessionMgr.addSessionListener(this);
        maxEditorsInSession = MCRConfiguration.instance().getInt(maxEditorsInSessionProperty, maxEditorsInSessionDefault);
    }

    public MCREditorSession getEditorSession(String id) {
        return (MCREditorSession) (getOrCreateCacheInCurrentSession().get(id));
    }

    public void storeEditorSession(MCREditorSession editor) {
        getOrCreateCacheInCurrentSession().put(editor.getID(), editor);
    }

    private MCRCache getOrCreateCacheInCurrentSession() {
        MCRSession currentSession = MCRSessionMgr.getCurrentSession();
        MCRCache cache = getCacheFromMCRSession(currentSession);
        if (cache == null) {
            cache = createCacheInMCRSession(currentSession);
        }
        return cache;
    }

    private MCRCache getCacheFromMCRSession(MCRSession session) {
        return (MCRCache) session.get(EDITOR_SESSIONS_KEY);
    }

    private MCRCache createCacheInMCRSession(MCRSession session) {
        String label = "Editor data in MCRSession " + session.getID();
        MCRCache cache = new MCRCache(maxEditorsInSession, label);
        session.put(EDITOR_SESSIONS_KEY, cache);
        return cache;
    }

    public void sessionEvent(MCRSessionEvent event) {
        if (event.getType() == MCRSessionEvent.Type.destroyed) {
            MCRSession session = event.getSession();
            closeCacheInMCRSession(session);
        }
    }

    private void closeCacheInMCRSession(MCRSession session) {
        MCRCache cache = getCacheFromMCRSession(session);
        if (cache != null) {
            cache.close();
        }
    }
}
