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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.mycore.common.MCRConstants;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.config.MCRConfiguration2;

/**
 * Represents a realm of users. Each user belongs to a realm. Realms are configured
 * in the file realms.xml. A realm determines the method that is used to login the user.
 * There is always a local default realm, which is defined by the attribute local in realms.xml.
 *
 * @author Frank L\u00fctzenkirchen
 * @author Thomas Scheffler (yagee)
 */
public class MCRRealm {
    /** The unique ID of the realm, e.g. 'local' */
    private String id;

    /** The labels of the realm */
    private Map<String, String> labels = new HashMap<>();

    /** The URL where users from this realm can change their password */
    private String passwordChangeURL;

    /** The URL where users from this realm can login */
    private String loginURL;

    /** The URL where new users may create an account for this realm  */
    private String createURL;

    private String redirectParameter;

    private String realmParameter;

    private static final String DEFAULT_LANG = MCRConfiguration2.getString("MCR.Metadata.DefaultLang")
        .orElse(MCRConstants.DEFAULT_LANG);

    public static final String USER_INFORMATION_ATTR = "realmId";

    /**
     * Creates a new realm.
     *
     * @param id the unique ID of the realm
     */
    MCRRealm(String id) {
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
        String label = labels.get(lang);
        if (label != null) {
            return label;
        }
        label = labels.get(DEFAULT_LANG);
        if (label != null) {
            return label;
        }
        return id;
    }

    /**
     * Sets the label for the given language
     */
    void setLabel(String lang, String label) {
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
    void setPasswordChangeURL(String url) {
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
    void setLoginURL(String url) {
        this.loginURL = url;
    }

    /**
     * @return the createURL
     */
    public String getCreateURL() {
        return createURL;
    }

    /**
     * @param createURL the createURL to set
     */
    void setCreateURL(String createURL) {
        this.createURL = createURL;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof MCRRealm realm && realm.id.equals(id);
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

    /**
     * Returns the URL where users from this realm can login with redirect URL attached.
     * If this realm has a attribut <code>redirectParameter</code> defined this method returns
     * a complete login URL with <code>redirectURL</code> properly configured.
     * @param redirectURL URL where to redirect to after login succeeds.
     * @return the same as {@link #getLoginURL()} if <code>redirectParameter</code> is undefined for this realm
     */
    public String getLoginURL(String redirectURL) {
        Map<String, String> parameter = new LinkedHashMap<>();
        String redirect = getRedirectParameter();
        if (redirect != null && redirectURL != null) {
            parameter.put(redirect, redirectURL);
        }
        String realmParameter = getRealmParameter();
        if (realmParameter != null) {
            parameter.put(realmParameter, getID());
        }
        if (parameter.isEmpty()) {
            return getLoginURL();
        }
        StringBuilder loginURL = new StringBuilder(getLoginURL());
        boolean firstParameter = !getLoginURL().contains("?");
        for (Entry<String, String> entry : parameter.entrySet()) {
            if (firstParameter) {
                loginURL.append('?');
                firstParameter = false;
            } else {
                loginURL.append('&');
            }
            loginURL.append(entry.getKey()).append('=').append(URLEncoder.encode(entry.getValue(),
                StandardCharsets.UTF_8));
        }
        return loginURL.toString();
    }

    /**
     * @return the redirectParameter
     */
    String getRedirectParameter() {
        return redirectParameter;
    }

    /**
     * @param redirectParameter the redirectParameter to set
     */
    void setRedirectParameter(String redirectParameter) {
        this.redirectParameter = redirectParameter;
    }

    public String getRealmParameter() {
        return realmParameter;
    }

    public void setRealmParameter(String realmParameter) {
        this.realmParameter = realmParameter;
    }

}
