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

package org.mycore.resource;

import java.net.URL;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.status.StatusLogger;
import org.mycore.common.MCRException;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.hint.MCRHint;
import org.mycore.common.hint.MCRHints;
import org.mycore.common.hint.MCRHintsBuilder;
import org.mycore.common.log.MCRListMessage;
import org.mycore.common.log.MCRTreeMessage;
import org.mycore.resource.provider.MCRCombinedResourceProvider;
import org.mycore.resource.provider.MCRResourceProvider;
import org.mycore.resource.provider.MCRResourceProvider.PrefixStripper;
import org.mycore.resource.provider.MCRResourceProvider.ProvidedUrl;

/**
 * A {@link MCRResourceResolver} is a component that uses a {@link MCRResourceProvider} to lookup resources.
 * <p>
 * A singular, globally available and centrally configured instance is made available by
 * {{@link MCRResourceResolver#instance()}}. This instance should be used wherever resources need to be resolved.
 * This ensures an application-wide consistent behaviour.
 * <p>
 * The globally available instance is configured through the configuration properties provided by
 * {@link MCRConfiguration2}. Multiple configurations may be configured. The name of the configuration
 * that is actually used is retrieved from the configuration property <code>MCR.Resource.Resolver</code>.
 * <p>
 * Specific configuration values begin with <code>MCR.Resource.Resolver.${configurationName}</code>.
 * <p>
 * There are the following configuration values:
 * <ul>
 *     <li><code>Provider.Class</code> - The configured instance of {@link MCRResourceProvider} to be used, configured
 *     by {@link MCRConfiguration2#getInstanceOf(String)}, typically a {@link MCRCombinedResourceProvider}.</li>
 * </ul>
 * <p>
 * Example:
 * <pre>
 * MCR.Resource.Resolver=foo
 * MCR.Resource.Resolver.foo.Provider.Class=org.mycore.resource.provider.MCRCombinedResourceProvider
 * MCR.Resource.Resolver.foo.Provider.Providers.1.Class=org.mycore.resource.provider.MCRConfigDirResourceProvider
 * MCR.Resource.Resolver.foo.Provider.Providers.2.Class=org.mycore.resource.provider.MCRClassLoaderResourceProvider
 * </pre>
 * <p>
 * Additionally, there are the following global configuration values for the provider:
 * <ul>
 *     <li><code>MCR.Resource.Resolver.Hints.${hintName}</code> - Configured instances of {@link MCRHint} to be used
 *     for {@link MCRResourceResolver#defaultHints()} and as default hints for methods that optionally take hints.</li>
 * </ul>
 * <p>
 * Example:
 * <pre>
 * MCR.Resource.Resolver.Hints.classLoader.Class=org.mycore.resource.hint.MCRClassLoaderResourceHint
 * MCR.Resource.Resolver.Hints.servletContext.Class=org.mycore.resource.hint.MCRServletContextResourceHint
 * </pre>
 */
public final class MCRResourceResolver {

    public static final String RESOLVER_KEY = "MCR.Resource.Resolver";

    public static final String HINTS_KEY = "MCR.Resource.Resolver.Hints";

    private static final Logger LOGGER = StatusLogger.getLogger();

    private static final MCRResourceResolver INSTANCE = new MCRResourceResolver(getProvider());

    private static final MCRHints DEFAULT_HINTS = getDefaultHints();

    private final MCRResourceProvider provider;

    public MCRResourceResolver(MCRResourceProvider provider) {
        this.provider = Objects.requireNonNull(provider);
    }

    private static MCRResourceProvider getProvider() {

        String name = MCRConfiguration2.getStringOrThrow(RESOLVER_KEY);
        LOGGER.info("Using resolver: {}", name);

        String providerProperty = RESOLVER_KEY + "." + name + ".Provider.Class";
        MCRResourceProvider provider = getInstanceOfOrThrow(providerProperty);

        MCRTreeMessage description = provider.compileDescription(LOGGER.getLevel());
        LOGGER.info(description.logMessage("Resolving resources with provider:"));

        return provider;

    }

    private static MCRHints getDefaultHints() {

        String prefix = HINTS_KEY + ".";
        Map<String, Callable<MCRHint<?>>> hintFactoriesByProperty = MCRConfiguration2.getInstances(prefix);

        MCRListMessage description = new MCRListMessage();
        MCRHintsBuilder builder = new MCRHintsBuilder();
        for (String property : hintFactoriesByProperty.keySet()) {
            try {
                String schema = property.substring(prefix.length());
                MCRHint<?> hint = hintFactoriesByProperty.get(property).call();
                description.add(schema, hint.getClass().getName());
                builder.add(hint);
            } catch (Exception e) {
                throw new MCRException("Failed to instantiate hint configured in: " + property);
            }
        }
        LOGGER.info(description.logMessage("Resolving resources with default hints:"));

        return builder.build();

    }

    private static <T> T getInstanceOfOrThrow(String property) {
        return MCRConfiguration2.<T>getInstanceOf(property)
            .orElseThrow(() -> MCRConfiguration2.createConfigurationException(property));
    }

    public static MCRResourceResolver instance() {
        return INSTANCE;
    }

    /**
     * Resolves a {@link MCRResourcePath}.
     */
    public Optional<URL> resolve(MCRResourcePath path) {
        return resolve(path, defaultHints());
    }

    /**
     * Resolves a {@link MCRResourcePath} using the given hints.
     */
    public Optional<URL> resolve(MCRResourcePath path, MCRHints hints) {
        return resource(path, hints);
    }

    /**
     * Resolves a {@link MCRResourcePath}, returning all alternatives (i.e. because one module
     * overrides a resource that is also provided by another module). Intended for introspective purposes only.
     */
    public List<ProvidedUrl> resolveAll(MCRResourcePath path) {
        return resolveAll(path, defaultHints());
    }

    /**
     * Resolves a {@link MCRResourcePath}using the given hints, returning all alternatives (i.e. because one module
     * overrides a resource that is also provided by another module). Intended for introspective purposes only.
     */
    public List<ProvidedUrl> resolveAll(MCRResourcePath path, MCRHints hints) {
        return allResources(path, hints);
    }

    /**
     * Resolves a {@link MCRResourcePath}.
     */
    public Optional<URL> resolve(Optional<MCRResourcePath> path) {
        return path.flatMap(this::resolve);
    }

    /**
     * Resolves a {@link MCRResourcePath} using the given hints.
     */
    public Optional<URL> resolve(Optional<MCRResourcePath> path, MCRHints hints) {
        return path.flatMap(p -> resolve(p, hints));
    }

    /**
     * Resolves a {@link MCRResourcePath}, returning all alternatives (i.e. because one module
     * overrides a resource that is also provided by another module). Intended for introspective purposes only.
     */
    public List<ProvidedUrl> resolveAll(Optional<MCRResourcePath> path) {
        return path.map(this::resolveAll).orElse(Collections.emptyList());
    }

    /**
     * Resolves a {@link MCRResourcePath} using the given hints, returning all alternatives (i.e. because one module
     * overrides a resource that is also provided by another module). Intended for introspective purposes only.
     */
    public List<ProvidedUrl> resolveAll(Optional<MCRResourcePath> path, MCRHints hints) {
        return path.map(p -> resolveAll(p, hints)).orElse(Collections.emptyList());
    }

    /**
     * Shorthand for {@link MCRResourceResolver#resolve(MCRResourcePath)}, interpreting
     * the given path as a resource path.
     */
    public Optional<URL> resolveResource(String path) {
        return resolveResource(path, defaultHints());
    }

    /**
     * Shorthand for {@link MCRResourceResolver#resolve(MCRResourcePath, MCRHints)}, interpreting
     * the given path as a resource path.
     */
    public Optional<URL> resolveResource(String path, MCRHints hints) {
        return MCRResourcePath.ofPath(path).flatMap(resourcePath -> resource(resourcePath, hints));
    }

    /**
     * Shorthand for {@link MCRResourceResolver#resolveAll(MCRResourcePath)}, interpreting
     * the given path as a resource path.
     */
    public List<ProvidedUrl> resolveAllResource(String path) {
        return resolveAllResource(path, defaultHints());
    }

    /**
     * Shorthand for {@link MCRResourceResolver#resolveAll(MCRResourcePath, MCRHints)}, interpreting
     * the given path as a resource path.
     */
    public List<ProvidedUrl> resolveAllResource(String path, MCRHints hints) {
        return MCRResourcePath.ofPath(path)
            .map(resourcePath -> allResources(resourcePath, hints))
            .orElse(Collections.emptyList());
    }

    /**
     * Shorthand for {@link MCRResourceResolver#resolve(MCRResourcePath)}, interpreting
     * the given path as a web resource path.
     */
    public Optional<URL> resolveWebResource(String path) {
        return resolveWebResource(path, defaultHints());
    }

    /**
     * Shorthand for {@link MCRResourceResolver#resolve(MCRResourcePath, MCRHints)}, interpreting
     * the given path as a web resource path.
     */
    public Optional<URL> resolveWebResource(String path, MCRHints hints) {
        return MCRResourcePath.ofWebPath(path).flatMap(resourcePath -> resource(resourcePath, hints));
    }

    /**
     * Shorthand for {@link MCRResourceResolver#resolveAll(MCRResourcePath)}, interpreting
     * the given path as a web resource path.
     */
    public List<ProvidedUrl> resolveAllWebResource(String path) {
        return resolveAllWebResource(path, defaultHints());
    }

    /**
     * Shorthand for {@link MCRResourceResolver#resolveAll(MCRResourcePath, MCRHints)}, interpreting
     * the given path as a web resource path.
     */
    public List<ProvidedUrl> resolveAllWebResource(String path, MCRHints hints) {
        return MCRResourcePath.ofWebPath(path)
            .map(resourcePath -> allResources(resourcePath, hints))
            .orElse(Collections.emptyList());
    }

    private Optional<URL> resource(MCRResourcePath path, MCRHints hints) {
        LOGGER.debug("Resolving resource {}", path);
        Optional<URL> resourceUrl = provider.provide(path, hints);
        if (LOGGER.isDebugEnabled()) {
            resourceUrl.ifPresentOrElse(
                url -> LOGGER.debug("Resolved resource URL for path {} as {}", path, url),
                () -> LOGGER.debug("Unable to resolve resource URL for path {}", path)
            );
        }
        return resourceUrl;
    }

    private List<ProvidedUrl> allResources(MCRResourcePath path, MCRHints hints) {
        LOGGER.debug("Resolving all resource {}", path);
        List<ProvidedUrl> resourceUrls = provider.provideAll(path, hints);
        if (LOGGER.isDebugEnabled()) {
            if (resourceUrls.isEmpty()) {
                LOGGER.debug("Unable to resolve resource URL for path {}", path);
            } else {
                resourceUrls.forEach(
                    url -> LOGGER.debug("Resolved resource URL for path {} as {}", path, url.url)
                );
            }
        }
        return resourceUrls;
    }

    /**
     * Tries to revers {@link MCRResourceResolver#resolve(MCRResourcePath)}.
     */
    public Optional<MCRResourcePath> reverse(URL resourceUrl) {
        return this.reverse(resourceUrl, defaultHints());
    }

    /**
     * Tries to revers {@link MCRResourceResolver#resolve(MCRResourcePath)}, using the given hints. Optionally
     * performs a consistency check by resolving the calculated {@link MCRResourcePath} and comparing the
     * result of this resolution against the given resource URL.
     */
    public Optional<MCRResourcePath> reverse(URL resourceUrl, MCRHints hints) {
        LOGGER.debug("Reversing resource URL {}", resourceUrl);
        List<Supplier<List<PrefixStripper>>> strippers = provider.prefixStrippers(hints);
        for (PrefixStripper stripper : toIterable(strippers)) {
            Optional<MCRResourcePath> potentialPath = stripper.strip(resourceUrl);
            if (potentialPath.isPresent()) {
                if (!isConsistent(resourceUrl, potentialPath.get(), hints)) {
                    continue;
                }
                return potentialPath;
            }
        }
        LOGGER.debug("Unable to reverse path for resource URL {}", resourceUrl);
        return Optional.empty();
    }

    private Iterable<PrefixStripper> toIterable(List<Supplier<List<PrefixStripper>>> strippers) {

        return () -> new Iterator<>() {

            private final Iterator<Supplier<List<PrefixStripper>>> all = strippers.iterator();

            private Iterator<PrefixStripper> current = Collections.emptyIterator();

            @Override
            public boolean hasNext() {
                while (!current.hasNext() && all.hasNext()) {
                    current = all.next().get().iterator();
                }
                return current.hasNext();
            }

            @Override
            public PrefixStripper next() {
                return current.next();
            }

        };

    }

    private boolean isConsistent(URL resourceUrl, MCRResourcePath potentialPath, MCRHints hints) {
        LOGGER.debug("Trying potential path {}", potentialPath);
        Optional<URL> resolvedResourceUrl = resolve(potentialPath, hints);
        if (resolvedResourceUrl.isEmpty()) {
            LOGGER.debug("Unable to resolve resource URL for potential path {}", potentialPath);
            return false;
        }
        return isConsistent(resourceUrl, potentialPath, resolvedResourceUrl.get());

    }

    private static boolean isConsistent(URL resourceUrl, MCRResourcePath potentialPath, URL resolvedResourceUrl) {
        LOGGER.debug("Resolved resource URL for possible path {} as {}", potentialPath, resolvedResourceUrl);
        if (!resolvedResourceUrl.toString().equals(resourceUrl.toString())) {
            LOGGER.debug("Resolved resource URL doesn't match original resource URL");
            return false;
        }
        LOGGER.debug("Resolved resource URL matches original resource URL");
        return true;
    }

    /**
     * The default {@link MCRHints} used by {@link MCRResourceResolver}, when no custom hints are given.
     * Intended to be a basis for customized hints, if customization or extension is required.
     */
    public static MCRHints defaultHints() {
        return DEFAULT_HINTS;
    }

}
