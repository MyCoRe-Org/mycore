package org.mycore.pi.urn;

import java.text.ParseException;
import java.util.Date;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRException;
import org.mycore.datamodel.metadata.MCRBase;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.pi.MCRPIRegistrationService;
import org.mycore.pi.backend.MCRPI;
import org.mycore.pi.exceptions.MCRIdentifierUnresolvableException;
import org.mycore.pi.exceptions.MCRPersistentIdentifierException;

/**
 * This class registers urn for Metadata.
 */
public class MCRURNOAIRegistrationService extends MCRPIRegistrationService<MCRDNBURN> {

    private static final Logger LOGGER = LogManager.getLogger();

    public MCRURNOAIRegistrationService(String registrationServiceID) {
        super(registrationServiceID, MCRDNBURN.TYPE);
    }

    @Override
    protected MCRDNBURN registerIdentifier(MCRBase obj, String additional) throws MCRPersistentIdentifierException {
        if (!additional.equals("")) {
            throw new MCRPersistentIdentifierException(
                getClass().getName() + " doesn't support additional information! (" + additional + ")");
        }

        return getNewIdentifier(obj.getId(), additional);
    }

    @Override
    protected void delete(MCRDNBURN identifier, MCRBase obj, String additional)
        throws MCRPersistentIdentifierException {
        throw new MCRPersistentIdentifierException("Not supported!");
    }

    @Override
    protected void update(MCRDNBURN identifier, MCRBase obj, String additional)
        throws MCRPersistentIdentifierException {

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
