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

import java.util.Optional;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.backend.jpa.MCREntityManagerProvider;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.annotation.MCRProperty;
import org.mycore.mcr.cronjob.MCRCronjob;
import org.mycore.pi.MCRPIManager;
import org.mycore.pi.MCRPIRegistrationInfo;
import org.mycore.pi.urn.MCRDNBURN;

import jakarta.persistence.EntityTransaction;
import jakarta.persistence.RollbackException;

/**
 * Check if created URNs are registered at the DNB
 */
public class MCRURNGranularRESTRegistrationCronjob extends MCRCronjob {

    private static final Logger LOGGER = LogManager.getLogger();

    private int batchSize;

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
        setRegisteredDateForUnregisteredIdentifiers(client);
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

    private void setRegisteredDateForUnregisteredIdentifiers(MCRDNBURNRestClient dnburnClient) {

        UnaryOperator<Integer> register = batchSize -> MCRPIManager.getInstance()
            .setRegisteredDateForUnregisteredIdentifiers(MCRDNBURN.TYPE, dnburnClient::register, batchSize);

        int numOfRegisteredObj = MCRTransactionExec.cute(register).apply(batchSize);
        while (numOfRegisteredObj > 0) {
            numOfRegisteredObj = MCRTransactionExec.cute(register).apply(batchSize);
        }

    }

    private static class MCRTransactionExec {
        public static <T, R> Function<T, R> cute(Function<T, R> function) {
            return t -> {
                EntityTransaction tx = beginTransaction();

                try {
                    return function.apply(t);
                } finally {
                    endTransaction(tx);
                }
            };
        }

        private static EntityTransaction beginTransaction() {
            EntityTransaction tx = MCREntityManagerProvider
                .getCurrentEntityManager()
                .getTransaction();

            tx.begin();
            return tx;
        }

        private static void endTransaction(EntityTransaction tx) {
            if (tx != null && tx.isActive()) {
                if (tx.getRollbackOnly()) {
                    tx.rollback();
                } else {
                    try {
                        tx.commit();
                    } catch (RollbackException e) {
                        if (tx.isActive()) {
                            tx.rollback();
                        }
                        throw e;
                    }
                }
            }
        }
    }

}

