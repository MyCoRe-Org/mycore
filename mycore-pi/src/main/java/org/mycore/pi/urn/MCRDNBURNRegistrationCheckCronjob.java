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
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.backend.jpa.MCREntityManagerProvider;
import org.mycore.common.MCRException;
import org.mycore.common.MCRSystemUserInformation;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.mcr.cronjob.MCRCronjob;
import org.mycore.pi.MCRPIManager;
import org.mycore.pi.MCRPIServiceManager;
import org.mycore.pi.backend.MCRPI;
import org.mycore.pi.exceptions.MCRIdentifierUnresolvableException;
import org.mycore.util.concurrent.MCRFixedUserCallable;

import jakarta.persistence.EntityManager;

/**
 * Check if created URNs are registered at the DNB
 */
public class MCRDNBURNRegistrationCheckCronjob extends MCRCronjob {

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public String getDescription() {
        return "DNB URN registration check";
    }

    @Override
    public void runJob() {

        try {

            new MCRFixedUserCallable<>(() -> {

                LOGGER.info("Searching unregistered URNs");

                EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
                MCRPIManager.getInstance().getUnregisteredIdentifiers(MCRDNBURN.TYPE, -1)
                    .stream()
                    .peek(em::detach)
                    .peek(mcrpi -> LOGGER.info("Found unregistered URN " + mcrpi.getIdentifier()))
                    .forEach(this::checkIfUrnIsRegistered);

                return null;

            }, MCRSystemUserInformation.getJanitorInstance()).call();

        } catch (Exception e) {
            LOGGER.error("Failed to check unregistered URNs", e);
        }

    }

    private void checkIfUrnIsRegistered(MCRPI mcrpi) {
        try {
            LOGGER.info("Checking unregistered URN {}", mcrpi.getIdentifier());
            getDateRegistered(mcrpi).ifPresent(date -> updateServiceFlags(mcrpi, date));
        } catch (Exception e) {
            LOGGER.error("Failed to check unregistered URN " + mcrpi.getIdentifier(), e);
        }
    }

    private Optional<Date> getDateRegistered(MCRPI mcrpi) throws MCRIdentifierUnresolvableException, ParseException {
        LOGGER.info("Fetching registration date for URN {}", mcrpi.getIdentifier());
        MCRDNBURN dnburn = new MCRDNBURNParser()
            .parse(mcrpi.getIdentifier())
            .orElseThrow(() -> new MCRException("Cannot parse Identifier from table: " + mcrpi.getIdentifier()));
        return Optional.ofNullable(MCRURNUtils.getDNBRegisterDate(dnburn));
    }

    private void updateServiceFlags(MCRPI mcrpi, Date registerDate) {
        LOGGER.info("Updating service flags for URN {}", mcrpi.getIdentifier());
        mcrpi.setRegistered(registerDate);
        MCRPIServiceManager.getInstance()
            .getRegistrationService(mcrpi.getService())
            .updateFlag(MCRObjectID.getInstance(mcrpi.getMycoreID()), mcrpi.getAdditional(), mcrpi);
        MCREntityManagerProvider.getCurrentEntityManager().merge(mcrpi);
    }

}
