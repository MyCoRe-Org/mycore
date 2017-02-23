package org.mycore.pi.urn.rest;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.mycore.backend.hibernate.MCRHIBConnection;
import org.mycore.pi.MCRPIRegistrationInfo;
import org.mycore.pi.MCRPersistentIdentifierManager;
import org.mycore.pi.backend.MCRPI;
import org.mycore.pi.exceptions.MCRIdentifierUnresolvableException;
import org.mycore.pi.urn.MCRDNBURN;
import org.mycore.pi.urn.MCRDNBURNParser;
import org.mycore.pi.urn.MCRURNUtils;

import java.io.Closeable;
import java.io.IOException;
import java.text.ParseException;
import java.util.Date;
import java.util.Optional;
import java.util.TimerTask;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Created by chi on 26.01.17.
 * porting from org.mycore.urn.rest.URNRegistrationService
 * @author shermann
 * @author Huu Chi Vu
 */
public final class MCRURNGranularRESTRegistrationTask extends TimerTask implements Closeable {
    protected static final Logger LOGGER = LogManager.getLogger();

    private final MCRURNServer server;

    public MCRURNGranularRESTRegistrationTask(MCRURNServer serverSupplier) {
        this.server = serverSupplier;
    }

    @Override
    public void run() {
        LOGGER.info("Task start: " + new Date());
        try {
            MCRPersistentIdentifierManager.getInstance()
                                          .getUnregisteredIdenifiers(MCRDNBURN.TYPE)
                                          .parallelStream()
                                          .forEach(this::registerURN);
        } catch (Throwable throwable) {
            LOGGER.error("General error", throwable);
        }
        LOGGER.info("Task end: " + new Date());
    }

    public void registerURN(MCRPIRegistrationInfo urnInfo) {
        LOGGER.info("URN " + urnInfo.getIdentifier() + " is NOT registered");
        server.put(urnInfo, this::callback);
    }

    private void callback(HttpResponse response, MCREpicurLite elp) {
        MCRPIRegistrationInfo urnInfo = elp.getUrn();

        Function<MCRPI, Consumer<Logger>> saveChangesToDB = urn -> {
            MCRHIBConnection.instance().getSession().update(urn);
            return logger -> logger
                    .info("URN " + urn.getIdentifier() + " registered to " + elp.getUrl() + " - " + urn.getRegistered()
                                                                                                       .toString());
        };

        Supplier<Consumer<Logger>> warnCouldNotUpdateRegisterTime = () -> logger -> logger
                .warn("Could not update register time for URN " + urnInfo.getIdentifier() + ".");

        Function<Date, MCRPI> setNewRegisterDate = date -> {
            MCRPI urn = (MCRPI) urnInfo;
            urn.setRegistered(date);
            return urn;
        };

        int putStatus = response.getStatusLine().getStatusCode();
        switch (putStatus) {
        case HttpStatus.SC_CREATED:
            toDNBURN(urnInfo).flatMap(this::getDNBRegisterDate)
                             .map(setNewRegisterDate)
                             .map(saveChangesToDB)
                             .orElseGet(warnCouldNotUpdateRegisterTime)
                             .accept(LOGGER);
            break;
        case HttpStatus.SC_SEE_OTHER:
            LOGGER.warn("URN " + urnInfo.getIdentifier() + " could NOT registered to " + elp.getUrl() + "\n"
                                + "At least one of the given URLs is already registered under another URN");
            break;
        case HttpStatus.SC_CONFLICT:
            LOGGER.warn("URN " + urnInfo.getIdentifier() + " could NOT registered to " + elp.getUrl() + "\n"
                                + "Conflict: URN-Record already exists and can not be created again");
            break;
        default:
            LOGGER.warn(
                    "Could not handle urnInfo request: status=" + putStatus + ", urnInfo=" + urnInfo.getIdentifier()
                            + ", url=" + elp
                            .getUrl()
                            + "\nEpicur Lite:\n\n" + new XMLOutputter(
                            Format.getPrettyFormat()).outputString(elp.getEpicurLite()));
        }
    }

    private Optional<Date> getDNBRegisterDate(MCRDNBURN urn) {
        try {
            return Optional.ofNullable(MCRURNUtils.getDNBRegisterDate(urn));
        } catch (MCRIdentifierUnresolvableException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return Optional.empty();
    }

    private Optional<MCRDNBURN> toDNBURN(MCRPIRegistrationInfo urnInfo) {
        return new MCRDNBURNParser().parse(urnInfo.getIdentifier());
    }

    @Override
    public void close() throws IOException {
        LOGGER.info("Stopping " + getClass().getSimpleName());
    }
}
