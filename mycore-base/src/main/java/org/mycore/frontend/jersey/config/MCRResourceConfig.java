package org.mycore.frontend.jersey.config;

import org.mycore.common.config.MCRConfiguration;

import com.sun.jersey.api.core.ScanningResourceConfig;
import com.sun.jersey.core.spi.scanning.PackageNamesScanner;

public class MCRResourceConfig extends ScanningResourceConfig {

    protected String[] packages;

    public MCRResourceConfig() {
        String propertyString = MCRConfiguration.instance().getString("MCR.Jersey.resource.packages", "org.mycore.frontend.jersey.resources");
        setPackages(propertyString.split(","));
        init(new PackageNamesScanner(getPackages()));
    }

    public void setPackages(String[] packages) {
        this.packages = packages;
    }

    public String[] getPackages() {
        return packages;
    }

    /**
     * Perform a new search for resource classes and provider classes.
     * @deprecated See {@link ScanningResourceConfig#reload()} for more information
     */
    @Deprecated
    public void reload() {
        getClasses().clear();
        init(new PackageNamesScanner(packages));
    }

}
