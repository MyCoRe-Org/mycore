/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
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
