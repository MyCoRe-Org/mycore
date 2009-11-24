package org.mycore.datamodel.classifications;

import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;

public class MCRClassificationPoolFactory {
    private MCRClassificationPoolFactory() {
    }
    
    public static MCRClassificationPool getInstance() {
        MCRSession session = MCRSessionMgr.getCurrentSession();
        Object cp = session.get("MCRClassificationPool.instance");
        if (cp != null && cp instanceof MCRClassificationPool) {
            return (MCRClassificationPool) cp;
        }
        MCRClassificationPool classPool = new MCRClassificationPool();
        session.put("MCRClassificationPool.instance", classPool);
        return classPool;
    }
}
