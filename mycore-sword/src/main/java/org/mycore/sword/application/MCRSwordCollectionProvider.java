package org.mycore.sword.application;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.sword.MCRSwordContainerHandler;
import org.swordapp.server.SwordError;

/**
 * Interface to tell the MyCoRe SwordV2 which MyCoRe Objects will be visible to sword and in which collections they are.
 *
 * @author Sebastian Hofmann (mcrshofm)
 */
public abstract class MCRSwordCollectionProvider implements MCRSwordLifecycle {

    protected static Logger LOGGER = LogManager.getLogger(MCRSwordCollectionProvider.class);

    private MCRSwordContainerHandler mcrSwordContainerHandler;

    private MCRSwordMediaHandler mcrSwordMediaHandler;

    protected MCRSwordCollectionProvider() {
        mcrSwordContainerHandler = new MCRSwordContainerHandler();
        mcrSwordMediaHandler = new MCRSwordMediaHandler();
    }

    /**
     * tells the SwordV2 impl if the Collection is visible for the current User.
     *
     * @return true if the collection should be provided.
     */
    public abstract boolean isVisible();

    /**
     * tells which packaging is supported by the collection.
     *
     * @return a list of supported packacking
     */
    public abstract List<String> getSupportedPagacking();

    /**
     * @return a supplier which tells the MyCoRe Sword implementation which objects can be exposed to a collection
     */
    public abstract MCRSwordObjectIDSupplier getIDSupplier();

    public MCRSwordContainerHandler getContainerHandler() {
        return mcrSwordContainerHandler;
    }

    public abstract MCRSwordIngester getIngester();

    public abstract MCRSwordMetadataProvider getMetadataProvider();

    /**
     * @return the {@link MCRSwordMediaHandler} which will be used for this collection
     */
    public MCRSwordMediaHandler getMediaHandler() {
        return mcrSwordMediaHandler;
    }

    public abstract MCRSwordAuthHandler getAuthHandler();

    public Stream<String> getDerivateIDsofObject(final String mcrObjectId) throws SwordError {
        final List<MCRObjectID> derivateIds = MCRMetadataManager.getDerivateIds(MCRObjectID.getInstance(mcrObjectId),
            10, TimeUnit.SECONDS);
        return derivateIds.stream().map(oid -> oid.toString());
    }

    @Override
    public void init(MCRSwordLifecycleConfiguration lifecycleConfiguration) {
        getIngester().init(lifecycleConfiguration);
        getMetadataProvider().init(lifecycleConfiguration);
        getMediaHandler().init(lifecycleConfiguration);
        getContainerHandler().init(lifecycleConfiguration);
    }

    @Override
    public void destroy() {
        getIngester().destroy();
        getMetadataProvider().destroy();
        getMediaHandler().destroy();
        getContainerHandler().destroy();
    }
}
