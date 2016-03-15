/**
 * 
 */
package org.mycore.backend.jpa.dnbtransfer;

import java.util.Date;

import org.mycore.backend.jpa.MCREntityManagerProvider;

/**
 * @author shermann
 * @author Thomas Scheffler (yagee)
 */
public class MCRDNBTransferResultsTableMgr {

    public static void addEntry(String protocolType, String tpName, String deliveryRole, String objectId,
        boolean transferPackageArchived, String errorMessage, String errorModule, Date date) {
        MCRDNBTRANSFERRESULTS transferResult = new MCRDNBTRANSFERRESULTS();
        transferResult.setProtocolType(protocolType);
        transferResult.setTpName(tpName);
        transferResult.setDeliveryRole(deliveryRole);
        transferResult.setObjectId(objectId);
        transferResult.setTransferPackageArchived(transferPackageArchived);
        transferResult.setErrorMessage(errorMessage);
        transferResult.setErrorModule(errorModule);
        transferResult.setDate(date);
        MCREntityManagerProvider.getCurrentEntityManager().persist(transferResult);
    }

    public static void addEntry(String protocolType, String tpName, String deliveryRole, String objectId,
        boolean transferPackageArchived, String errorMessage, String errorModule) {
        addEntry(protocolType, tpName, deliveryRole, objectId, transferPackageArchived, errorMessage, errorModule,
            new Date());
    }
}
