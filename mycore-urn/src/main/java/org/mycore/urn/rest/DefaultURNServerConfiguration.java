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

package org.mycore.urn.rest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Deprecated
public class DefaultURNServerConfiguration implements URNServerConfiguration {

    static final Logger LOGGER = LogManager.getLogger(DefaultURNServerConfiguration.class);

    /**
     * The base url for the urn registration service 
     */
    static final String SERVICE_BASE_URL = "https://restapi.nbn-resolving.org/";

    /**
     * The url for registering/updating urn
     */
    static final String SERVICE_URN_URL = SERVICE_BASE_URL + "urns/";

    private static final String LOGIN;

    private static final String PASSWORD;

    /**
     * Field holding login data
     */
    private String login;

    /**
     * Field holding password data
     */
    private String password;

    private String serviceURL;

    static {
        LOGIN = org.mycore.common.config.MCRConfiguration.instance().getString("MCR.URN.DNB.Credentials.Login");
        PASSWORD = org.mycore.common.config.MCRConfiguration.instance().getString("MCR.URN.DNB.Credentials.Password");
    }

    /**
     * Creates a new configuration with login and password from mycore.properties.<br>
     * MCR.URN.DNB.Credentials.Login<br>
     * MCR.URN.DNB.Credentials.Password
     */
    public DefaultURNServerConfiguration() {
        this.login = LOGIN;
        this.password = PASSWORD;
        this.serviceURL = SERVICE_URN_URL;
    }

    @Override
    public String getLogin() {
        return this.login;
    }

    @Override
    public String getPassword() {
        return this.password;
    }

    @Override
    public String getServiceURL() {
        return this.serviceURL;
    }

    /**
     * Sets a new login name.
     * 
     * @param login name of the login
     * @return itself
     */
    public DefaultURNServerConfiguration setLogin(String login) {
        this.login = login;
        return this;
    }

    /**
     * Sets a new password.
     * 
     * @return itself
     */
    public DefaultURNServerConfiguration setPassword(String password) {
        this.password = password;
        return this;
    }

    public DefaultURNServerConfiguration setServiceURL(String serviceURL) {
        this.serviceURL = serviceURL;
        return this;
    }

}
