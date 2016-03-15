package org.mycore.backend.jpa.deleteditems;

import java.util.Date;

import javax.persistence.EntityManager;

import org.apache.logging.log4j.LogManager;
import org.mycore.backend.jpa.MCREntityManagerProvider;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;

public class MCRDeletedItemManager {

    /**
     * @param identifier
     *            the identifier of the MCRObject or MCRDerivate
     * @param dateDeleted
     *            the current date
     */
    public static final void addEntry(String identifier, Date dateDeleted) throws MCRPersistenceException {
        if (identifier == null) {
            throw new MCRPersistenceException("The identifier is null.");
        }
        if (dateDeleted == null) {
            throw new MCRPersistenceException("The deleted date parameter is null");
        }

        EntityManager entityManager = MCREntityManagerProvider.getCurrentEntityManager();
        MCRDELETEDITEMSPK pk = new MCRDELETEDITEMSPK(identifier, dateDeleted);
        MCRSession mcrSession = MCRSessionMgr.getCurrentSession();

        MCRDELETEDITEMS tab = entityManager.find(MCRDELETEDITEMS.class, pk);
        boolean create = tab == null;
        if (create) {
            tab = new MCRDELETEDITEMS();
            tab.setKey(pk);
        }
        tab.setUserid(mcrSession.getUserInformation().getUserID());
        tab.setIp(mcrSession.getCurrentIP());

        if (create) {
            LogManager.getLogger().debug("Inserting into MCRDELETEDITEMS table");
            entityManager.persist(tab);
        }
    }

}
