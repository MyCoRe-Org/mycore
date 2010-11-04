package org.mycore.frontend.editor;

import org.jdom.Element;
import org.mycore.common.MCRCache;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.events.MCRSessionEvent;
import org.mycore.common.events.MCRSessionListener;

/**
 * For each user session, the state of all editor forms most recently used
 * is kept in a cache. The number of editor form data that is kept is controlled
 * by the property MCR.EditorFramework.MaxEditorsInSession.
 */
public class MCREditorCache implements MCRSessionListener 
{
    private int maxEditors = MCRConfiguration.instance().getInt("MCR.EditorFramework.MaxEditorsInSession", 10);

    private final static String EDITOR_SESSIONS_KEY = "editorSessions";

    private final static MCREditorCache INSTANCE = new MCREditorCache();
    
    private MCREditorCache() {
        MCRSessionMgr.addSessionListener(this);
    }
    
    public static MCREditorCache instance() {
        return INSTANCE;
    }

    public Element getEditor(String editorSessionID) {
        return (Element) (getCacheForCurrentSession().get(editorSessionID));
    }
    
    public void putEditor(String editorSessionID, Element editor) {
        getCacheForCurrentSession().put(editorSessionID, editor);
    }

    private MCRCache getCacheForCurrentSession() {
        MCRSession currentSession = MCRSessionMgr.getCurrentSession();
        MCRCache cache = getCacheFromSession(currentSession);
        if (cache == null) {
            cache = createCacheInSession(currentSession);
        }
        return cache;
    }

    private MCRCache getCacheFromSession(MCRSession session) {
        return (MCRCache) session.get(EDITOR_SESSIONS_KEY);
    }

    private MCRCache createCacheInSession(MCRSession session) {
        String label = "Editor data in MCRSession " + session.getID();
        MCRCache cache = new MCRCache(maxEditors, label);
        session.put(EDITOR_SESSIONS_KEY, cache);
        return cache;
    }

    public void sessionEvent(MCRSessionEvent event) {
        if (event.getType() == MCRSessionEvent.Type.destroyed) {
            MCRSession session = event.getSession();
            closeCacheInSession(session);
        }
    }
      
    private void closeCacheInSession(MCRSession session) {
        MCRCache cache = getCacheFromSession(session);
        if (cache != null) {
            cache.close();
        }
    }
}
