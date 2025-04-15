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

/// A [MCRUserInformationResolver] can be used to obtain [MCRUserInformation],
/// without knowledge of the underlying mechanism that creates or looks up that user information,
/// by providing a string specification. To do so, it uses [MCRUserInformationProvider] instances
/// that each implement a strategy to create or look up user information.
/// 
/// A specification can be created from a schema and a user ID with
/// [MCRUserInformationResolver#getSpecification(String,String)].
/// 
/// A singular, globally available and automatically configured instance can be obtained with
/// [MCRUserInformationResolver#obtainInstance()]. This instance should generally be used,
/// although custom instances can be created when necessary.
/// It is configured using the property prefix [MCRUserInformationResolver#RESOLVER_PROPERTY]. 
/// 
/// ```properties
/// MCR.UserInformation.Resolver.Class=org.mycore.common.MCRUserInformationResolver
/// ```
/// 
/// The following configuration options are available, if configured automatically:
/// - The configuration suffix [MCRUserInformationResolver#PROVIDERS_KEY] can be used to specify
///   the map of providers to be used.
/// - For each provider, the configuration suffix [MCRSentinel#ENABLED_KEY] can be used to
///   excluded that provider from the configuration.
/// 
/// Example:
/// ```properties
/// [...].Class=org.mycore.common.MCRUserInformationResolver
/// [...].Providers.foo.Class=foo.bar.FooProvider
/// [...].Providers.foo.Enabled=true
/// [...].Providers.foo.Key1=Value1
/// [...].Providers.foo.Key2=Value2
/// [...].Providers.bar.Class=foo.bar.BarProvider
/// [...].Providers.bar.Enabled=false
/// [...].Providers.bar.Key1=Value1
/// [...].Providers.bar.Key2=Value2
/// ```
@MCRConfigurationProxy(proxyClass = MCRUserInformationResolver.Factory.class)
public final class MCRUserInformationResolver {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final MCRUserInformationResolver SHARED_INSTANCE = createInstance();

    public static final String RESOLVER_PROPERTY = "MCR.UserInformation.Resolver";

    public static final String PROVIDERS_KEY = "Providers";

    private final Map<String, MCRUserInformationProvider> providers;

    public MCRUserInformationResolver(Map<String, MCRUserInformationProvider> providers) {

        this.providers = Objects.requireNonNull(providers, "Providers must not be null");
        this.providers
            .forEach((name, provider) -> Objects.requireNonNull(provider, "Provider " + name + " must not be null"));

        LOGGER.info(() -> "Working with providers: " + String.join(", ", providers.keySet()));

    }

    /**
     * @deprecated Use {@link #obtainInstance()} instead
     */
    @Deprecated
    public static MCRUserInformationResolver instance() {
        return obtainInstance();
    }

    public static MCRUserInformationResolver obtainInstance() {
        return SHARED_INSTANCE;
    }

    /**
     * @deprecated Use {@link #createInstance()} instead
     */
    @Deprecated
    public static MCRUserInformationResolver instantiate() {
        return createInstance();
    }

    public static MCRUserInformationResolver createInstance() {
        String classProperty = RESOLVER_PROPERTY + ".Class";
        return MCRConfiguration2.getInstanceOfOrThrow(MCRUserInformationResolver.class, classProperty);
    }

    public MCRUserInformation getOrThrow(String schema, String userId) {
        return getOrThrow(getSpecification(schema, userId));
    }

    public MCRUserInformation getOrThrow(String specification) {
        return get(specification)
            .orElseThrow(() -> new MCRException("Unable to resolve user information for: " + specification));
    }

    public Optional<MCRUserInformation> get(String schema, String userId) {
        return get(getSpecification(schema, userId));
    }

    public Optional<MCRUserInformation> get(String specification) {

        int delimiterPosition = specification.indexOf(':');
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
