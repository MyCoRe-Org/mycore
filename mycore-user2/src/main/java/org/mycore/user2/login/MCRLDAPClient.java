/**
 * $Revision: 23345 $ 
 * $Date: 2012-01-30 12:08:41 +0100 (Mo, 30 Jan 2012) $
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

package org.mycore.user2.login;

import java.util.Hashtable;
import java.util.Locale;

import javax.naming.Context;
import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.mycore.common.MCRUsageException;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.user2.MCRRole;
import org.mycore.user2.MCRRoleManager;
import org.mycore.user2.MCRUser;
import org.mycore.user2.MCRUserManager;
import org.mycore.user2.utils.MCRUserTransformer;

/**
 * Queries an LDAP server for the user's properties.
 * 
 *  # Timeout when connecting to LDAP server 
 *  MCR.user2.LDAP.ReadTimeout=5000
 *  
 *  # LDAP server
 *  MCR.user2.LDAP.ProviderURL=ldap://idp.uni-duisburg-essen.de
 *  
 *  # Security principal for logging in at LDAP server
 *  MCR.user2.LDAP.SecurityPrincipal=cn=duepublico,dc=idp
 *  
 *  # Security credentials for logging in at LDAP server
 *  MCR.user2.LDAP.SecurityCredentials=XXXXXX
 *  
 *  # Base DN
 *  MCR.user2.LDAP.BaseDN=ou=people,dc=idp
 *  
 *  # Filter for user ID
 *  MCR.user2.LDAP.UIDFilter=(uid=%s)
 *  
 *  # LDAP attribute mappings
 *  
 *  # Mapping from LDAP attribute to real name of user
 *  MCR.user2.LDAP.Mapping.Name=cn
 *  
 *  # Mapping from LDAP attribute to E-Mail address of user
 *  MCR.user2.LDAP.Mapping.E-Mail=mail
 *  
 *  # Mapping of any attribute.value combination to group membership of user 
 *  MCR.user2.LDAP.Mapping.Group.eduPersonScopedAffiliation.staff@uni-duisburg-essen.de=creators
 *  
 *  # Default group membership (optional)
 *  MCR.user2.LDAP.Mapping.Group.DefaultGroup=submitters
 *
 * @author Frank L\u00fctzenkirchen
 */
public class MCRLDAPClient {
    /** The logger */
    private static Logger LOGGER = LogManager.getLogger(MCRLDAPClient.class);

    private static MCRLDAPClient instance = new MCRLDAPClient();

    /** Base DN */
    private String baseDN;

    /** Filter for user ID */
    private String uidFilter;

    /** Mapping from LDAP attribute to real name of user */
    private String mapName;

    /** Mapping from LDAP attribute to E-Mail address of user */
    private String mapEMail;

    /** Default group of user */
    private MCRRole defaultGroup;

    private Hashtable<String, String> ldapEnv;

    public static MCRLDAPClient instance() {
        return instance;
    }

    private MCRLDAPClient() {
        MCRConfiguration config = MCRConfiguration.instance();

        String prefix = "MCR.user2.LDAP.";
        /* Timeout when connecting to LDAP server */
        String readTimeout = config.getString(prefix + "ReadTimeout", "10000");
        /* LDAP server */
        String providerURL = config.getString(prefix + "ProviderURL");
        /* Security principal for logging in at LDAP server */
        String securityPrincipal = config.getString(prefix + "SecurityPrincipal");
        /* Security credentials for logging in at LDAP server */
        String securityCredentials = config.getString(prefix + "SecurityCredentials");
        baseDN = config.getString(prefix + "BaseDN");
        uidFilter = config.getString(prefix + "UIDFilter");

        prefix += "Mapping.";
        mapName = config.getString(prefix + "Name");
        mapEMail = config.getString(prefix + "E-Mail");

        String group = config.getString(prefix + "Group.DefaultGroup", null);
        if (group != null)
            defaultGroup = MCRRoleManager.getRole(group);

        ldapEnv = new Hashtable<String, String>();
        ldapEnv.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        ldapEnv.put("com.sun.jndi.ldap.read.timeout", readTimeout);
        ldapEnv.put(Context.PROVIDER_URL, providerURL);
        ldapEnv.put(Context.SECURITY_AUTHENTICATION, "simple");
        ldapEnv.put(Context.SECURITY_PRINCIPAL, securityPrincipal);
        ldapEnv.put(Context.SECURITY_CREDENTIALS, securityCredentials);
    }

    public boolean updateUserProperties(MCRUser user) throws NamingException {
        String userName = user.getUserName();
        boolean userChanged = false;

        if ((defaultGroup != null) && (!user.isUserInRole((defaultGroup.getName())))) {
            LOGGER.info("User " + userName + " add to group " + defaultGroup);
            userChanged = true;
            user.assignRole(defaultGroup.getName());
        }

        // Get user properties from LDAP server
        DirContext ctx = new InitialDirContext(ldapEnv);

        try {
            SearchControls controls = new SearchControls();
            controls.setSearchScope(SearchControls.SUBTREE_SCOPE);
            NamingEnumeration<SearchResult> results = ctx.search(baseDN,
                String.format(Locale.ROOT, uidFilter, userName), controls);

            while (results.hasMore()) {
                SearchResult searchResult = results.next();
                Attributes attributes = searchResult.getAttributes();

                for (NamingEnumeration<String> attributeIDs = attributes.getIDs(); attributeIDs.hasMore();) {
                    String attributeID = attributeIDs.next();
                    Attribute attribute = attributes.get(attributeID);

                    for (NamingEnumeration<?> values = attribute.getAll(); values.hasMore();) {
                        String attributeValue = values.next().toString();
                        LOGGER.debug(attributeID + "=" + attributeValue);

                        if (attributeID.equals(mapName) && (user.getRealName() == null)) {
                            attributeValue = formatName(attributeValue);
                            LOGGER.info("User " + userName + " name = " + attributeValue);
                            user.setRealName(attributeValue);
                            userChanged = true;
                        }
                        if (attributeID.equals(mapEMail) && (user.getEMailAddress() == null)) {
                            LOGGER.info("User " + userName + " e-mail = " + attributeValue);
                            user.setEMail(attributeValue);
                            userChanged = true;
                        }
                        String groupMapping = "MCR.user2.LDAP.Mapping.Group." + attributeID + "." + attributeValue;
                        String group = MCRConfiguration.instance().getString(groupMapping, null);
                        if ((group != null) && (!user.isUserInRole((group)))) {
                            LOGGER.info("User " + userName + " add to group " + group);
                            user.assignRole(group);
                            userChanged = true;
                        }
                    }
                }
            }
        } catch (NameNotFoundException ex) {
            String msg = "LDAP base name not found: " + ex.getMessage() + " " + ex.getExplanation();
            throw new MCRConfigurationException(msg, ex);
        } catch (NamingException ex) {
            String msg = "Exception accessing LDAP server";
            throw new MCRUsageException(msg, ex);
        } finally {
            if (ctx != null) {
                try {
                    ctx.close();
                } catch (Exception ignored) {
                }
            }
        }

        return userChanged;
    }

    /**
     * Formats a user name into "lastname, firstname" syntax.
     */
    private String formatName(String name) {
        name = name.replaceAll("\\s+", " ").trim();
        if (name.contains(","))
            return name;
        int pos = name.lastIndexOf(' ');
        if (pos == -1)
            return name;
        return name.substring(pos + 1, name.length()) + ", " + name.substring(0, pos);
    }

    public static void main(String[] args) throws Exception {
        String userName = args[0];
        String realmID = args[1];
        MCRUser user = MCRUserManager.getUser(userName, realmID);
        if (user == null) {
            user = new MCRUser(userName, realmID);
        }

        LOGGER.info("\n"
            + new XMLOutputter(Format.getPrettyFormat()).outputString(MCRUserTransformer.buildExportableSafeXML(user)));
        MCRLDAPClient.instance().updateUserProperties(user);
        LOGGER.info("\n"
            + new XMLOutputter(Format.getPrettyFormat()).outputString(MCRUserTransformer.buildExportableSafeXML(user)));
    }
}
