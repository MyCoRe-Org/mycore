package org.mycore.wcms2.navigation;

import org.apache.log4j.Logger;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.wcms2.datamodel.MCRNavigation;

import com.google.gson.JsonObject;

public class MCRWCMSNavigationManager {

    private static final Logger LOGGER = Logger.getLogger(MCRWCMSNavigationManager.class);

    private MCRWCMSNavigationProvider navigationProvider;

    public MCRWCMSNavigationManager() {
        MCRConfiguration conf = MCRConfiguration.instance();
        Object navProvider = conf.getInstanceOf("MCR.WCMS2.navigationProvider", MCRWCMSDefaultNavigationProvider.class.getName());
        if(!(navProvider instanceof MCRWCMSNavigationProvider)) {
            LOGGER.error("MCR.WCMS2.navigationProvider is not an instance of NavigationProvider");
            return;
        }
        this.navigationProvider = (MCRWCMSNavigationProvider)navProvider;
    }

    /**
     * 
     * @see MCRWCMSNavigationProvider#toJSON(MCRNavigation)
     * @param navigation
     * @return
     */
    public JsonObject toJSON(MCRNavigation navigation) {
        return this.navigationProvider.toJSON(navigation);
    }

    public MCRNavigation fromJSON(JsonObject jsonNavigation) {
        return this.navigationProvider.fromJSON(jsonNavigation);
    }

}
