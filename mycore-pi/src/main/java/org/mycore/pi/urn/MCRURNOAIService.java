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

import java.text.ParseException;
import java.util.Date;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRException;
import org.mycore.datamodel.metadata.MCRBase;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.pi.MCRPIService;
import org.mycore.pi.backend.MCRPI;
import org.mycore.pi.exceptions.MCRIdentifierUnresolvableException;
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
        throw new MCRPersistentIdentifierException("Not supported!");
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

    @Override
    public boolean isRegistered(MCRObjectID id, String additional) {
        boolean registered = super.isRegistered(id, additional);
        if (registered)
            return true;

        if (!isCreated(id, additional)) {
            return false;
        }

        // URN is created. Now we need to check if it is resolvable
        MCRPI mcrpi = getTableEntry(id, additional);
        MCRDNBURN dnburn = new MCRDNBURNParser()
            .parse(mcrpi.getIdentifier())
            .orElseThrow(() -> new MCRException("Cannot parse Identifier from table: " + mcrpi.getIdentifier()));

        try {
            // Find register date in dnb rest
            Date dnbRegisteredDate = MCRURNUtils.getDNBRegisterDate(dnburn);

            if (dnbRegisteredDate == null) {
                return false;
            }

            mcrpi.setRegistered(dnbRegisteredDate);
            updateFlag(id, additional, mcrpi);
            return true;
        } catch (ParseException e) {
            LOGGER.error("Could not parse Date from PIDEF ! URN wont be marked as registered because of this! ", e);
            return false;
        } catch (MCRIdentifierUnresolvableException e) {
            return false;
        }

    }

}
