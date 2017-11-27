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

package org.mycore.sword.manager;

import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.abdera.i18n.iri.IRI;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.sword.MCRSword;
import org.mycore.sword.MCRSwordUtil;
import org.mycore.sword.application.MCRSwordMediaHandler;
import org.swordapp.server.AuthCredentials;
import org.swordapp.server.Deposit;
import org.swordapp.server.DepositReceipt;
import org.swordapp.server.MediaResource;
import org.swordapp.server.MediaResourceManager;
import org.swordapp.server.SwordAuthException;
import org.swordapp.server.SwordConfiguration;
import org.swordapp.server.SwordError;
import org.swordapp.server.SwordServerException;
import org.swordapp.server.UriRegistry;

/**
 * @author Sebastian Hofmann (mcrshofm)
 */
public class MCRSwordMediaManager implements MediaResourceManager {
    protected static Logger LOGGER = LogManager.getLogger(MCRSwordMediaManager.class);

    public static MCRSwordMediaHandler getMediaProvider(String collection) {
        return MCRSword.getCollection(collection).getMediaHandler();
    }

    protected static void checkObject(String objectID) throws SwordError {
        if (objectID == null) {
            throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, HttpServletResponse.SC_BAD_REQUEST,
                "Could not extract ObjectID from IRI.");
        }

        if (!MCRMetadataManager.exists(MCRObjectID.getInstance(objectID))) {
            throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, HttpServletResponse.SC_NOT_FOUND,
                "The requested object " + objectID + " does not exist!");
        }
    }

    public static void doAuthentication(AuthCredentials authCredentials, String collection)
        throws SwordError, SwordServerException, SwordAuthException {
        MCRSword.getCollection(collection).getAuthHandler().authentication(authCredentials);
    }

    public MediaResource getMediaResourceRepresentation(String editMediaIRI, Map<String, String> accept,
        AuthCredentials authCredentials, SwordConfiguration swordConfiguration)
        throws SwordError, SwordServerException, SwordAuthException {
        LOGGER.info("getMediaResourceRepresentation: {}", editMediaIRI);
        final IRI mediaEditIRI = new IRI(editMediaIRI);
        final String requestDerivateID = MCRSwordUtil.ParseLinkUtil.MediaEditIRI
            .getDerivateFromMediaEditIRI(mediaEditIRI);
        final String requestFilePath = MCRSwordUtil.ParseLinkUtil.MediaEditIRI
            .getFilePathFromMediaEditIRI(mediaEditIRI);
        final String collection = MCRSwordUtil.ParseLinkUtil.MediaEditIRI.getCollectionFromMediaEditIRI(mediaEditIRI);

        doAuthentication(authCredentials, collection);
        checkObject(requestDerivateID);

        return getMediaProvider(collection).getMediaResourceRepresentation(requestDerivateID, requestFilePath, accept);
    }

    public DepositReceipt replaceMediaResource(String editMediaIRI, Deposit deposit, AuthCredentials authCredentials,
        SwordConfiguration swordConfiguration) throws SwordError, SwordServerException, SwordAuthException {
        LOGGER.info("replaceMediaResource: {}", editMediaIRI);
        final IRI mediaEditIRI = new IRI(editMediaIRI);

        final String requestDerivateID = MCRSwordUtil.ParseLinkUtil.MediaEditIRI
            .getDerivateFromMediaEditIRI(mediaEditIRI);
        final String requestFilePath = MCRSwordUtil.ParseLinkUtil.MediaEditIRI
            .getFilePathFromMediaEditIRI(mediaEditIRI);
        final String collection = MCRSwordUtil.ParseLinkUtil.MediaEditIRI.getCollectionFromMediaEditIRI(mediaEditIRI);

        doAuthentication(authCredentials, collection);
        checkObject(requestDerivateID);
        getMediaProvider(collection).replaceMediaResource(requestDerivateID, requestFilePath, deposit);

        final MCRObject mcrObject = MCRSwordUtil.getMcrObjectForDerivateID(requestDerivateID);
        return MCRSword.getCollection(collection).getMetadataProvider().provideMetadata(mcrObject);
    }

    public DepositReceipt addResource(String editMediaIRI, Deposit deposit, AuthCredentials authCredentials,
        SwordConfiguration swordConfiguration) throws SwordError, SwordServerException, SwordAuthException {
        LOGGER.info("addResource: {}", editMediaIRI);

        final IRI mediaEditIRI = new IRI(editMediaIRI);
        final String requestDerivateID = MCRSwordUtil.ParseLinkUtil.MediaEditIRI
            .getDerivateFromMediaEditIRI(mediaEditIRI);
        String requestFilePath = MCRSwordUtil.ParseLinkUtil.MediaEditIRI.getFilePathFromMediaEditIRI(mediaEditIRI);
        final String collection = MCRSwordUtil.ParseLinkUtil.MediaEditIRI.getCollectionFromMediaEditIRI(mediaEditIRI);

        doAuthentication(authCredentials, collection);
        checkObject(requestDerivateID);

        if (requestFilePath == null) {
            requestFilePath = "/";
        }

        getMediaProvider(collection).addResource(requestDerivateID, requestFilePath, deposit);
        final MCRObject mcrObject = MCRSwordUtil.getMcrObjectForDerivateID(requestDerivateID);
        return MCRSword.getCollection(collection).getMetadataProvider().provideMetadata(mcrObject);
    }

    public void deleteMediaResource(String editMediaIRI, AuthCredentials authCredentials,
        SwordConfiguration swordConfiguration) throws SwordError, SwordServerException, SwordAuthException {
        LOGGER.info("deleteMediaResource: {}", editMediaIRI);
        final IRI mediaEditIRI = new IRI(editMediaIRI);
        final String requestObjectID = MCRSwordUtil.ParseLinkUtil.MediaEditIRI
            .getDerivateFromMediaEditIRI(mediaEditIRI);
        final String requestFilePath = MCRSwordUtil.ParseLinkUtil.MediaEditIRI
            .getFilePathFromMediaEditIRI(mediaEditIRI);
        final String collection = MCRSwordUtil.ParseLinkUtil.MediaEditIRI.getCollectionFromMediaEditIRI(mediaEditIRI);

        doAuthentication(authCredentials, collection);
        getMediaProvider(collection).deleteMediaResource(requestObjectID, requestFilePath);
    }

}
