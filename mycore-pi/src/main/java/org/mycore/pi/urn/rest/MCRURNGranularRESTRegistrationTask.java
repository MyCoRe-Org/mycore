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

package org.mycore.pi.urn.rest;

import java.io.Closeable;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.TimerTask;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.backend.jpa.MCREntityManagerProvider;
import org.mycore.common.MCRSystemUserInformation;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.pi.MCRPIManager;
import org.mycore.pi.backend.MCRPI;
import org.mycore.pi.urn.MCRDNBURN;
import org.mycore.util.concurrent.MCRFixedUserCallable;

/**
 * Created by chi on 26.01.17.
 * porting from org.mycore.urn.rest.URNRegistrationService
 *
 * @author shermann
 * @author Huu Chi Vu
 */
public final class MCRURNGranularRESTRegistrationTask extends TimerTask implements Closeable {

    public final int BATCH_SIZE;

    protected static final Logger LOGGER = LogManager.getLogger(MCRURNGranularRESTRegistrationTask.class);

    private final MCRDNBURNRestClient dnburnClient;

    public MCRURNGranularRESTRegistrationTask(MCRDNBURNRestClient client) {
        this.dnburnClient = client;
        this.BATCH_SIZE = MCRConfiguration2.getInt("MCR.PI.GranularRESTRegistrationTask.batchSize").orElse(10);
    }

    @Override
    public void run() {
        LOGGER.info("Check for unregistered URNs .. ");
         try {
             List<MCRPI> unregisteredURNs = MCRPIManager.getInstance()
                 .getUnregisteredIdentifiers(MCRDNBURN.TYPE, BATCH_SIZE);

             for (MCRPI urn : unregisteredURNs) {
                 dnburnClient.register(urn).ifPresent(date -> setRegisterDate(urn, date));
             }
         } catch (Exception e) {
             LOGGER.error("Error occured while registering URNs!", e);
         }
    }

    private void setRegisterDate(MCRPI mcrpi, Date registerDate) {
        try {
            new MCRFixedUserCallable<>(() -> {
                mcrpi.setRegistered(registerDate);
                return MCREntityManagerProvider.getCurrentEntityManager().merge(mcrpi);
            }, MCRSystemUserInformation.getJanitorInstance()).call();
        } catch (Exception e) {
            LOGGER.error("Error while set registered date!", e);
        }
    }

    @Override
    public void close() throws IOException {
        LOGGER.info("Stopping {}", getClass().getSimpleName());
    }
}
