/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
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

package org.mycore.frontend;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.DOMOutputter;
import org.jdom2.output.XMLOutputter;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRSystemUserInformation;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.datamodel.metadata.MCRXMLConstants;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public final class MCRWebsiteWriteProtection {

    private static final String FS = FileSystems.getDefault().getSeparator();

    private static final String CONFIG_FOLDER_PATH = MCRConfiguration2.getStringOrThrow("MCR.datadir") + FS
        + "config";
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String CONFIG_FILE_PATH = CONFIG_FOLDER_PATH + FS + "config-writeProtectionWebsite.xml";

    private static final File CONFIG_FILE = new File(CONFIG_FILE_PATH);

    private static final String ELEMENT_PROTECTION_ENABLED = "protectionEnabled";

    private static final String ELEMENT_MESSAGE = "message";

    private static long cacheInitTime;

    private static Element configCache;

    private MCRWebsiteWriteProtection() {
        //do not allow instantiation
    }

    /**
     * Checks if website protection is currently active.
     * If current user is super user this method always returns false.
     *
     * @return true if write access is currently active, false if not
     */
    public static boolean isActive() {
        // if superuser is online, return false
        String superUser = MCRSystemUserInformation.SUPER_USER.getUserID();
        if (MCRSessionMgr.getCurrentSession().getUserInformation().getUserID().equals(superUser)) {
            return false;
        }
        // init, if impossible return false
        Element config = getConfiguration();
        if (config == null) {
            return false;
        }
        // return value contained in config
        String protection = config.getChildTextTrim(ELEMENT_PROTECTION_ENABLED);
        return Boolean.parseBoolean(protection);
    }

    public static org.w3c.dom.Document getMessage() throws JDOMException {
        Element config = getConfiguration();
        if (config == null) {
            return new DOMOutputter().output(new Document());
        } else {
            Element messageElem = config.getChild(ELEMENT_MESSAGE);
            Document message = new Document(messageElem.clone());
            return new DOMOutputter().output(message);
        }
    }

    private static Element getConfiguration() {
        // try to get file
        File configFolder = new File(CONFIG_FOLDER_PATH);
        if (!configFolder.exists()) {
            configFolder.mkdirs();
        }
        // file exist?, return it's content
        if (CONFIG_FILE.exists()) {
            Element config;
            // try to get from cache
            if (cacheValid()) {
                config = configCache;
            } else {
                SAXBuilder builder = new SAXBuilder();
                try {
                    config = builder.build(CONFIG_FILE).getRootElement();
                    // update cache
                    updateCache(config);
                } catch (JDOMException | IOException e) {
                    LOGGER.debug(e);
                    return null;
                }
            }
            return config;
        } else {
            // create XML
            Element config = configToJDOM(false, " ");
            setConfiguration(config);
            return config;
        }
    }

    private static void setConfiguration(Element configXML) {
        try {
            XMLOutputter xmlOut = new XMLOutputter();
            try(OutputStream fileOutputStream = Files.newOutputStream(CONFIG_FILE.toPath())) {
                xmlOut.output(configXML, fileOutputStream);
            }
        } catch (IOException e) {
            LOGGER.debug(e);
        }
        updateCache(configXML);
    }

    private static void updateCache(Element configXML) {
        configCache = configXML;
        cacheInitTime = System.currentTimeMillis();
    }

    private static Element configToJDOM(boolean protection, String message) {
        Element xml = new Element("config-writeProtectionWebsite");
        xml.addContent(new Element(ELEMENT_PROTECTION_ENABLED).setText(Boolean.toString(protection)));
        xml.addContent(new Element(ELEMENT_MESSAGE).setText(message));
        return xml;
    }

    // to be used by cli
    public static void activate() {
        // create file, set param in file to true, add message to file
        Element config = getConfiguration();
        config.getChild(ELEMENT_PROTECTION_ENABLED).setText(MCRXMLConstants.TRUE);
        setConfiguration(config);
    }

    // to be used by cli
    public static void activate(String message) {
        // create file, set param in file to true, add message to file
        Element config = getConfiguration();
        config.getChild(ELEMENT_PROTECTION_ENABLED).setText(MCRXMLConstants.TRUE);
        config.getChild(ELEMENT_MESSAGE).setText(message);
        setConfiguration(config);
    }

    // to be used by cli
    public static void deactivate() {
        // set param in file to false
        Element config = getConfiguration();
        config.getChild(ELEMENT_PROTECTION_ENABLED).setText(MCRXMLConstants.FALSE);
        setConfiguration(config);
    }

    public static boolean printInfoPageIfNoAccess(HttpServletRequest request, HttpServletResponse response,
        String baseURL) throws IOException {
        if (isActive()) {
            response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
            String pageURL = baseURL + MCRConfiguration2.getStringOrThrow("MCR.WriteProtectionWebsite.ErrorPage");
            response.sendRedirect(response.encodeRedirectURL(pageURL));
            return true;
        }
        return false;
    }

    /**
     * Verifies if the cache of configuration is valid.
     *
     * @return true if valid, false if note
     */
    private static boolean cacheValid() {
        return !(configCache == null || cacheInitTime < CONFIG_FILE.lastModified());
    }

}
