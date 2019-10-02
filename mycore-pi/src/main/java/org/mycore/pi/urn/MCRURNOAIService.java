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

package org.mycore.pi.urn;

import java.util.Date;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRSystemUserInformation;
import org.mycore.datamodel.metadata.MCRBase;
import org.mycore.pi.MCRPIService;
import org.mycore.pi.exceptions.MCRPersistentIdentifierException;

/**
 * This class registers urn for Metadata.
 */
public class MCRURNOAIService extends MCRPIService<MCRDNBURN> {

    private static final Logger LOGGER = LogManager.getLogger();

    public MCRURNOAIService(String registrationServiceID) {
        super(registrationServiceID, MCRDNBURN.TYPE);
    }

    @Override
    protected void registerIdentifier(MCRBase obj, String additional, MCRDNBURN urn)
        throws MCRPersistentIdentifierException {
        if (!additional.equals("")) {
            throw new MCRPersistentIdentifierException(
                getClass().getName() + " doesn't support additional information! (" + additional + ")");
        }
    }

    @Override
    protected void delete(MCRDNBURN identifier, MCRBase obj, String additional)
        throws MCRPersistentIdentifierException {
        if (!MCRSessionMgr.getCurrentSession().getUserInformation().getUserID()
            .equals(MCRSystemUserInformation.getSuperUserInstance().getUserID())) {
            throw new MCRPersistentIdentifierException("Not supported!");
        }
    }

    @Override
    protected void update(MCRDNBURN identifier, MCRBase obj, String additional)
        throws MCRPersistentIdentifierException {
        // nothing to do here!
    }

    @Override
    protected Date provideRegisterDate(MCRBase obj, String additional) {
        return null;
    }

}
