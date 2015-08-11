/**
 * 
 */
package org.mycore.backend.hibernate;

import java.util.Date;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.mycore.backend.hibernate.tables.MCRDNBTRANSFERRESULTS;
import org.mycore.common.MCRPersistenceException;

/**
 * @author shermann
 */
public class MCRDNBTransferResultsTableMgr {

    private static final Logger LOGGER = Logger.getLogger(MCRDNBTransferResultsTableMgr.class);

    private static MCRDNBTransferResultsTableMgr _instance;

    private MCRDNBTransferResultsTableMgr() {
        //hide constructor
    }

    /**
     * Returns the singleton instance of this class.
     * 
     * @return the singleton instance
     */
    public static MCRDNBTransferResultsTableMgr getInstance() {
        if (_instance != null) {
            return _instance;
        }
        return (_instance = new MCRDNBTransferResultsTableMgr());
    }

    public synchronized void addEntry(String protocolType, String tpName, String deliveryRole, String objectId,
            boolean transferPackageArchived, String errorMessage, String errorModule, Date date) {

        Session session = MCRHIBConnection.instance().getSession();
        Transaction tx = null;

        try {
            tx = session.beginTransaction();
            MCRDNBTRANSFERRESULTS transferResult = new MCRDNBTRANSFERRESULTS();
            transferResult.setProtocolType(protocolType);
            transferResult.setTpName(tpName);
            transferResult.setDeliveryRole(deliveryRole);
            transferResult.setObjectId(objectId);
            transferResult.setTransferPackageArchived(transferPackageArchived);
            transferResult.setErrorMessage(errorMessage);
            transferResult.setErrorModule(errorModule);
            transferResult.setDate(date);
            session.save(transferResult);
            tx.commit();

        } catch (Exception ex) {
            LOGGER.error("Error occured while inserting data into table \"MCRDNBTRANSFERRESULTS\"", ex);
            tx.rollback();
        } finally {
            session.close();
        }
    }

    public synchronized void addEntry(String protocolType, String tpName, String deliveryRole, String objectId,
            boolean transferPackageArchived, String errorMessage, String errorModule) {
        addEntry(protocolType, tpName, deliveryRole, objectId, transferPackageArchived, errorMessage, errorModule, new Date());
    }
}
