package org.mycore.datamodel.ifs2;

import org.mycore.common.MCRConfiguration;
import org.mycore.datamodel.ifs2.MCRStore.MCRStoreConfig;

class MCRStoreDefaultConfig implements MCRStoreConfig {
    private String cfg;
    private String id;

    public MCRStoreDefaultConfig(String id) {
        this.id = id;
        cfg = "MCR.IFS2.Store." + id + ".";
    }

    @Override
    public String getBaseDir() {
        return MCRConfiguration.instance().getString(cfg + "BaseDir");
    }

    @Override
    public String getSlotLayout() {
        return MCRConfiguration.instance().getString(cfg + "SlotLayout");
    }

    @Override
    public String getID() {
        return id;
    }

}