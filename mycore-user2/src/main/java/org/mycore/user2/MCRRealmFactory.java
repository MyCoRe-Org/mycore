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

package org.mycore.user2;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.transform.JDOMSource;
import org.mycore.common.MCRException;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.content.MCRFileContent;
import org.mycore.common.content.MCRSourceContent;
import org.mycore.common.xml.MCRXMLParserFactory;

/**
 * Handles {@link MCRRealm} instantiation.
 * Will create a file <code>${MCR.datadir}/realms.xml</code> if that file does not exist.
 * You can redefine the location if you define a URI in <code>MCR.users2.Realms.URI</code>.
 * This class monitors the source file for changes and adapts at runtime.
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRRealmFactory {

    static final String RESOURCE_REALMS_URI = "resource:realms.xml";

    static final String REALMS_URI_CFG_KEY = MCRUser2Constants.CONFIG_PREFIX + "Realms.URI";

    private static final Logger LOGGER = LogManager.getLogger();

    private static final int REFRESH_DELAY = 5000;

    private static long lastModified;

    /** Map of defined realms, key is the ID of the realm */
    private static Map<String, MCRRealm> realmsMap = new HashMap<>();

    private static Map<String, MCRUserAttributeMapper> attributeMapper = new HashMap<>();

    /** List of defined realms */
    private static List<MCRRealm> realmsList = new ArrayList<>();

    /** The local realm, which is the default realm */
    private static MCRRealm localRealm;

    private static URI realmsURI;

    private static File realmsFile;

    private static long lastChecked;

    private static Document realmsDocument;

    static {
        String dataDirProperty = "MCR.datadir";
        String dataDir = MCRConfiguration2.getString(dataDirProperty).orElse(null);
        if (dataDir == null) {
            LOGGER.warn("{} is undefined.", dataDirProperty);
            try {
                realmsURI = new URI(MCRConfiguration2.getString(REALMS_URI_CFG_KEY).orElse(RESOURCE_REALMS_URI));
            } catch (URISyntaxException e) {
                throw new MCRException(e);
            }
        } else {
            File dataDirFile = new File(dataDir);
            String realmsCfg = MCRConfiguration2.getString(REALMS_URI_CFG_KEY)
                .orElse(dataDirFile.toURI() + "realms.xml");
            try {
                realmsURI = new URI(realmsCfg);
                LOGGER.info("Using realms defined in {}", realmsURI);
                if ("file".equals(realmsURI.getScheme())) {
                    realmsFile = new File(realmsURI);
                    LOGGER.info("Loading realms from file: {}", realmsFile);
                } else {
                    LOGGER.info("Try loading realms with URIResolver for scheme {}", realmsURI);
                }
            } catch (URISyntaxException e) {
                throw new MCRException(e);
            }
        }
        loadRealms();
    }

    private static void loadRealms() {
        Element root;
        try {
            root = getRealms().getRootElement();
        } catch (JDOMException | TransformerException | IOException e) {
            throw new MCRException("Could not load realms from URI: " + realmsURI, e);
        }
        String localRealmID = root.getAttributeValue("local");
        Map<String, MCRRealm> realmsMap = new HashMap<>();

        Map<String, MCRUserAttributeMapper> attributeMapper = new HashMap<>();

        List<MCRRealm> realmsList = new ArrayList<>();

        List<Element> realms = root.getChildren("realm");
        for (Element child : realms) {
            String id = child.getAttributeValue("id");
            MCRRealm realm = new MCRRealm(id);

            List<Element> labels = child.getChildren("label");
            for (Element label : labels) {
                String text = label.getTextTrim();
                String lang = label.getAttributeValue("lang", Namespace.XML_NAMESPACE);
                realm.setLabel(lang, text);
            }

            realm.setPasswordChangeURL(child.getChildTextTrim("passwordChangeURL"));
            Element login = child.getChild("login");
            if (login != null) {
                realm.setLoginURL(login.getAttributeValue("url"));
                realm.setRedirectParameter(login.getAttributeValue("redirectParameter"));
                realm.setRealmParameter(login.getAttributeValue("realmParameter"));
            }
            Element createElement = child.getChild("create");
            if (createElement != null) {
                realm.setCreateURL(createElement.getAttributeValue("url"));
            }

            attributeMapper.put(id, MCRUserAttributeMapper.createInstance(child));

            realmsMap.put(id, realm);
            realmsList.add(realm);
            if (localRealmID.equals(id)) {
                localRealm = realm;
            }
        }
        realmsDocument = root.getDocument();
        MCRRealmFactory.realmsMap = realmsMap;
        MCRRealmFactory.realmsList = realmsList;
        MCRRealmFactory.attributeMapper = attributeMapper;
    }

    private static Document getRealms() throws JDOMException, TransformerException, IOException {
        if (realmsFile == null) {
            return MCRSourceContent.createInstance(realmsURI.toASCIIString()).asXML();
        }
        if (!realmsFile.exists() || realmsFile.length() == 0) {
            LOGGER.info("Creating {}...", realmsFile::getAbsolutePath);
            MCRSourceContent realmsContent = MCRSourceContent.createInstance(RESOURCE_REALMS_URI);
            realmsContent.sendTo(realmsFile);
        }
        updateLastModified();
        return MCRXMLParserFactory.getNonValidatingParser().parseXML(new MCRFileContent(realmsFile));
    }

    /**
     * Returns the realm with the given ID.
     *
     * @param id the ID of the realm
     * @return the realm with that ID, or null
     */
    public static MCRRealm getRealm(String id) {
        reInitIfNeeded();
        return realmsMap.get(id);
    }

    public static MCRUserAttributeMapper getAttributeMapper(String id) {
        reInitIfNeeded();
        return attributeMapper.get(id);
    }

    /**
     * Returns a list of all defined realms.
     *
     * @return a list of all realms.
     */
    public static List<MCRRealm> listRealms() {
        reInitIfNeeded();
        return realmsList;
    }

    /**
     * Returns the Realms JDOM document clone.
     */
    public static Document getRealmsDocument() {
        reInitIfNeeded();
        return realmsDocument.clone();
    }

    /**
     * Returns the Realms JDOM document as a {@link Source} useful for transformation processes.
     */
    static Source getRealmsSource() {
        reInitIfNeeded();
        return new JDOMSource(realmsDocument);
    }

    /**
     * Returns the local default realm, as specified by the attribute 'local' in realms.xml
     *
     * @return the local default realm.
     */
    public static MCRRealm getLocalRealm() {
        reInitIfNeeded();
        return localRealm;
    }

    private static boolean reloadingRequired() {
        boolean reloading = false;

        long now = System.currentTimeMillis();

        if (now > lastChecked + REFRESH_DELAY) {
            lastChecked = now;
            if (hasChanged()) {
                reloading = true;
            }
        }

        return reloading;
    }

    private static boolean hasChanged() {
        if (realmsFile == null || !realmsFile.exists()) {
            return false;
        }
        return realmsFile.lastModified() > lastModified;
    }

    private static void updateLastModified() {
        if (realmsFile == null || !realmsFile.exists()) {
            return;
        }
        lastModified = realmsFile.lastModified();
    }

    private static void reInitIfNeeded() {
        if (reloadingRequired()) {
            loadRealms();
        }
    }

}
