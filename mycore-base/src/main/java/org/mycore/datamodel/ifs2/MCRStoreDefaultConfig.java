package org.mycore.datamodel.ifs2;

import org.mycore.common.config.MCRConfiguration;
import org.mycore.datamodel.ifs2.MCRStore.MCRStoreConfig;

class MCRStoreDefaultConfig implements MCRStoreConfig {
    private String storeConfigPrefix;

    private String id;

    public MCRStoreDefaultConfig(String id) {
        this.id = id;
        storeConfigPrefix = "MCR.IFS2.Store." + id + ".";
    }

    @Override
    public String getBaseDir() {
        return MCRConfiguration.instance().getString(storeConfigPrefix + "BaseDir");
    }

    @Override
    public String getSlotLayout() {
        return MCRConfiguration.instance().getString(storeConfigPrefix + "SlotLayout");
    }

    @Override
    public String getID() {
        return id;
    }

}
