/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mycore.frontend.filter;

import java.net.URISyntaxException;
import java.util.Collection;
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

    private static Logger LOGGER = LogManager.getLogger();

    static {
        MCRConfiguration configuration = MCRConfiguration.instance();
        List<String> propertyValues = configuration.getStrings("MCR.SecureTokenV2.Extensions",
            Collections.emptyList());
        if (propertyValues.isEmpty()) {
            enabled = false;
            LOGGER.info("Local MCRSecureToken2 support is disabled.");
        } else {
            enabled = true;
            securedExtensions = getExtensionPattern(propertyValues);
            LOGGER.info("SecureTokenV2 extension pattern: {}", securedExtensions);
            hashParameter = configuration.getString("MCR.SecureTokenV2.ParameterName");
            sharedSecret = configuration.getString("MCR.SecureTokenV2.SharedSecret").trim();
        }
    }

    static Pattern getExtensionPattern(Collection<String> propertyValues) {
        return Pattern.compile(propertyValues
            .stream()
            .map(s -> s.toLowerCase(Locale.ROOT))
            .distinct()
            .collect(Collectors.joining("|", "(.+(\\.(?i)(", "))$)")));
    }

    public static boolean isFilterEnabled() {
        return enabled;
    }

    public static String getHashParameterName() {
        return hashParameter;
    }

    public static String getSharedSecret() {
        return sharedSecret;
    }

    public static boolean requireHash(String filename) {
        return enabled && securedExtensions.matcher(filename).matches();
    }

    public static String getFileNodeServletSecured(MCRObjectID derivate, String path) {
        return getFileNodeServletSecured(derivate, path, MCRFrontendUtil.getBaseURL());
    }

    public static String getFileNodeServletSecured(MCRObjectID derivate, String path, String baseURL) {
        String fileNodeBaseURL = baseURL + "servlets/MCRFileNodeServlet/";
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
