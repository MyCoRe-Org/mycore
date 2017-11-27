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
import java.util.TimerTask;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import javax.persistence.EntityTransaction;
import javax.persistence.RollbackException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.backend.jpa.MCREntityManagerProvider;
import org.mycore.pi.MCRPersistentIdentifierManager;
import org.mycore.pi.urn.MCRDNBURN;

/**
 * Created by chi on 26.01.17.
 * porting from org.mycore.urn.rest.URNRegistrationService
 *
 * @author shermann
 * @author Huu Chi Vu
 */
public final class MCRURNGranularRESTRegistrationTask extends TimerTask implements Closeable {

    protected static final Logger LOGGER = LogManager.getLogger();

    public static final int BATCH_SIZE = 20;

    private final MCRDNBURNRestClient dnburnClient;

    public MCRURNGranularRESTRegistrationTask(MCRDNBURNRestClient client) {
        this.dnburnClient = client;
    }

    @Override
    public void run() {
        UnaryOperator<Integer> register = b -> MCRPersistentIdentifierManager
            .getInstance()
            .setRegisteredDateForUnregisteredIdenifiers(MCRDNBURN.TYPE, dnburnClient::register, b);

        Integer numOfRegisteredObj = MCRTransactionExec.cute(register).apply(BATCH_SIZE);

        while (numOfRegisteredObj > 0) {
            numOfRegisteredObj = MCRTransactionExec.cute(register).apply(BATCH_SIZE);
        }

    }

    @Override
    public void close() throws IOException {
        LOGGER.info("Stopping {}", getClass().getSimpleName());
    }

    public static class MCRTransactionExec {
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
