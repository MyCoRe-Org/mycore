/**
 * 
 */
package org.mycore.backend.hibernate;

import java.util.Date;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.mycore.backend.hibernate.tables.MCRDELETEDITEMS;
import org.mycore.backend.hibernate.tables.MCRDELETEDITEMSPK;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;

/**
 * @author shermann
 *
 */
public class MCRDeletedItemManager {

    private static MCRDeletedItemManager _instance;

    private static final Logger LOGGER = Logger.getLogger(MCRDeletedItemManager.class);

    private MCRDeletedItemManager() {
    }

    public static MCRDeletedItemManager getInstance() {
        if (_instance == null) {
            _instance = new MCRDeletedItemManager();
        }
        return _instance;
    }

    public synchronized final void addEntry(String identifier, Date dateDeleted) throws MCRPersistenceException {
        if (identifier == null) {
            throw new MCRPersistenceException("The identifier is null.");
        }
        if (dateDeleted == null) {
            throw new MCRPersistenceException("The deleted date parameter is null");
        }

        Session dataBaseSession = getSession();
        MCRDELETEDITEMSPK pk = new MCRDELETEDITEMSPK(identifier, dateDeleted);
        MCRSession httpSession = MCRSessionMgr.getCurrentSession();

        MCRDELETEDITEMS tab = (MCRDELETEDITEMS) dataBaseSession.get(MCRDELETEDITEMS.class, pk);
        if (tab == null) {
            tab = new MCRDELETEDITEMS();
            tab.setKey(pk);
        }
        tab.setUserid(httpSession.getCurrentUserID());
        tab.setIp(httpSession.getCurrentIP());

        LOGGER.debug("Inserting into MCRDELETEDITEMS table");
        dataBaseSession.save(tab);
    }

    private Session getSession() {
        return MCRHIBConnection.instance().getSession();
    }
}
