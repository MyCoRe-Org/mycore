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

package org.mycore.user2.login;

import java.util.Hashtable;
import java.util.Locale;
import java.util.Optional;

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
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.user2.MCRRole;
import org.mycore.user2.MCRRoleManager;
import org.mycore.user2.MCRUser;
import org.mycore.user2.MCRUserManager;
import org.mycore.user2.utils.MCRUserTransformer;

/**
 * Queries an LDAP server for the user's properties.
 *
 * <p>Configuration properties for connecting to the LDAP server:</p>
 *
 * <ul>
 *   <li><b>Timeout when connecting to LDAP server:</b>
 *     <pre>MCR.user2.LDAP.ReadTimeout=5000</pre>
 *   </li>
 *   <li><b>LDAP server:</b>
 *     <pre>MCR.user2.LDAP.ProviderURL=ldap://idp.uni-duisburg-essen.de</pre>
 *   </li>
 *   <li><b>Security principal for logging into the LDAP server:</b>
 *     <pre>MCR.user2.LDAP.SecurityPrincipal=cn=duepublico,dc=idp</pre>
 *   </li>
 *   <li><b>Security credentials for logging into the LDAP server:</b>
 *     <pre>MCR.user2.LDAP.SecurityCredentials=XXXXXX</pre>
 *   </li>
 *   <li><b>Base DN:</b>
 *     <pre>MCR.user2.LDAP.BaseDN=ou=people,dc=idp</pre>
 *   </li>
 *   <li><b>Filter for user ID:</b>
 *     <pre>MCR.user2.LDAP.UIDFilter=(uid=%s)</pre>
 *   </li>
 * </ul>
 *
 * <p>LDAP attribute mappings:</p>
 *
 * <ul>
 *   <li><b>Mapping from LDAP attribute to real name of user:</b>
 *     <pre>MCR.user2.LDAP.Mapping.Name=cn</pre>
 *   </li>
 *   <li><b>Mapping from LDAP attribute to email address of user:</b>
 *     <pre>MCR.user2.LDAP.Mapping.E-Mail=mail</pre>
 *   </li>
 *   <li><b>Mapping of any attribute.value combination to group membership of user:</b>
 *     <pre>MCR.user2.LDAP.Mapping.Group.eduPersonScopedAffiliation.staff@uni-duisburg-essen.de=creators</pre>
 *   </li>
 *   <li><b>Default group membership (optional):</b>
 *     <pre>MCR.user2.LDAP.Mapping.Group.DefaultGroup=submitters</pre>
 *   </li>
 * </ul>
 *
 * @author Frank Lützenkirchen
 */
public final class MCRLDAPClient {

    private static final Logger LOGGER = LogManager.getLogger();

    /** Base DN */
    private final String baseDN;

    /** Filter for user ID */
    private final String uidFilter;

    /** Mapping from LDAP attribute to real name of user */
    private final String mapName;

    /** Mapping from LDAP attribute to E-Mail address of user */
    private final String mapEMail;

    /** Default group of user */
    private MCRRole defaultGroup;

    @SuppressWarnings({ "PMD.LooseCoupling", "PMD.ReplaceHashtableWithMap" })
    private final Hashtable<String, String> ldapEnv;

    @SuppressWarnings("PMD.ReplaceHashtableWithMap")
    private MCRLDAPClient() {
        String prefix = "MCR.user2.LDAP.";
        /* Timeout when connecting to LDAP server */
        String readTimeout = MCRConfiguration2.getString(prefix + "ReadTimeout").orElse("10000");
        /* LDAP server */
        String providerURL = MCRConfiguration2.getStringOrThrow(prefix + "ProviderURL");
        /* Security principal for logging in at LDAP server */
        String securityPrincipal = MCRConfiguration2.getStringOrThrow(prefix + "SecurityPrincipal");
        /* Security credentials for logging in at LDAP server */
        String securityCredentials = MCRConfiguration2.getStringOrThrow(prefix + "SecurityCredentials");
        baseDN = MCRConfiguration2.getStringOrThrow(prefix + "BaseDN");
        uidFilter = MCRConfiguration2.getStringOrThrow(prefix + "UIDFilter");

        prefix += "Mapping.";
        mapName = MCRConfiguration2.getStringOrThrow(prefix + "Name");
        mapEMail = MCRConfiguration2.getStringOrThrow(prefix + "E-Mail");

        String group = MCRConfiguration2.getString(prefix + "Group.DefaultGroup").orElse(null);
        if (group != null) {
            defaultGroup = MCRRoleManager.getRole(group);
        }

        ldapEnv = new Hashtable<>();
        ldapEnv.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        ldapEnv.put("com.sun.jndi.ldap.read.timeout", readTimeout);
        ldapEnv.put(Context.PROVIDER_URL, providerURL);
        ldapEnv.put(Context.SECURITY_AUTHENTICATION, "simple");
        ldapEnv.put(Context.SECURITY_PRINCIPAL, securityPrincipal);
        ldapEnv.put(Context.SECURITY_CREDENTIALS, securityCredentials);
    }


    /**
     * @deprecated Use {@link #getInstance()} instead
     */
    @Deprecated
    public static MCRLDAPClient instance() {
        return getInstance();
    }

    public static MCRLDAPClient getInstance() {
        return LazyInstanceHolder.SINGLETON_INSTANCE;
    }

    public static void main(String[] args) throws Exception {
        String userName = args[0];
        String realmID = args[1];
        MCRUser user = Optional.ofNullable(MCRUserManager.getUser(userName, realmID))
            .orElseGet(() -> new MCRUser(userName, realmID));

        LOGGER.info("\n{}",
            () -> new XMLOutputter(Format.getPrettyFormat())
                .outputString(MCRUserTransformer.buildExportableSafeXML(user)));
        getInstance().updateUserProperties(user);
        LOGGER.info("\n{}",
            () -> new XMLOutputter(Format.getPrettyFormat())
                .outputString(MCRUserTransformer.buildExportableSafeXML(user)));
    }

    public boolean updateUserProperties(MCRUser user) throws NamingException {
        String userName = user.getUserName();
        boolean userChanged = false;

        if ((defaultGroup != null) && (!user.isUserInRole((defaultGroup.getName())))) {
            LOGGER.info("User {} add to group {}", userName, defaultGroup);
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
                        LOGGER.debug("{}={}", attributeID, attributeValue);

                        if (attributeID.equals(mapName) && (user.getRealName() == null)) {
                            attributeValue = formatName(attributeValue);
                            LOGGER.info("User {} name = {}", userName, attributeValue);
                            user.setRealName(attributeValue);
                            userChanged = true;
                        }
                        if (attributeID.equals(mapEMail) && (user.getEMailAddress() == null)) {
                            LOGGER.info("User {} e-mail = {}", userName, attributeValue);
                            user.setEMail(attributeValue);
                            userChanged = true;
                        }
                        String groupMapping = "MCR.user2.LDAP.Mapping.Group." + attributeID + "." + attributeValue;
                        String group = MCRConfiguration2.getString(groupMapping).orElse(null);
                        if ((group != null) && (!user.isUserInRole((group)))) {
                            LOGGER.info("User {} add to group {}", userName, group);
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
        String trimmedName = name.replaceAll("\\s+", " ").trim();
        if (trimmedName.contains(",")) {
            return trimmedName;
        }
        int pos = name.lastIndexOf(' ');
        if (pos == -1) {
            return trimmedName;
        }
        return trimmedName.substring(pos + 1) + ", " + trimmedName.substring(0, pos);
    }

    private static final class LazyInstanceHolder {
        public static final MCRLDAPClient SINGLETON_INSTANCE = new MCRLDAPClient();
    }

}
