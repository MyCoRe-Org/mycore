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

package org.mycore.resource;

import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRInstanceMap;
import org.mycore.common.config.annotation.MCRProperty;
import org.mycore.common.config.annotation.MCRSentinel;
import org.mycore.common.hint.MCRHint;
import org.mycore.common.hint.MCRHints;
import org.mycore.common.hint.MCRHintsBuilder;
import org.mycore.common.log.MCRListMessage;
import org.mycore.common.log.MCRTreeMessage;
import org.mycore.resource.provider.MCRResourceProvider;
import org.mycore.resource.provider.MCRResourceProvider.PrefixStripper;
import org.mycore.resource.provider.MCRResourceProvider.ProvidedUrl;

/**
 * A {@link MCRResourceResolver} is a component that uses a {@link MCRResourceProvider} instance to lookup resources.
 * To inject variable values into the resolving mechanism, several {@link MCRHint} instances can be provided as a
 * {@link MCRHints} instance. A default set of hints can be provided.
 * <p>
 * A singular, globally available and centrally configured instance can be obtained with
 * {@link MCRResourceResolver#obtainInstance()}. This instance is configured using the property prefix
 * {@link MCRResourceResolver#RESOLVER_PROPERTY} and should be used wherever resources need to be resolved.
 * This ensures an application-wide consistent behaviour, although custom instances can be created when necessary.
 * <p>
 * The following configuration options are available, if configured automatically:
 * <ul>
 * <li> Hints are configured as a map using the property suffix {@link MCRResourceResolver#HINTS_KEY}.
 * <li> Providers are configured as a map using the property suffix {@link MCRResourceResolver#PROVIDERS_KEY}.
 * <li> Each provider can be excluded from the configuration using the property {@link MCRSentinel#ENABLED_KEY}.
 * <li> The selected provider is configured using the property suffix {@link MCRResourceResolver#SELECTED_PROVIDER_KEY}.
 * </ul>
 * Example:
 * <pre>
 * MCR.Resource.Resolver.Class=org.mycore.resource.MCRResourceResolver
 * MCR.Resource.Resolver.Hints.foo.Class=foo.bar.FooHint
 * MCR.Resource.Resolver.Hints.foo.Key1=Value1
 * MCR.Resource.Resolver.Hints.foo.Key2=Value2
 * MCR.Resource.Resolver.Hints.bar.Class=foo.bar.BarHint
 * MCR.Resource.Resolver.Hints.bar.Key1=Value1
 * MCR.Resource.Resolver.Hints.bar.Key2=Value2
 * MCR.Resource.Resolver.Providers.foo.Class=foo.bar.FooProvider
 * MCR.Resource.Resolver.Providers.foo.Enabled=true
 * MCR.Resource.Resolver.Providers.foo.Key1=Value1
 * MCR.Resource.Resolver.Providers.foo.Key2=Value2
 * MCR.Resource.Resolver.Providers.bar.Class=foo.bar.BarProvider
 * MCR.Resource.Resolver.Providers.bar.Enabled=false
 * MCR.Resource.Resolver.Providers.bar.Key1=Value1
 * MCR.Resource.Resolver.Providers.bar.Key2=Value2
 * MCR.Resource.Resolver.SelectedProvider=foo
 * </pre>
 * Although only one provider is ever in use, multiple providers can be prepared. This configuration mechanism greatly
 * simplifies the configuration changes necessary in order to switch to another provider. It also ensures that all
 * prepared providers are properly configured.
 */
@MCRConfigurationProxy(proxyClass = MCRResourceResolver.Factory.class)
public final class MCRResourceResolver {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final MCRResourceResolver SHARED_INSTANCE = createInstance();

    public static final String RESOLVER_PROPERTY = "MCR.Resource.Resolver";

    public static final String HINTS_KEY = "Hints";

    public static final String PROVIDERS_KEY = "Providers";

    public static final String SELECTED_PROVIDER_KEY = "SelectedProvider";

    private final MCRHints hints;

    private final MCRResourceProvider provider;

    public MCRResourceResolver(MCRHints hints, MCRResourceProvider provider) {
        this.hints = Objects.requireNonNull(hints, "Hints must not be null");
        this.provider = Objects.requireNonNull(provider, "Provider must not be null");
    }

    public static MCRResourceResolver obtainInstance() {
        return SHARED_INSTANCE;
    }

    public static MCRResourceResolver createInstance() {
        String classProperty = RESOLVER_PROPERTY + ".Class";
        return MCRConfiguration2.getInstanceOfOrThrow(MCRResourceResolver.class, classProperty);
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
     * Resolves a {@link MCRResourcePath} using the given hints, returning all alternatives (i.e. because one module
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
                () -> LOGGER.debug("Unable to resolve resource URL for path {}", path));
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
                    url -> LOGGER.debug("Resolved resource URL for path {} as {}", path, url.url));
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
     * Tries to revers {@link MCRResourceResolver#resolve(MCRResourcePath)}, using the given hints, i.e. tries to
     * calculate a {@link MCRResourcePath}, that resolves to the given resource URL.
     * <p>
     * This resource path is calculated by removing different prefixes from the given resource URL and checking if
     * resolving the remaining part of the resource URL as a resource path results in the given resource URL.
     * <p>
     * For this to happen, the used {@link MCRResourceResolver} provides a stream of {@link PrefixStripper}, each of
     * which can remove multiple prefixes from the given resource URL. The prefix strippers and the remaining parts
     * returned by each prefix stripper are checked in order. returns the first such remaining part, that resolves to
     * the given resource URL, if any.
     */
    public Optional<MCRResourcePath> reverse(URL resourceUrl, MCRHints hints) {
        LOGGER.debug("Reversing resource URL {}", resourceUrl);
        return provider.prefixStrippers(hints).sequential().flatMap(
            prefixStripper -> {
                List<MCRResourcePath> potentialPaths = prefixStripper.strip(resourceUrl).get();
                for (MCRResourcePath potentialPath : potentialPaths) {
                    if (isConsistent(resourceUrl, potentialPath, hints)) {
                        return Stream.of(potentialPath);
                    }
                }
                return Stream.empty();
            }
        ).findFirst();
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
    public MCRHints defaultHints() {
        return hints;
    }

    @SuppressWarnings("PMD.SystemPrintln")
    public static class Factory implements Supplier<MCRResourceResolver> {

        @MCRInstanceMap(name = HINTS_KEY, valueClass = MCRHint.class)
        public Map<String, MCRHint<?>> hints;

        @MCRInstanceMap(name = PROVIDERS_KEY, valueClass = MCRResourceProvider.class, sentinel = @MCRSentinel)
        public Map<String, MCRResourceProvider> providers;

        @MCRProperty(name = SELECTED_PROVIDER_KEY)
        public String selectedProvider;

        @Override
        public MCRResourceResolver get() {
            System.out.println("Found providers: " + String.join(", ", providers.keySet()));
            System.out.println("Resolving resources with provider: " + selectedProvider);
            return new MCRResourceResolver(getHints(), getProvider(selectedProvider));
        }

        private MCRHints getHints() {
            MCRListMessage description = new MCRListMessage();
            MCRHintsBuilder builder = new MCRHintsBuilder();
            for (Map.Entry<String, MCRHint<?>> entry : hints.entrySet()) {
                MCRHint<?> hint = entry.getValue();
                description.add(entry.getKey(), hint.getClass().getName());
                builder.add(hint);
            }
            System.out.println(description.logMessage("Default hints:"));
            return builder.build();
        }

        private MCRResourceProvider getProvider(String selectedProviderName) {
            MCRResourceProvider selectedProvider = providers.get(selectedProviderName);
            if (selectedProvider == null) {
                throw new MCRConfigurationException("Selected provider " + selectedProviderName + " unavailable, got: "
                    + String.join(", ", providers.keySet()));
            }
            MCRTreeMessage description = selectedProvider.compileDescription(LOGGER.getLevel());
            String introduction = "Configuration of selected provider " + selectedProviderName + ":";
            System.out.println(description.logMessage(introduction));
            return selectedProvider;
        }

    }

}
