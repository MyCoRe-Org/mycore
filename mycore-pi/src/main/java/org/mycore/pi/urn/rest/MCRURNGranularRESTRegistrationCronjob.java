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

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.backend.jpa.MCREntityManagerProvider;
import org.mycore.common.MCRSystemUserInformation;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.annotation.MCRProperty;
import org.mycore.mcr.cronjob.MCRCronjob;
import org.mycore.pi.MCRPIManager;
import org.mycore.pi.MCRPIRegistrationInfo;
import org.mycore.pi.backend.MCRPI;
import org.mycore.pi.urn.MCRDNBURN;
import org.mycore.util.concurrent.MCRFixedUserCallable;

/**
 * Check if created URNs are registered at the DNB
 */
public class MCRURNGranularRESTRegistrationCronjob extends MCRCronjob {

    private static final Logger LOGGER = LogManager.getLogger();

    private int batchSize = 20;

    @MCRProperty(name = "BatchSize", defaultName = "MCR.CronJob.Default.URNGranularRESTRegistration.BatchSize")
    public void setBatchSize(String batchSize) {
        this.batchSize = Integer.parseInt(batchSize);
    }

    @Override
    public String getDescription() {
        return "URN granular REST registration";
    }

    @Override
    public void runJob() {
        MCRDNBURNRestClient client = new MCRDNBURNRestClient(getBundleProvider(), getUsernamePasswordCredentials());

        try {
            List<MCRPI> unregisteredURNs = MCRPIManager.getInstance().getUnregisteredIdentifiers(MCRDNBURN.TYPE,
                batchSize);

            for (MCRPI urn : unregisteredURNs) {
                client.register(urn).ifPresent(date -> setRegisterDate(urn, date));
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

    private Function<MCRPIRegistrationInfo, MCRURNJsonBundle> getBundleProvider() {
        return urn -> MCRURNJsonBundle.instance(urn, MCRDerivateURNUtils.getURL(urn));
    }

    private Optional<UsernamePasswordCredentials> getUsernamePasswordCredentials() {
        String username = MCRConfiguration2.getString("MCR.PI.DNB.Credentials.Login").orElse(null);
        String password = MCRConfiguration2.getString("MCR.PI.DNB.Credentials.Password").orElse(null);
        if (username == null || password == null || username.isBlank() || password.isBlank()) {
            LOGGER.warn("Could not instantiate {} as required credentials are not set", this.getClass().getName());
            return Optional.empty();
        }
        return Optional.of(new UsernamePasswordCredentials(username, password));
    }
}
