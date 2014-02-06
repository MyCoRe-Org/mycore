package org.mycore.multitenancy.wcms.navigation;

import org.apache.log4j.Logger;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.datamodel.navigation.Navigation;

import com.google.gson.JsonObject;

public class WCMSNavigationManager {

    private static final Logger LOGGER = Logger.getLogger(WCMSNavigationManager.class);

    private NavigationProvider navigationProvider;

    public WCMSNavigationManager() {
        MCRConfiguration conf = MCRConfiguration.instance();
        Object navProvider = conf.getInstanceOf("MCR.WCMS2.navigationProvider", DefaultNavigationProvider.class.getName());
        if(!(navProvider instanceof NavigationProvider)) {
            LOGGER.error("MCR.WCMS2.navigationProvider is not an instance of NavigationProvider");
            return;
        }
        this.navigationProvider = (NavigationProvider)navProvider;
    }

    /**
     * 
     * @see NavigationProvider#toJSON(Navigation)
     * @param navigation
     * @return
     */
    public JsonObject toJSON(Navigation navigation) {
        return this.navigationProvider.toJSON(navigation);
    }

    public Navigation fromJSON(JsonObject jsonNavigation) {
        return this.navigationProvider.fromJSON(jsonNavigation);
    }

}
