/**
 * 
 */
package org.mycore.common.xml;

import org.apache.log4j.Logger;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRConfigurationException;

/**
 * @author shermann
 */
public class MCRPropertyFunctions {

    private static MCRConfiguration CONFIG = MCRConfiguration.instance();

    /**
     * @param property
     * @return
     */
    public static String getProperty(String property) {

        String value = "";
        try {
            value = CONFIG.getString(property);
        } catch (MCRConfigurationException ex) {
            Logger.getLogger(MCRPropertyFunctions.class).warn("The requested property '" + property + "' could not be found.");
            return "";
        }
        return value;
    }

    /**
     * @param property
     * @return
     */
    public static String getProperty(String property, String defaultValue) {

        String value = "";
        try {
            value = CONFIG.getString(property, defaultValue);
        } catch (MCRConfigurationException ex) {
            Logger.getLogger(MCRPropertyFunctions.class).warn("The requested property '" + property + "' could not be found.");
            return "";
        }
        return value;
    }

    /**
     * @param url
     * @param lookup
     * @return
     */
    public static String getParameterValue(String url, String lookup) {
        String parameters = url.substring(url.indexOf("?") + 1);
        String[] params = parameters.split("&");
        for (String val : params) {
            if (val.startsWith(lookup + "=")) {
                String toReturn = val.substring(val.indexOf("=") + 1);
                return toReturn;
            }
        }
        return null;
    }
}
