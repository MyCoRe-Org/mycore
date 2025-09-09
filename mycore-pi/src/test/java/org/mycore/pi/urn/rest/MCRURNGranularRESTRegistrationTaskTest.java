/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
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

package org.mycore.pi.urn.rest;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mycore.pi.MCRPIUtils.generateMCRPI;
import static org.mycore.pi.MCRPIUtils.randomFilename;

import java.util.Date;
import java.util.Optional;
import java.util.function.Function;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mycore.backend.jpa.MCREntityManagerProvider;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.pi.MCRPIManager;
import org.mycore.pi.MCRPIRegistrationInfo;
import org.mycore.pi.MCRPIUtils;
import org.mycore.pi.backend.MCRPI;
import org.mycore.pi.urn.MCRDNBURN;
import org.mycore.test.MCRMetadataExtension;
import org.mycore.test.MyCoReTest;

/**
 * Created by chi on 23.02.17.
 *
 * @author Huu Chi Vu
 */
@MyCoReTest
@ExtendWith(MCRMetadataExtension.class)
@MCRTestConfiguration(properties = {
    @MCRTestProperty(key = "MCR.Metadata.Type.test", string = "true"),
    @MCRTestProperty(key = "MCR.PI.Generator.testGenerator.Namespace", string = "frontend-")
})
public class MCRURNGranularRESTRegistrationTaskTest {
    private static final String countRegistered = "select count(u) from MCRPI u "
        + "where u.type = :type "
        + "and u.registered is not null";

    public static final int BATCH_SIZE = 20;

    private static final Logger LOGGER = LogManager.getLogger();

    @Disabled
    @Test
    public void run()  {
        MCRPI urn1 = generateMCRPI(randomFilename(), countRegistered);
        MCREntityManagerProvider.getCurrentEntityManager()
            .persist(urn1);

        assertNull(urn1.getRegistered(), "Registered date should be null.");

        MCRPIManager.getInstance()
            .getUnregisteredIdentifiers(urn1.getType())
            .stream()
            .map(MCRPIRegistrationInfo::getIdentifier)
            .map("URN: "::concat)
            .forEach(LOGGER::info);

        Integer progressedIdentifiersFromDatabase;
        Function<MCRPIRegistrationInfo, Optional<Date>> registerFn = MCRPIUtils.getMCRURNClient()::register;
        do {
            progressedIdentifiersFromDatabase = MCRPIManager.getInstance()
                .setRegisteredDateForUnregisteredIdentifiers(MCRDNBURN.TYPE, registerFn, BATCH_SIZE);
        } while (progressedIdentifiersFromDatabase > 0);

        boolean registered = MCRPIManager.getInstance().isRegistered(urn1);
        LOGGER.info("Registered: {}", registered);

        MCRPI mcrpi = MCREntityManagerProvider.getCurrentEntityManager().find(MCRPI.class, urn1.getId());
        Optional.ofNullable(mcrpi)
            .filter(pi -> pi.getRegistered() != null)
            .map(MCRPI::getRegistered)
            .map(Date::toString)
            .map("URN registered: "::concat)
            .ifPresent(LOGGER::info);

        MCRPIManager.getInstance().getUnregisteredIdentifiers(urn1.getType())
            .stream()
            .map(MCRPIRegistrationInfo::getIdentifier)
            .map("URN update: "::concat)
            .forEach(LOGGER::info);

        LOGGER.info("end.");
    }

}
