package org.mycore.frontend.filter;

import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRException;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.datamodel.ifs.MCRFileNodeServlet;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.frontend.MCRFrontendUtil;
import org.mycore.frontend.support.MCRSecureTokenV2;

public class MCRSecureTokenV2FilterConfig {
    private static boolean enabled;

    private static String hashParameter;

    private static String sharedSecret;

    private static Pattern securedExtensions;
    
    private static Logger LOGGER=LogManager.getLogger();

    static {
        MCRConfiguration configuration = MCRConfiguration.instance();
        List<String> propertyValues = configuration.getStrings("MCR.SecureTokenV2.Extensions",
            Collections.emptyList());
        if (propertyValues.isEmpty()) {
            enabled = false;
            LOGGER.info("Local MCRSecureToken2 support is disabled.");
        } else {
            enabled = true;
            securedExtensions = Pattern.compile(propertyValues
                .stream()
                .map(s -> s.toLowerCase(Locale.ROOT))
                .distinct()
                .collect(Collectors.joining("|", "([^\\s]+(\\.(?i)(", "))$)")));
            LOGGER.info("SecureTokenV2 extension pattern: " + securedExtensions);
            hashParameter = configuration.getString("MCR.SecureTokenV2.ParameterName");
            sharedSecret = configuration.getString("MCR.SecureTokenV2.SharedSecret").trim();
        }
    }

    public static boolean isFilterEnabled() {
        return enabled;
    }

    public static boolean requireHash(String filename) {
        return enabled && securedExtensions.matcher(filename).matches();
    }

    public static String getFileNodeServletSecured(MCRObjectID derivate, String path) {
        String fileNodeBaseURL = MCRFrontendUtil.getBaseURL() + "servlets/MCRFileNodeServlet/";
        if (requireHash(path)) {
            MCRSecureTokenV2 token = new MCRSecureTokenV2(derivate + "/" + path,
                MCRSessionMgr.getCurrentSession().getCurrentIP(), sharedSecret);
            try {
                return token.toURI(fileNodeBaseURL, hashParameter).toString();
            } catch (URISyntaxException e) {
                throw new MCRException("Could not find out URL to " + MCRFileNodeServlet.class.getSimpleName(), e);
            }
        } else {
            return fileNodeBaseURL + derivate + "/" + path;
        }
    }

}