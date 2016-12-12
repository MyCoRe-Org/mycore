/**
 * 
 */
package org.mycore.restapi;

import org.mycore.common.config.MCRConfiguration;

import com.sun.jersey.api.core.ScanningResourceConfig;
import com.sun.jersey.core.spi.scanning.PackageNamesScanner;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRRESTResourceConfig extends ScanningResourceConfig {

    protected String[] packages;

    public MCRRESTResourceConfig() {
        String[] restPackages = MCRConfiguration.instance()
                                            .getStrings("MCR.RestAPI.Resource.Packages")
                                            .stream()
                                            .toArray(String[]::new);
        setPackages(restPackages);
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
