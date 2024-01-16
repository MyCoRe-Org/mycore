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

package org.mycore.common;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.status.StatusLogger;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.common.log.MCRListMessage;

/**
 * A {@link MCRUserInformationResolver} can be used to obtain {@link MCRUserInformation}, without knowledge
 * of the underlying mechanism that creates or looks up that user information, by providing a string specification.
 * The specification can be created from a schema and a user ID with
 * {@link MCRUserInformationResolver#getSpecification(String, String)}.
 * <p>
 * Multiple instances of {@link MCRUserInformationProvider} can be configured as a comma separated list of fully
 * qualified class names with the configuration property {@link MCRUserInformationResolver#PROVIDERS_KEY}.
 * Each user information provider has a fixed schema and implements a strategy to create or look up user information
 * for a given user ID.
 * <p>
 * A possible use case is to have the ability of storing the user information required for e.g. 
 * {@link org.mycore.util.concurrent.MCRFixedUserCallable} as a string in a configuration property.
 */
public final class MCRUserInformationResolver {

    public static final String PROVIDERS_KEY = "MCR.UserInformation.Resolver.Providers";

    private static final Logger LOGGER = StatusLogger.getLogger();

    private static final MCRUserInformationResolver INSTANCE = new MCRUserInformationResolver(getProviders());

    private final Map<String, MCRUserInformationProvider> providers;

    public MCRUserInformationResolver(Map<String, MCRUserInformationProvider> providers) {
        this.providers = Objects.requireNonNull(providers);
    }

    private static Map<String, MCRUserInformationProvider> getProviders() {

        List<MCRUserInformationProvider> providers = MCRConfiguration2
            .getOrThrow(PROVIDERS_KEY, MCRConfiguration2::splitValue)
            .map(MCRConfiguration2::<MCRUserInformationProvider>instantiateClass)
            .toList();

        MCRListMessage description = new MCRListMessage();
        Map<String, MCRUserInformationProvider> providersBySchema = new HashMap<>();

        providers.forEach(provider -> {
            checkIsUniqueSchema(providersBySchema, provider);
            description.add(provider.getSchema(), provider.getClass().getName());
            providersBySchema.put(provider.getSchema(), provider);
        });

        LOGGER.info(description.logMessage("Resolving user information with providers:"));

        return Collections.unmodifiableMap(providersBySchema);

    }

    private static void checkIsUniqueSchema(Map<String, MCRUserInformationProvider> providersBySchema,
        MCRUserInformationProvider provider) {
        String schema = provider.getSchema();
        if (providersBySchema.containsKey(schema)) {
            throw new MCRConfigurationException("Multiple user information providers with schema '"
                + schema + "' configured: " + providersBySchema.get(schema).getClass().getName()
                + ", " + provider.getClass().getName());
        }
    }

    public static MCRUserInformationResolver instance() {
        return INSTANCE;
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

}
