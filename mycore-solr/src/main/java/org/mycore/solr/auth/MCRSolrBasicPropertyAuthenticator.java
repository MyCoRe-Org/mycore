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

package org.mycore.solr.auth;

import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRProperty;

import java.util.function.Supplier;

/**
 * Basic authentication implementation that uses the username and password from the configuration.
 * The username and password are configured using the properties {@code Username} and {@code Password}.
 */
@MCRConfigurationProxy(proxyClass = MCRSolrBasicPropertyAuthenticator.Factory.class)
public final class MCRSolrBasicPropertyAuthenticator extends MCRSolrBasicAuthenticatorBase {

    public static final String USERNAME_KEY = "Username";

    public static final String PASSWORD_KEY = "Password";

    private final String username;

    private final String password;

    public MCRSolrBasicPropertyAuthenticator(String username, String password) {
        this.username = username;
        this.password = password;
    }

    @Override
    protected String getUsername() {
        return username;
    }

    @Override
    protected String getPassword() {
        return password;
    }
    
    public static class Factory implements Supplier<MCRSolrBasicPropertyAuthenticator> {

        @MCRProperty(name = USERNAME_KEY)
        public String username;

        @MCRProperty(name = PASSWORD_KEY)
        public String password;

        @Override
        public MCRSolrBasicPropertyAuthenticator get() {
            return new MCRSolrBasicPropertyAuthenticator(username, password);
        }

    }

}
