package org.mycore.urn.rest;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.TimerTask;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.urn.epicurlite.BaseEpicurLiteProvider;
import org.mycore.urn.epicurlite.IEpicurLiteProvider;
import org.mycore.urn.hibernate.MCRURN;
import org.mycore.urn.services.MCRIURNProvider;
import org.mycore.urn.services.MCRURNManager;

/**
 * @author shermann
 *
 */
@Deprecated
public class URNRegistrationService extends TimerTask implements Closeable {

    protected static final Logger LOGGER = LogManager.getLogger(URNRegistrationService.class);

    protected URNServer server;

    protected IEpicurLiteProvider epicurLiteProvider;

    protected MCRIURNProvider urnProvider;

    /**
     * @throws Exception if the {@link IEpicurLiteProvider} implementing class cannot be loaded
     */
    @SuppressWarnings("unchecked")
    public URNRegistrationService() throws Exception {
        server = new URNServer(new DefaultURNServerConfiguration());

        String epicurLiteProviderClass = MCRConfiguration.instance().getString("MCR.URN.EpicurLiteProvider.Class",
            BaseEpicurLiteProvider.class.getName());
        Class<IEpicurLiteProvider> c = (Class<IEpicurLiteProvider>) Class.forName(epicurLiteProviderClass);
        epicurLiteProvider = c.newInstance();

        String urnProviderClass = MCRConfiguration.instance().getString("MCR.URN.Provider.Class");
        Class<MCRIURNProvider> c2 = (Class<MCRIURNProvider>) Class.forName(urnProviderClass);
        urnProvider = c2.newInstance();
    }

    @Override
    public void run() {
        try {
            LOGGER.debug("Currently there are a total of  " + MCRURNManager.getCount(false) + " unregistered urn");

            LOGGER.debug("Getting bunch of urn for registration at the DNB");
            List<MCRURN> list = getURNList();
            LOGGER.debug("Found a total of " + list.size() + " urn");

            URNProcessor processor = new URNProcessor(server, epicurLiteProvider);
            for (MCRURN urn : list) {
                processor.process(urn);
            }
        } catch (Throwable throwable) {
            LOGGER.error("General error", throwable);
        }
    }

    List<MCRURN> getURNList() {
        List<MCRURN> list = MCRURNManager.get(false, 0, 16384);
        return list;
    }

    @Override
    public void close() throws IOException {
        LOGGER.info("Stopping " + getClass().getSimpleName());
    }
}
