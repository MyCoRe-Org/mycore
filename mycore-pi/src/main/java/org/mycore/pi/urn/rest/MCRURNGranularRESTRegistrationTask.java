package org.mycore.pi.urn.rest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.pi.MCRPIRegistrationInfo;
import org.mycore.pi.MCRPersistentIdentifierManager;
import org.mycore.pi.urn.MCRDNBURN;

import java.io.Closeable;
import java.io.IOException;
import java.util.Date;
import java.util.Optional;
import java.util.TimerTask;

/**
 * Created by chi on 26.01.17.
 * porting from org.mycore.urn.rest.URNRegistrationService
 *
 * @author shermann
 * @author Huu Chi Vu
 */
public final class MCRURNGranularRESTRegistrationTask extends TimerTask implements Closeable {
    protected static final Logger LOGGER = LogManager.getLogger();

    private final MCRDNBURNRestClient dnburnClient;

    public MCRURNGranularRESTRegistrationTask(MCRDNBURNRestClient client) {
        this.dnburnClient = client;
    }

    @Override
    public void run() {
        MCRPersistentIdentifierManager
                .getInstance()
                .setRegisteredDateForUnregisteredIdenifiers(MCRDNBURN.TYPE, dnburnClient::register);
    }

    @Override
    public void close() throws IOException {
        LOGGER.info("Stopping " + getClass().getSimpleName());
    }
}
