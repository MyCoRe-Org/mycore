/*
 * $Id$
 * $Revision: 5697 $ $Date: 20.02.2012 $
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */

package org.mycore.user2;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.xml.transform.Source;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.jdom.transform.JDOMSource;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRException;
import org.mycore.common.xml.MCRURIResolver;
import org.mycore.common.xml.MCRXMLParserFactory;
import org.mycore.datamodel.ifs2.MCRContent;
import org.xml.sax.SAXParseException;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRRealmFactory {

    static final String RESOURCE_REALMS_URI = "resource:realms.xml";

    static final String REALMS_URI_CFG_KEY = MCRUser2Constants.CONFIG_PREFIX + "Realms.URI";

    private static final Logger LOGGER = Logger.getLogger(MCRRealm.class);

    private static final int REFRESH_DELAY = 5000;

    private static long lastModified = 0;

    /** Map of defined realms, key is the ID of the realm */
    private static HashMap<String, MCRRealm> realmsMap = new HashMap<String, MCRRealm>();

    /** List of defined realms */
    private static List<MCRRealm> realmsList = new ArrayList<MCRRealm>();

    /** The local realm, which is the default realm */
    private static MCRRealm localRealm;

    private static URI realmsURI;

    private static File realmsFile;

    private static long lastChecked;

    private static Document realmsDocument;

    static {
        MCRConfiguration config = MCRConfiguration.instance();
        String dataDirProperty = "MCR.datadir";
        String dataDir = config.getString(dataDirProperty, null);
        if (dataDir == null) {
            LOGGER.warn(dataDirProperty + " is undefined.");
            try {
                realmsURI = new URI(config.getString(REALMS_URI_CFG_KEY, RESOURCE_REALMS_URI));
            } catch (URISyntaxException e) {
                throw new MCRException(e);
            }
        } else {
            File dataDirFile = new File(dataDir);
            String realmsCfg = config.getString(REALMS_URI_CFG_KEY, dataDirFile.toURI().toString() + "realms.xml");
            try {
                realmsURI = new URI(realmsCfg);
                LOGGER.info("Using realms defined in " + realmsURI);
                if ("file".equals(realmsURI.getScheme())) {
                    realmsFile = new File(realmsURI);
                    LOGGER.info("Loading realms from file: " + realmsFile);
                } else {
                    LOGGER.info("Try loading realms with URIResolver for scheme " + realmsURI.toString());
                }
            } catch (URISyntaxException e) {
                throw new MCRException(e);
            }
        }
        loadRealms();
    }

    /**
     * 
     */
    private static void loadRealms() {
        Element root;
        try {
            root = getRealms().getRootElement();
        } catch (Exception e) {
            throw new MCRException("Could not load realms from URI: " + realmsURI, e);
        }
        String localRealmID = root.getAttributeValue("local");
        /** Map of defined realms, key is the ID of the realm */
        HashMap<String, MCRRealm> realmsMap = new HashMap<String, MCRRealm>();

        /** List of defined realms */
        List<MCRRealm> realmsList = new ArrayList<MCRRealm>();

        @SuppressWarnings("unchecked")
        List<Element> realms = (List<Element>) (root.getChildren("realm"));
        for (Element child : realms) {
            String id = child.getAttributeValue("id");
            MCRRealm realm = new MCRRealm(id);

            @SuppressWarnings("unchecked")
            List<Element> labels = (List<Element>) (child.getChildren("label"));
            for (Element label : labels) {
                String text = label.getTextTrim();
                String lang = label.getAttributeValue("lang", Namespace.XML_NAMESPACE);
                realm.setLabel(lang, text);
            }

            realm.setPasswordChangeURL(child.getChildTextTrim("passwordChangeURL"));
            realm.setLoginURL(child.getChild("login").getAttributeValue("url"));

            realmsMap.put(id, realm);
            realmsList.add(realm);
            if (localRealmID.equals(id)) {
                localRealm = realm;
            }
        }
        MCRRealmFactory.realmsDocument = root.getDocument();
        MCRRealmFactory.realmsMap = realmsMap;
        MCRRealmFactory.realmsList = realmsList;
    }

    private static Document getRealms() throws SAXParseException, IOException {
        if (realmsFile == null) {
            return MCRURIResolver.instance().resolve(realmsURI.toString()).getDocument();
        }
        if (!realmsFile.exists()) {
            LOGGER.info("Creating " + realmsFile.getAbsolutePath() + "...");
            Element root = MCRURIResolver.instance().resolve(RESOURCE_REALMS_URI);
            FileOutputStream fout = new FileOutputStream(realmsFile);
            try {
                XMLOutputter xout = new XMLOutputter(Format.getPrettyFormat());
                xout.output(root.getDocument(), fout);
            } finally {
                fout.close();
            }
        }
        return MCRXMLParserFactory.getNonValidatingParser().parseXML(MCRContent.readFrom(realmsFile));
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

    /**
     * Returns a list of all defined realms.
     *  
     * @return a list of all realms.
     */
    public static List<MCRRealm> listRealms() {
        reInitIfNeeded();
        return realmsList;
    }

    public static Document getRealmsDocument() {
        reInitIfNeeded();
        return (Document) realmsDocument.clone();
    }

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
