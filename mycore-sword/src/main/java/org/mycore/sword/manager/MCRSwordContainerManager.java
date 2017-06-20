package org.mycore.sword.manager;

import java.text.MessageFormat;
import java.util.Map;
import java.util.Optional;

import javax.servlet.http.HttpServletResponse;

import org.apache.abdera.i18n.iri.IRI;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.datamodel.metadata.MCRBase;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.sword.MCRSword;
import org.mycore.sword.MCRSwordUtil;
import org.mycore.sword.application.MCRSwordCollectionProvider;
import org.swordapp.server.AuthCredentials;
import org.swordapp.server.ContainerManager;
import org.swordapp.server.Deposit;
import org.swordapp.server.DepositReceipt;
import org.swordapp.server.SwordAuthException;
import org.swordapp.server.SwordConfiguration;
import org.swordapp.server.SwordError;
import org.swordapp.server.SwordServerException;
import org.swordapp.server.UriRegistry;

/**
 * @author Sebastian Hofmann (mcrshofm)
 */
public class MCRSwordContainerManager implements ContainerManager {
    private static Logger LOGGER = LogManager.getLogger(MCRSwordContainerManager.class);

    public static void throwObjectDoesNotExist(String objectIdString) throws SwordError {
        throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, HttpServletResponse.SC_NOT_FOUND,
            "The object '" + objectIdString + "' does not exist!");
    }

    public static void checkIsObject(MCRBase retrievedMCRBase) throws SwordError {
        if (retrievedMCRBase instanceof MCRDerivate) {
            throw new SwordError(UriRegistry.ERROR_METHOD_NOT_ALLOWED, HttpServletResponse.SC_BAD_REQUEST,
                "You cannot directly change Metadata of a Derivate!");
        }
    }

    @Override
    public DepositReceipt getEntry(String editIRI, Map<String, String> map, AuthCredentials authCredentials,
        SwordConfiguration swordConfiguration) throws SwordServerException, SwordError, SwordAuthException {
        IRI iri = new IRI(editIRI);
        String collection = MCRSwordUtil.ParseLinkUtil.EditIRI.getCollectionFromEditIRI(iri);
        String objectIdString = MCRSwordUtil.ParseLinkUtil.EditIRI.getObjectFromEditIRI(iri);
        final MCRSwordCollectionProvider collectionProvider = MCRSword.getCollection(collection);

        LOGGER.info(MessageFormat.format("REQUEST: Get entry {0} from {1} !", objectIdString, collection));

        collectionProvider.getAuthHandler().authentication(authCredentials);

        MCRObjectID objectId = MCRObjectID.getInstance(objectIdString);
        if (!MCRMetadataManager.exists(objectId)) {
            throwObjectDoesNotExist(objectIdString);
        }
        MCRBase retrievedMCRBase = MCRMetadataManager.retrieve(objectId);
        checkIsObject(retrievedMCRBase);

        final Optional<Map<String, String>> accept = Optional.of(map);
        final DepositReceipt metadata = collectionProvider.getContainerHandler().getMetadata(collection,
            (MCRObject) retrievedMCRBase, accept);
        return metadata;
    }

    @Override
    public DepositReceipt addMetadata(String editIRI, Deposit deposit, AuthCredentials authCredentials,
        SwordConfiguration swordConfiguration) throws SwordError, SwordServerException, SwordAuthException {
        return this.replaceMetadata(editIRI, deposit, authCredentials, swordConfiguration);
    }

    @Override
    public DepositReceipt addMetadataAndResources(String editIRI, Deposit deposit, AuthCredentials authCredentials,
        SwordConfiguration swordConfiguration) throws SwordError, SwordServerException, SwordAuthException {
        // this is not even supported by the JavaSwordServer
        return null;
    }

    @Override
    public DepositReceipt replaceMetadata(String editIRI, Deposit deposit, AuthCredentials authCredentials,
        SwordConfiguration swordConfiguration) throws SwordError, SwordServerException, SwordAuthException {
        IRI iri = new IRI(editIRI);
        String collection = MCRSwordUtil.ParseLinkUtil.EditIRI.getCollectionFromEditIRI(iri);
        String objectIdString = MCRSwordUtil.ParseLinkUtil.EditIRI.getObjectFromEditIRI(iri);
        final MCRSwordCollectionProvider collectionProvider = MCRSword.getCollection(collection);

        LOGGER.info(MessageFormat.format("REQUEST: Replace metadata of {0} from {1} !", objectIdString, collection));
        collectionProvider.getAuthHandler().authentication(authCredentials);
        MCRObjectID objectId = MCRObjectID.getInstance(objectIdString);

        if (!MCRMetadataManager.exists(objectId)) {
            throwObjectDoesNotExist(objectIdString);
        }

        MCRBase retrievedMCRBase = MCRMetadataManager.retrieve(objectId);
        checkIsObject(retrievedMCRBase);

        return collectionProvider.getContainerHandler().replaceMetadata((MCRObject) retrievedMCRBase, deposit);
    }

    @Override
    public DepositReceipt replaceMetadataAndMediaResource(String editIRI, Deposit deposit,
        AuthCredentials authCredentials, SwordConfiguration swordConfiguration)
        throws SwordError, SwordServerException, SwordAuthException {
        IRI iri = new IRI(editIRI);
        String collection = MCRSwordUtil.ParseLinkUtil.EditIRI.getCollectionFromEditIRI(iri);
        String objectIdString = MCRSwordUtil.ParseLinkUtil.EditIRI.getObjectFromEditIRI(iri);
        final MCRSwordCollectionProvider collectionProvider = MCRSword.getCollection(collection);

        LOGGER.info(MessageFormat.format("REQUEST: Replace metadata and resource of {0} from {1} !", objectIdString,
            collection));
        collectionProvider.getAuthHandler().authentication(authCredentials);

        MCRObjectID objectId = MCRObjectID.getInstance(objectIdString);

        if (!MCRMetadataManager.exists(objectId)) {
            throwObjectDoesNotExist(objectIdString);
        }

        MCRBase retrievedMCRBase = MCRMetadataManager.retrieve(objectId);
        return collectionProvider.getContainerHandler().replaceMetadataAndResources((MCRObject) retrievedMCRBase,
            deposit);
    }

    @Override
    public DepositReceipt addResources(String editIRI, Deposit deposit, AuthCredentials authCredentials,
        SwordConfiguration swordConfiguration) throws SwordError, SwordServerException, SwordAuthException {
        IRI iri = new IRI(editIRI);
        String collection = MCRSwordUtil.ParseLinkUtil.EditIRI.getCollectionFromEditIRI(iri);
        String objectIdString = MCRSwordUtil.ParseLinkUtil.EditIRI.getObjectFromEditIRI(iri);
        final MCRSwordCollectionProvider collectionProvider = MCRSword.getCollection(collection);

        LOGGER.info(MessageFormat.format("REQUEST: add resources {0} from {1} !", objectIdString, collection));

        collectionProvider.getAuthHandler().authentication(authCredentials);
        MCRObjectID objectId = MCRObjectID.getInstance(objectIdString);

        if (!MCRMetadataManager.exists(objectId)) {
            throwObjectDoesNotExist(objectIdString);
        }

        MCRBase retrievedMCRBase = MCRMetadataManager.retrieve(objectId);

        return collectionProvider.getContainerHandler().addResources((MCRObject) retrievedMCRBase, deposit);
    }

    @Override
    public void deleteContainer(String editIRI, AuthCredentials authCredentials, SwordConfiguration swordConfiguration)
        throws SwordError, SwordServerException, SwordAuthException {
        IRI iri = new IRI(editIRI);
        String collection = MCRSwordUtil.ParseLinkUtil.EditIRI.getCollectionFromEditIRI(iri);
        String objectIdString = MCRSwordUtil.ParseLinkUtil.EditIRI.getObjectFromEditIRI(iri);
        final MCRSwordCollectionProvider collectionProvider = MCRSword.getCollection(collection);

        LOGGER.info(MessageFormat.format("REQUEST: Delete {0} from {1}", objectIdString, collection));

        collectionProvider.getAuthHandler().authentication(authCredentials);

        MCRObjectID objectId = MCRObjectID.getInstance(objectIdString);
        if (!MCRMetadataManager.exists(objectId)) {
            throwObjectDoesNotExist(objectIdString);
        }

        final MCRBase object = MCRMetadataManager.retrieve(objectId);
        checkIsObject(object);

        collectionProvider.getContainerHandler().deleteObject((MCRObject) object);
    }

    @Override
    public DepositReceipt useHeaders(String editIRI, Deposit deposit, AuthCredentials authCredentials,
        SwordConfiguration swordConfiguration) throws SwordError, SwordServerException, SwordAuthException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isStatementRequest(String editIRI, Map<String, String> map, AuthCredentials authCredentials,
        SwordConfiguration swordConfiguration) throws SwordError, SwordServerException, SwordAuthException {
        return false;
    }
}
