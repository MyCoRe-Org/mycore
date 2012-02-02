/**
 * $Revision$ 
 * $Date$
 *
 * This file is part of the MILESS repository software.
 * Copyright (C) 2011 MILESS/MyCoRe developer team
 * See http://duepublico.uni-duisburg-essen.de/ and http://www.mycore.de/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 **/

package org.mycore.user2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.jdom.Element;
import org.jdom.Namespace;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.xml.MCRURIResolver;

/**
 * Represents a realm of users. Each user belongs to a realm. Realms are configured 
 * in the file realms.xml. A realm determines the method that is used to login the user.
 * There is always a local default realm, which is defined by the attribute local in realms.xml.
 * 
 * @author Frank L\u00fctzenkirchen
 */
public class MCRRealm {
    /** Map of defined realms, key is the ID of the realm */
    private static HashMap<String, MCRRealm> realmsMap = new HashMap<String, MCRRealm>();

    /** List of defined realms */
    private static List<MCRRealm> realmsList = new ArrayList<MCRRealm>();

    /** The local realm, which is the default realm */
    private static MCRRealm localRealm;

    static {
        String configurationFile = "resource:realms.xml";
        Element root = MCRURIResolver.instance().resolve(configurationFile);
        String localRealmID = root.getAttributeValue("local");

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
            if (localRealmID.equals(id))
                localRealm = realm;
        }
    }

    /** The unique ID of the realm, e.g. 'local' */
    private String id;

    /** The labels of the realm */
    private HashMap<String, String> labels = new HashMap<String, String>();

    /** The URL where users from this realm can change their password */
    private String passwordChangeURL;

    /** The URL where users from this realm can login */
    private String loginURL;

    /** 
     * Creates a new realm.
     * 
     * @param id the unique ID of the realm
     */
    private MCRRealm(String id) {
        this.id = id;
    }

    /**
     * Returns the unique ID of the realm.
     * 
     * @return the unique ID of the realm.
     */
    public String getID() {
        return id;
    }

    /**
     * Returns the label in the current language
     */
    public String getLabel() {
        String lang = MCRSessionMgr.getCurrentSession().getCurrentLanguage();
        return labels.get(lang);
    }

    /**
     * Sets the label for the given language
     */
    private void setLabel(String lang, String label) {
        labels.put(lang, label);
    }

    /** 
     * Returns the URL where users from this realm can change their password 
     */
    public String getPasswordChangeURL() {
        return passwordChangeURL;
    }

    /** 
     * Sets the URL where users from this realm can change their password 
     */
    private void setPasswordChangeURL(String url) {
        this.passwordChangeURL = url;
    }

    /** 
     * Returns the URL where users from this realm can login 
     */
    public String getLoginURL() {
        return loginURL;
    }

    /** 
     * Sets the URL where users from this realm can login 
     */
    private void setLoginURL(String url) {
        this.loginURL = url;
    }

    /**
     * Returns the realm with the given ID.
     * 
     * @param id the ID of the realm
     * @return the realm with that ID, or null
     */
    public static MCRRealm getRealm(String id) {
        return realmsMap.get(id);
    }

    /**
     * Returns a list of all defined realms.
     *  
     * @return a list of all realms.
     */
    public static List<MCRRealm> listRealms() {
        return realmsList;
    }

    /**
     * Returns the local default realm, as specified by the attribute 'local' in realms.xml
     * 
     * @return the local default realm.
     */
    public static MCRRealm getLocalRealm() {
        return localRealm;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof MCRRealm)
            return ((MCRRealm) obj).id.equals(id);
        else
            return false;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "id=" + id + ", " + (loginURL != null ? "loginURL=" + loginURL : "");
    }
}
