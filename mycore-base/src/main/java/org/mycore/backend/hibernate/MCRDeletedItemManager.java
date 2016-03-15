/**
 * 
 */
package org.mycore.backend.hibernate;

import java.util.Date;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.mycore.backend.jpa.deleteditems.MCRDELETEDITEMS;
import org.mycore.backend.jpa.deleteditems.MCRDELETEDITEMSPK;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;

/**
 * @author shermann
 */
public class MCRDeletedItemManager {

    private static MCRDeletedItemManager _instance = new MCRDeletedItemManager();

    private static final Logger LOGGER = Logger.getLogger(MCRDeletedItemManager.class);

    private MCRDeletedItemManager() {
    }

    public static MCRDeletedItemManager getInstance() {
        return _instance;
    }

    /**
     * @param identifier
     *            the identifier of the MCRObject or MCRDerivate
     * @param dateDeleted
     *            the current date
     */
    public synchronized final void addEntry(String identifier, Date dateDeleted) throws MCRPersistenceException {
        if (identifier == null) {
            throw new MCRPersistenceException("The identifier is null.");
        }
        if (dateDeleted == null) {
            throw new MCRPersistenceException("The deleted date parameter is null");
        }

        Session dataBaseSession = getSession();
        MCRDELETEDITEMSPK pk = new MCRDELETEDITEMSPK(identifier, dateDeleted);
        MCRSession mcrSession = MCRSessionMgr.getCurrentSession();

        MCRDELETEDITEMS tab = (MCRDELETEDITEMS) dataBaseSession.get(MCRDELETEDITEMS.class, pk);
        if (tab == null) {
            tab = new MCRDELETEDITEMS();
            tab.setKey(pk);
        }
        tab.setUserid(mcrSession.getUserInformation().getUserID());
        tab.setIp(mcrSession.getCurrentIP());

        LOGGER.debug("Inserting into MCRDELETEDITEMS table");
        dataBaseSession.save(tab);
    }

    private Session getSession() {
        return MCRHIBConnection.instance().getSession();
    }
}
