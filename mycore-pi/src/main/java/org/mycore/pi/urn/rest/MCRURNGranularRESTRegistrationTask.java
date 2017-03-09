package org.mycore.pi.urn.rest;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.mycore.pi.MCRPIRegistrationInfo;
import org.mycore.pi.MCRPersistentIdentifierManager;
import org.mycore.pi.urn.MCRDNBURN;
import org.mycore.pi.urn.MCRURNUtils;

import java.io.Closeable;
import java.io.IOException;
import java.util.Date;
import java.util.Optional;
import java.util.TimerTask;
import java.util.function.Function;

/**
 * Created by chi on 26.01.17.
 * porting from org.mycore.urn.rest.URNRegistrationService
 * @author shermann
 * @author Huu Chi Vu
 */
public final class MCRURNGranularRESTRegistrationTask extends TimerTask implements Closeable {
    protected static final Logger LOGGER = LogManager.getLogger();

    private final MCRDNBURNClient dnburnClient;

    public MCRURNGranularRESTRegistrationTask(MCRDNBURNClient client) {
        this.dnburnClient = client;
    }

    @Override
    public void run() {
        Function<MCRPIRegistrationInfo, Optional<Date>> dateProvider = urn -> dnburnClient
                .head(urn, this::handleHeadResponse)
                .flatMap(this::getDNBRegisterDate);

        MCRPersistentIdentifierManager
                .getInstance()
                .setRegisteredDateForUnregisteredIdenifiers(MCRDNBURN.TYPE, dateProvider);
    }

    private Optional<MCRPIRegistrationInfo> handleHeadResponse(HttpResponse response, MCRPIRegistrationInfo urnInfo) {
        int headStatus = response.getStatusLine().getStatusCode();
        Optional<MCRPIRegistrationInfo> urn = Optional.empty();

        switch (headStatus) {
        // urnInfo already registered
        case HttpStatus.SC_NO_CONTENT:
            LOGGER.info("URN " + urnInfo.getIdentifier() + " is already registered, performing update of url");
            urn = dnburnClient.post(urnInfo, this::handlePostResponse);
            break;
        // urnInfo not registered
        case HttpStatus.SC_NOT_FOUND:
            LOGGER.info("URN " + urnInfo.getIdentifier() + " is NOT registered");
            urn = dnburnClient.put(urnInfo, this::handlePutResponse);
            break;
        default:
            LOGGER.warn("Could not handle request for urnInfo " + urnInfo + " Status code " + headStatus);
            break;
        }

        return urn;
    }

    private Optional<Date> getDNBRegisterDate(MCRPIRegistrationInfo urnInfo){
        Optional<Date> dnbRegisterDate = MCRURNUtils.getDNBRegisterDate(urnInfo);
        dnbRegisterDate.map(Date::toString)
                       .map(date -> "URN " + urnInfo.getIdentifier() + " registered on " + date)
                       .ifPresent(LOGGER::info);

        return dnbRegisterDate;
    }

    private Optional<MCRPIRegistrationInfo> handlePostResponse(HttpResponse response, MCREpicurLite elp) {
        MCRPIRegistrationInfo urn = elp.getUrn();
        int postStatus = response.getStatusLine().getStatusCode();
        Optional<MCRPIRegistrationInfo> urnInfo = Optional.empty();

        switch (postStatus) {
        case HttpStatus.SC_NO_CONTENT:
            LOGGER.info("URN " + urn + " updated to " + elp.getUrl());
            urnInfo = Optional.of(urn);
            break;
        default:
            LOGGER.warn("URN " + urnInfo + " could not be updated. Status " + postStatus);
            break;
        }

        return urnInfo;
    }

    private Optional<MCRPIRegistrationInfo> handlePutResponse(HttpResponse response, MCREpicurLite elp) {
        MCRPIRegistrationInfo urn = elp.getUrn();
        int putStatus = response.getStatusLine().getStatusCode();
        Optional<MCRPIRegistrationInfo> urnInfo = Optional.empty();

        switch (putStatus) {
        case HttpStatus.SC_CREATED:
            LOGGER.info("URN " + urn.getIdentifier() + " registered to " + elp.getUrl());
            urnInfo = Optional.of(urn);
            break;
        case HttpStatus.SC_SEE_OTHER:
            LOGGER.warn("URN " + urn.getIdentifier() + " could NOT registered to " + elp.getUrl() + "\n"
                                + "At least one of the given URLs is already registered under another URN");
            break;
        case HttpStatus.SC_CONFLICT:
            LOGGER.warn("URN " + urn.getIdentifier() + " could NOT registered to " + elp.getUrl() + "\n"
                                + "Conflict: URN-Record already exists and can not be created again");
            break;
        default:
            LOGGER.warn(
                    "Could not handle urnInfo request: status=" + putStatus + ", urnInfo=" + urn.getIdentifier()
                            + ", url=" + elp
                            .getUrl()
                            + "\nEpicur Lite:\n\n" + new XMLOutputter(
                            Format.getPrettyFormat()).outputString(elp.toXML()));
        }

        return urnInfo;
    }

    @Override
    public void close() throws IOException {
        LOGGER.info("Stopping " + getClass().getSimpleName());
    }
}
