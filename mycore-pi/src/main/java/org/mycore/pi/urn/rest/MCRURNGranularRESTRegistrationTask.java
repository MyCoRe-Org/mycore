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
import java.net.URL;
import java.text.MessageFormat;
import java.util.Date;
import java.util.Optional;
import java.util.TimerTask;
import java.util.function.Function;

/**
 * Created by chi on 26.01.17.
 * porting from org.mycore.urn.rest.URNRegistrationService
 *
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

        String identifier = urnInfo.getIdentifier();
        switch (headStatus) {
        // urnInfo already registered
        case HttpStatus.SC_NO_CONTENT:
            LOGGER.info("URN {} is already registered, performing update of url", identifier);
            urn = dnburnClient.post(urnInfo, this::handlePostResponse);
            break;
        // urnInfo not registered
        case HttpStatus.SC_NOT_FOUND:
            LOGGER.info("URN {} is NOT registered", identifier);
            urn = dnburnClient.put(urnInfo, this::handlePutResponse);
            break;
        default:
            LOGGER.warn("Could not handle request for urnInfo {} Status code {}.", identifier, headStatus);
            break;
        }

        return urn;
    }

    private void loggerInfo(String msg, MCRPIRegistrationInfo urnInfo) {
        LOGGER.info(MessageFormat.format(msg, urnInfo.getIdentifier()));
    }

    private Optional<Date> getDNBRegisterDate(Optional<MCRPIRegistrationInfo> urnInfo) {
        Optional<Date> dnbRegisterDate = urnInfo.flatMap(MCRURNUtils::getDNBRegisterDate);
        dnbRegisterDate.map(Date::toString)
                       .flatMap(date -> urnInfo.map(urn -> "URN " + urn.getIdentifier() + " registered on " + date))
                       .ifPresent(LOGGER::info);

        return dnbRegisterDate;
    }

    private MCRPIRegistrationInfo handlePostResponse(HttpResponse response, MCREpicurLite elp) {
        MCRPIRegistrationInfo urn = null;
        int postStatus = response.getStatusLine().getStatusCode();

        String identifier = elp.getUrn().getIdentifier();
        switch (postStatus) {
        case HttpStatus.SC_NO_CONTENT:
            urn = elp.getUrn();
            LOGGER.info("URN {} updated to {}.", identifier, elp.getUrl());
            break;
        default:
            LOGGER.warn("URN {} could not be updated. Status {}.", identifier, postStatus);
            break;
        }

        return urn;
    }

    private MCRPIRegistrationInfo handlePutResponse(HttpResponse response, MCREpicurLite elp) {
        MCRPIRegistrationInfo urn = null;
        int putStatus = response.getStatusLine().getStatusCode();

        String identifier = elp.getUrn().getIdentifier();
        URL url = elp.getUrl();
        switch (putStatus) {
        case HttpStatus.SC_CREATED:
            urn = elp.getUrn();
            LOGGER.info("URN " + identifier + " registered to " + url);
            break;
        case HttpStatus.SC_SEE_OTHER:
            LOGGER.warn("URN " + identifier + " could NOT registered to " + url + "\n"
                                + "At least one of the given URLs is already registered under another URN");
            break;
        case HttpStatus.SC_CONFLICT:
            LOGGER.warn("URN " + identifier + " could NOT registered to " + url + "\n"
                                + "Conflict: URN-Record already exists and can not be created again");
            break;
        default:
            LOGGER.warn("Could not handle urnInfo request: status={}, urn={}, url={}.", putStatus, identifier, url);
            LOGGER.warn("Epicur Lite:");
            LOGGER.warn(epicurLiteToString(elp));
            break;
        }

        return urn;
    }

    private String epicurLiteToString(MCREpicurLite elp) {
        return new XMLOutputter(
                Format.getPrettyFormat()).outputString(elp.toXML());
    }

    @Override
    public void close() throws IOException {
        LOGGER.info("Stopping " + getClass().getSimpleName());
    }
}
