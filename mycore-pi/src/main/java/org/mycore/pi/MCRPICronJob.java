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

package org.mycore.pi;

import java.text.ParseException;
import java.util.AbstractMap;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.backend.jpa.MCREntityManagerProvider;
import org.mycore.common.MCRException;
import org.mycore.common.MCRSystemUserInformation;
import org.mycore.common.events.MCRShutdownHandler;
import org.mycore.common.events.MCRStartupHandler;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.pi.backend.MCRPI;
import org.mycore.pi.exceptions.MCRIdentifierUnresolvableException;
import org.mycore.pi.urn.MCRDNBURN;
import org.mycore.pi.urn.MCRDNBURNParser;
import org.mycore.pi.urn.MCRURNUtils;
import org.mycore.util.concurrent.MCRFixedUserCallable;

import jakarta.persistence.EntityManager;
import jakarta.servlet.ServletContext;

/**
 * Handles various tasks that need to be executed time based:
 * <ul>
 *     <li>Check if created URNs are registered at the DNB</li>
 * </ul>
 */
public class MCRPICronJob implements Runnable, MCRStartupHandler.AutoExecutable {

    private static final int CHECK_URN_THREAD_COUNT = Math.max(1, Runtime.getRuntime().availableProcessors() / 4);

    private static final int CRON_INITIAL_DELAY_MINUTES = 1;

    private static final int CRON_PERIOD_MINUTES = (int) TimeUnit.HOURS.toMinutes(12);

    private static final int CRON_THREAD_COUNT = 1;

    private static final Logger LOGGER = LogManager.getLogger();

    private static final ExecutorService CHECK_URN_EXECUTOR_SERVICE = getCheckUrnExecutorService();

    private ScheduledExecutorService cronExcutorService;

    private ExecutorService updateExecutorService;

    private static ExecutorService getCheckUrnExecutorService() {
        final AtomicInteger num = new AtomicInteger();
        final ExecutorService executorService = Executors.newFixedThreadPool(CHECK_URN_THREAD_COUNT, r -> {
            int tNum = num.incrementAndGet();
            Thread t = new Thread(r);
            t.setName(MCRPICronJob.class.getSimpleName() + ".urn#" + tNum);
            return t;
        });
        addShutdownHandler(executorService);
        return executorService;
    }

    private static void addShutdownHandler(ExecutorService executorService) {
        MCRShutdownHandler.getInstance().addCloseable(() -> {
            executorService.shutdown();
            try {
                executorService.awaitTermination(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                LOGGER.error("Interupted wait for termination.", e);
            }
        });
    }

    public void run() {
        LOGGER.info("Running " + getName() + "..");
        EntityManager em = MCREntityManagerProvider.getCurrentEntityManager();
        final List<MCRPI> urns = MCRPIManager.getInstance().getUnregisteredIdentifiers(MCRDNBURN.TYPE, -1);

        CompletableFuture[] cfs = urns.stream()
            .peek(em::detach)
            .map(mcrpi -> CompletableFuture.supplyAsync(() -> getDateRegistred(mcrpi), CHECK_URN_EXECUTOR_SERVICE))
            .map(cf -> cf.thenAcceptAsync(result -> {
                if (result == null || result.getKey() == null || result.getValue() == null) {
                    return;
                }
                try {
                    updateFlags(result.getKey(), result.getValue()).call();
                } catch (RuntimeException e) {
                    throw e;
                } catch (Exception e) {
                    throw new MCRException(e);
                }
            }, updateExecutorService))
            .toArray(CompletableFuture[]::new);

        if (cfs.length == 0) {
            return;
        }

        try {
            LOGGER.info("Waiting for {} updates to complete", cfs.length);
            CompletableFuture
                .allOf(cfs)
                .join();
            LOGGER.info("{} updates completed", cfs.length);
        } catch (CompletionException e) {
            LOGGER.error("Error in PICronjob!", e);
        }

    }

    private Map.Entry<MCRPI, Date> getDateRegistred(MCRPI mcrpi) {
        LOGGER.info("check {} is registered.", mcrpi.getIdentifier());
        MCRDNBURN dnburn = new MCRDNBURNParser()
            .parse(mcrpi.getIdentifier())
            .orElseThrow(() -> new MCRException("Cannot parse Identifier from table: " + mcrpi.getIdentifier()));
        try {
            // Find register date in dnb rest
            return new AbstractMap.SimpleEntry<>(mcrpi, MCRURNUtils.getDNBRegisterDate(dnburn));
        } catch (ParseException e) {
            LOGGER.error("Could not parse Date from PIDEF ! URN wont be marked as registered because of this! ", e);
        } catch (MCRIdentifierUnresolvableException e) {
            LOGGER.error("Could not update Date from PIDEF ! URN wont be marked as registered because of this! ", e);
        }
        return null;
    }

    private MCRFixedUserCallable<Void> updateFlags(MCRPI mcrpi, Date registerDate) {
        return new MCRFixedUserCallable<>(() -> {
            mcrpi.setRegistered(registerDate);
            MCRPIServiceManager.getInstance().getRegistrationService(mcrpi.getService())
                .updateFlag(MCRObjectID.getInstance(mcrpi.getMycoreID()), mcrpi.getAdditional(), mcrpi);
            MCREntityManagerProvider.getCurrentEntityManager().merge(mcrpi);
            return null;
        }, MCRSystemUserInformation.getJanitorInstance());
    }

    @Override
    public String getName() {
        return getClass().getName();
    }

    @Override
    public int getPriority() {
        return Integer.MIN_VALUE + 1000;
    }

    @Override
    public void startUp(ServletContext servletContext) {
        if (servletContext == null) {
            return; //do not run in CLI
        }
        updateExecutorService = Executors
            .newSingleThreadExecutor(r -> new Thread(r, MCRPICronJob.class.getSimpleName() + ".update"));
        addShutdownHandler(updateExecutorService);
        cronExcutorService = Executors.newScheduledThreadPool(CRON_THREAD_COUNT,
            r -> new Thread(r, MCRPICronJob.class.getSimpleName() + ".cron"));
        addShutdownHandler(cronExcutorService);
        cronExcutorService
            .scheduleWithFixedDelay(this, CRON_INITIAL_DELAY_MINUTES, CRON_PERIOD_MINUTES, TimeUnit.MINUTES);
    }
}
