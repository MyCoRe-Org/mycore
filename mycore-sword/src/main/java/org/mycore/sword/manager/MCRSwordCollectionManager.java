package org.mycore.sword.manager;

import java.text.MessageFormat;

import javax.servlet.http.HttpServletResponse;

import org.apache.abdera.Abdera;
import org.apache.abdera.i18n.iri.IRI;
import org.apache.abdera.model.Feed;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.sword.MCRSword;
import org.mycore.sword.MCRSwordConstants;
import org.mycore.sword.MCRSwordUtil;
import org.mycore.sword.application.MCRSwordCollectionProvider;
import org.swordapp.server.AuthCredentials;
import org.swordapp.server.CollectionDepositManager;
import org.swordapp.server.CollectionListManager;
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
public class MCRSwordCollectionManager implements CollectionListManager, CollectionDepositManager {
    private static Logger LOGGER = LogManager.getLogger(MCRSwordCollectionManager.class);

    @Override
    public Feed listCollectionContents(IRI collectionIRI, AuthCredentials authCredentials, SwordConfiguration config)
        throws SwordServerException, SwordAuthException, SwordError {
        String collection = MCRSwordUtil.ParseLinkUtil.CollectionIRI.getCollectionNameFromCollectionIRI(collectionIRI);
        String path = collectionIRI.getPath();

        LOGGER.info(MessageFormat.format("List Collection: {0}", collection));

        Feed feed = new Abdera().newFeed();
        if (MCRSword.getCollectionNames().contains(collection)) {
            MCRSwordCollectionProvider collectionProvider = MCRSword.getCollection(collection);

            collectionProvider.getAuthHandler().authentication(authCredentials);
            if (collectionProvider.isVisible()) {
                Integer paginationFromIRI = MCRSwordUtil.ParseLinkUtil.CollectionIRI
                    .getPaginationFromCollectionIRI(collectionIRI);
                final int start = (paginationFromIRI - 1) * MCRSwordConstants.MAX_ENTRYS_PER_PAGE;

                collectionProvider.getIDSupplier().get(start, MCRSwordConstants.MAX_ENTRYS_PER_PAGE).stream()
                    .map(id -> {
                        try {
                            return collectionProvider.getMetadataProvider().provideListMetadata(id);
                        } catch (SwordError swordError) {
                            LOGGER.warn("Error while creating feed for [" + id + "]! (Will be removed from List)");
                            return null;
                        }
                    }).filter(e -> e != null)
                    .forEach(feed::addEntry);

                MCRSwordUtil.BuildLinkUtil.addPaginationLinks(collectionIRI, collection, feed, collectionProvider);
            }

        } else {
            throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, HttpServletResponse.SC_NOT_FOUND,
                "The collection '" + collection + "' does not exist!");
        }

        return feed;
    }

    @Override
    public DepositReceipt createNew(String editIRI, Deposit deposit, AuthCredentials authCredentials,
        SwordConfiguration swordConfiguration) throws SwordError, SwordServerException, SwordAuthException {
        LOGGER.info("createNew:" + editIRI);
        String collection = MCRSwordUtil.ParseLinkUtil.CollectionIRI
            .getCollectionNameFromCollectionIRI(new IRI(editIRI));
        MCRSwordCollectionProvider collectionProvider = MCRSword.getCollection(collection);
        collectionProvider.getAuthHandler().authentication(authCredentials);
        return collectionProvider.getContainerHandler().addObject(deposit);
    }

}
