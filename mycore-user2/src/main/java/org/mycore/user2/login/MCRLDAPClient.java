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

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import javax.naming.Context;
import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRUsageException;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRInstance;
import org.mycore.common.config.annotation.MCRPostConstruction;
import org.mycore.common.config.annotation.MCRProperty;
import org.mycore.user2.MCRRoleManager;
import org.mycore.user2.MCRUser;

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
@SuppressWarnings({"PMD.ReplaceHashtableWithMap"})
@MCRConfigurationProxy(proxyClass = MCRLDAPClient.Factory.class)
public final class MCRLDAPClient {

    private static final Logger LOGGER = LogManager.getLogger();

    public static final String CLIENT_PROPERTY = "MCR.user2.LDAP";

    private final SearchSettings searchSettings;

    @SuppressWarnings({"PMD.LooseCoupling"})
    private final Hashtable<String, String> ldapSettings;

    public MCRLDAPClient(ConnectionSettings connectionSettings, SearchSettings searchSettings) {
        this(Objects.requireNonNull(connectionSettings, "Connection settings must not be null")
            .toLdapSettings(), searchSettings);
    }

    public MCRLDAPClient(Map<String, String> ldapSettings, SearchSettings searchSettings) {
        this.ldapSettings = new Hashtable<>(Objects.requireNonNull(ldapSettings, "LDAP settings must not be null"));
        this.ldapSettings.forEach((key, value) ->
            Objects.requireNonNull(value, "LDAP Setting " + key + " must not be null"));
        this.searchSettings = Objects.requireNonNull(searchSettings, "Search settings must not be null");
    }

    /**
     * @deprecated Use {@link #obtainInstance()} instead
     */
    @Deprecated
    public static MCRLDAPClient instance() {
        return obtainInstance();
    }

    public static MCRLDAPClient obtainInstance() {
        return LazyInstanceHolder.SHARED_INSTANCE;
    }

    public static MCRLDAPClient createInstance() {
        String classProperty = CLIENT_PROPERTY + ".Class";
        return MCRConfiguration2.getInstanceOfOrThrow(MCRLDAPClient.class, classProperty);
    }

    public boolean updateUserProperties(MCRUser user) throws NamingException {

        String userName = user.getUserName();
        boolean userChanged = false;

        if (searchSettings.defaultGroup.isPresent()) {
            String defaultGroup = searchSettings.defaultGroup.get();
            if (!user.isUserInRole(defaultGroup)) {
                LOGGER.info("Adding {} to group {}", userName, defaultGroup);
                user.assignRole(MCRRoleManager.getRole(defaultGroup).getName());
                userChanged = true;
            }
        }

        DirContext ldapContext = new InitialDirContext(ldapSettings);

        try {

            SearchControls controls = new SearchControls();
            controls.setSearchScope(SearchControls.SUBTREE_SCOPE);
            NamingEnumeration<SearchResult> results = ldapContext.search(searchSettings.baseDn,
                String.format(Locale.ROOT, searchSettings.uidFilter, userName), controls);

            while (results.hasMore()) {

                SearchResult searchResult = results.next();
                Attributes attributes = searchResult.getAttributes();
                NamingEnumeration<String> attributeIDs = attributes.getIDs();

                while (attributeIDs.hasMore()) {

                    String attributeId = attributeIDs.next();
                    javax.naming.directory.Attribute attribute = attributes.get(attributeId);
                    NamingEnumeration<?> values = attribute.getAll();

                    while (values.hasMore()) {

                        String attributeValue = values.next().toString();
                        LOGGER.debug("{}={}", attributeId, attributeValue);

                        if (user.getRealName() == null && attributeId.equals(searchSettings.nameAttributeId)) {
                            String formattedName = formatName(attributeValue);
                            LOGGER.info("Updating name of {} to {}", userName, formattedName);
                            user.setRealName(formattedName);
                            userChanged = true;
                        }
                        if (user.getEMailAddress() == null && attributeId.equals(searchSettings.emailAttributeId)) {
                            LOGGER.info("Updating e-mail of {} to {}", userName, attributeValue);
                            user.setEMail(attributeValue);
                            userChanged = true;
                        }

                        String group = searchSettings.groupMappings.get(attributeId + "." + attributeValue);
                        if (group != null && !user.isUserInRole(group)) {
                            LOGGER.info("Adding {} to group {}", userName, group);
                            user.assignRole(MCRRoleManager.getRole(group).getName());
                            userChanged = true;
                        }
                    }
                }
            }
        } catch (NameNotFoundException e) {
            throw new MCRConfigurationException("LDAP base name not found: "
                + e.getMessage() + " " + e.getExplanation(), e);
        } catch (NamingException e) {
            throw new MCRUsageException("Exception accessing LDAP server", e);
        } finally {
            try {
                ldapContext.close();
            } catch (Exception e) {
                LOGGER.warn("Failed to close LDAP context", e);
            }
        }

        return userChanged;
    }

    /**
     * Formats a users name into "lastname, firstname" syntax, if possible.
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

    public interface SecuritySettings {

        Map<String, String> toLdapSettings();

        enum Authentication {

            NONE,

            EXTERNAL,

            SIMPLE;

        }

        enum Protocol {

            PLAIN,

            SSL;

        }

        /**
         * No authentication (anonymous)
         * @see <a href="https://datatracker.ietf.org/doc/html/rfc4513#section-5.1.1">RFC 4513 / 5.1.1</a>
         */
        record None(Protocol protocol) implements SecuritySettings {

            public None {
                Objects.requireNonNull(protocol, "Protocol must not be null");
            }

            @Override
            public Map<String, String> toLdapSettings() {
                Map<String, String> ldapSettings = new HashMap<>();
                ldapSettings.put(Context.SECURITY_AUTHENTICATION, "none");
                ldapSettings.put(Context.SECURITY_PROTOCOL, protocol.name().toLowerCase(Locale.ROOT));
                return ldapSettings;
            }

        }

        /**
         * External SASL authentication
         * @see <a href="https://datatracker.ietf.org/doc/html/rfc4513#section-5.2.3">RFC 4513 / 5.2.3</a>
         */
        record External() implements SecuritySettings {

            @Override
            public Map<String, String> toLdapSettings() {
                Map<String, String> ldapSettings = new HashMap<>();
                ldapSettings.put(Context.SECURITY_AUTHENTICATION, "EXTERNAL");
                ldapSettings.put(Context.SECURITY_PROTOCOL, "ssl");
                return ldapSettings;
            }

        }

        /**
         * Weak authentication (clear-text password)
         * @see <a href="https://datatracker.ietf.org/doc/html/rfc4513#section-5.1.3">RFC 4513 / 5.1.3</a>
         */
        record Simple(Protocol protocol, String principal, String credentials) implements SecuritySettings {

            public Simple {
                Objects.requireNonNull(protocol, "Protocol must not be null");
                Objects.requireNonNull(principal, "Principal must not be null");
                Objects.requireNonNull(principal, "Credentials must not be null");
            }

            @Override
            public Map<String, String> toLdapSettings() {
                Map<String, String> ldapSettings = new HashMap<>();
                ldapSettings.put(Context.SECURITY_AUTHENTICATION, "simple");
                ldapSettings.put(Context.SECURITY_PROTOCOL, protocol.name().toLowerCase(Locale.ROOT));
                ldapSettings.put(Context.SECURITY_PRINCIPAL, principal);
                ldapSettings.put(Context.SECURITY_CREDENTIALS, credentials);
                return ldapSettings;
            }

        }

    }

    public record ConnectionSettings(
        String providerUrl,
        SecuritySettings securitySettings,
        Integer connectTimeoutMillis,
        Integer readTimeoutMillis) {

        public ConnectionSettings {
            Objects.requireNonNull(providerUrl, "Provider URL must not be null");
            Objects.requireNonNull(securitySettings, "Security authentication settingsmust not be null");
            Objects.requireNonNull(connectTimeoutMillis, "Read timeout must not be null");
            Objects.requireNonNull(readTimeoutMillis, "Read timeout must not be null");
        }

        public Map<String, String> toLdapSettings() {
            Map<String, String> ldapSettings = new HashMap<>();
            ldapSettings.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
            ldapSettings.put(Context.PROVIDER_URL, providerUrl);
            ldapSettings.putAll(securitySettings.toLdapSettings());
            ldapSettings.put("com.sun.jndi.ldap.connect.timeout", readTimeoutMillis.toString());
            ldapSettings.put("com.sun.jndi.ldap.read.timeout", readTimeoutMillis.toString());
            return ldapSettings;
        }

    }

    public record SearchSettings(
        String baseDn,
        String uidFilter,
        String nameAttributeId,
        String emailAttributeId,
        Optional<String> defaultGroup,
        Map<String, String> groupMappings) {

        public SearchSettings {
            Objects.requireNonNull(baseDn, "Base DN must not be null");
            Objects.requireNonNull(uidFilter, "UID filter must not be null");
            Objects.requireNonNull(nameAttributeId, "Name attribute ID must not be null");
            Objects.requireNonNull(emailAttributeId, "Email attribute ID must not be null");
            Objects.requireNonNull(defaultGroup, "Default group optional must not be null");
            Objects.requireNonNull(groupMappings, "Group mappings must not be null");
            groupMappings.forEach((key, value) ->
                Objects.requireNonNull(value, () -> "Group mapping " + key + " must not be null"));
        }

    }

    public static class Factory implements Supplier<MCRLDAPClient> {

        @MCRProperty(name = "ProviderURL")
        public String providerUrl;

        @MCRProperty(name = "SecurityAuthentication")
        public String securityAuthentication;

        @MCRProperty(name = "SecurityProtocol", required = false)
        public String securityProtocol;

        @MCRProperty(name = "SecurityPrincipal", required = false)
        public String securityPrincipal;

        @MCRProperty(name = "SecurityCredentials", required = false)
        public String securityCredentials;

        @MCRProperty(name = "ConnectTimeout")
        public String connectTimeoutMillis;

        @MCRProperty(name = "ReadTimeout")
        public String readTimeoutMillis;

        @MCRProperty(name = "BaseDN")
        public String baseDn;

        @MCRProperty(name = "UIDFilter")
        public String uidFilter;

        @MCRInstance(name = "Mapping", valueClass = Mapping.class)
        public Mapping mapping;

        @Override
        public MCRLDAPClient get() {

            ConnectionSettings connectionSettings = new ConnectionSettings(
                providerUrl,
                switch (getAuthentication()) {
                    case NONE -> new SecuritySettings.None(getProtocol());
                    case EXTERNAL -> new SecuritySettings.External();
                    case SIMPLE -> new SecuritySettings.Simple(getProtocol(), securityPrincipal, securityCredentials);
                },
                Integer.parseInt(connectTimeoutMillis),
                Integer.parseInt(readTimeoutMillis)
            );

            SearchSettings searchSettings = new SearchSettings(
                baseDn,
                uidFilter,
                mapping.nameAttributeId,
                mapping.emailAttributeId,
                Optional.ofNullable(mapping.group.defaultGroup),
                mapping.group.groupMappings
            );

            return new MCRLDAPClient(connectionSettings, searchSettings);

        }

        private SecuritySettings.Authentication getAuthentication() {
            return SecuritySettings.Authentication.valueOf(securityAuthentication.toUpperCase(Locale.ROOT));
        }

        private SecuritySettings.Protocol getProtocol() {
            return SecuritySettings.Protocol.valueOf(securityProtocol);
        }

        public static final class Mapping {

            @MCRProperty(name = "Name")
            public String nameAttributeId;

            @MCRProperty(name = "E-Mail")
            public String emailAttributeId;

            @MCRInstance(name = "Group", valueClass = Group.class)
            public Group group;

            public static final class Group {

                @MCRProperty(name = "DefaultGroup", required = false)
                public String defaultGroup;

                @MCRProperty(name = "*")
                public Map<String, String> groupMappings;

                @MCRPostConstruction
                public void removeDefaultGroupFromGroupMappings() {
                    groupMappings.remove("DefaultGroup");
                }

            }

        }

    }

    private static final class LazyInstanceHolder {
        public static final MCRLDAPClient SHARED_INSTANCE = createInstance();
    }

}
