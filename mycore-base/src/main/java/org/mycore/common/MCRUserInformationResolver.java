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

package org.mycore.common;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRInstanceMap;
import org.mycore.common.config.annotation.MCRSentinel;

/**
 * A {@link MCRUserInformationResolver} can be used to obtain {@link MCRUserInformation}, without knowledge
 * of the underlying mechanism that creates or looks up that user information, by providing a string specification.
 * The specification can be created from a schema and a user ID with
 * {@link MCRUserInformationResolver#getSpecification(String, String)}. To do so, it uses
 * {@link MCRUserInformationProvider} instances that each implement a strategy to create or look up user information.
 * <p>
 * A singular, globally available and centrally configured instance can be obtained with
 * {@link MCRUserInformationResolver#instance()}. This instance is configured using the property prefix
 * {@link MCRUserInformationResolver#RESOLVER_PROPERTY} and should be used in order obtain user information with
 * consistently applied strategies, although custom instances can be created when necessary.
 * <p>
 * The following configuration options are available, if configured automatically:
 * <ul>
 * <li> Providers are configured as a map using the property suffix {@link MCRUserInformationResolver#PROVIDERS_KEY}.
 * <li> Each resolver can be excluded from the configuration using the property {@link MCRSentinel#ENABLED_KEY}.
 * </ul>
 * Example:
 * <pre>
 * MCR.UserInformation.Resolver.Class=org.mycore.common.MCRUserInformationResolver
 * MCR.UserInformation.Resolver.Providers.foo.Class=foo.bar.FooProvider
 * MCR.UserInformation.Resolver.Providers.foo.Enabled=true
 * MCR.UserInformation.Resolver.Providers.foo.Key1=Value1
 * MCR.UserInformation.Resolver.Providers.foo.Key2=Value2
 * MCR.UserInformation.Resolver.Providers.bar.Class=foo.bar.BarProvider
 * MCR.UserInformation.Resolver.Providers.bar.Enabled=false
 * MCR.UserInformation.Resolver.Providers.bar.Key1=Value1
 * MCR.UserInformation.Resolver.Providers.bar.Key2=Value2
 * </pre>
 */
@MCRConfigurationProxy(proxyClass = MCRUserInformationResolver.Factory.class)
public final class MCRUserInformationResolver {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final MCRUserInformationResolver INSTANCE = instantiate();

    public static final String RESOLVER_PROPERTY = "MCR.UserInformation.Resolver";

    public static final String PROVIDERS_KEY = "Providers";

    private final Map<String, MCRUserInformationProvider> providers;

    public MCRUserInformationResolver(Map<String, MCRUserInformationProvider> providers) {

        this.providers = Objects.requireNonNull(providers, "Providers must not be null");
        this.providers.values().forEach(provider -> Objects.requireNonNull(provider, "Provider must not be null"));

        LOGGER.info("Working with providers: " + String.join(", ", providers.keySet()));

    }

    public static MCRUserInformationResolver instance() {
        return INSTANCE;
    }

    public static MCRUserInformationResolver instantiate() {
        String classProperty = RESOLVER_PROPERTY + ".Class";
        return MCRConfiguration2.getInstanceOfOrThrow(MCRUserInformationResolver.class, classProperty);
    }

    public MCRUserInformation getOrThrow(String schema, String userId) {
        return getOrThrow(getSpecification(schema, userId));
    }

    public MCRUserInformation getOrThrow(String specification) {
        return get(specification).orElseThrow(() ->
            new MCRException("Unable to resolve user information for: " + specification));
    }

    public Optional<MCRUserInformation> get(String schema, String userId) {
        return get(getSpecification(schema, userId));
    }

    public Optional<MCRUserInformation> get(String specification) {

        int delimiterPosition = specification.indexOf(":");
        if (delimiterPosition == -1) {
            throw new MCRUsageException("No schema delimiter found in " + specification);
        }

        String schema = specification.substring(0, delimiterPosition);
        if (schema.isEmpty()) {
            throw new MCRUsageException("Empty schema found in " + specification);
        }

        String userId = specification.substring(delimiterPosition + 1);
        if (userId.isEmpty()) {
            throw new MCRUsageException("Empty user ID found in " + specification);
        }

        MCRUserInformationProvider provider = providers.get(schema);
        if (provider == null) {
            return Optional.empty();
        } else {
            return provider.get(userId);
        }

    }

    public static String getSpecification(String schema, String userId) {
        return schema + ":" + userId;
    }

    public static class Factory implements Supplier<MCRUserInformationResolver> {

        @MCRInstanceMap(name = PROVIDERS_KEY, valueClass = MCRUserInformationProvider.class, sentinel = @MCRSentinel)
        public Map<String, MCRUserInformationProvider> providers;

        @Override
        public MCRUserInformationResolver get() {
            return new MCRUserInformationResolver(providers);
        }

    }

}
