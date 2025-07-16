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

package org.mycore.resource.locator;

import java.net.URL;
import java.net.URLStreamHandlerFactory;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRInstanceList;
import org.mycore.common.hint.MCRHints;
import org.mycore.resource.MCRResourcePath;
import org.mycore.resource.common.MCRResourceTracer;
import org.mycore.resource.common.MCRSyntheticResourceSpec;
import org.mycore.resource.provider.MCRResourceProvider.PrefixPrefixStripper;
import org.mycore.resource.provider.MCRResourceProvider.PrefixStripper;

/**
 * A {@link MCRSyntheticResourceLocator} is a {@link MCRResourceLocator} that looks up resources
 * in a given list of {@link MCRSyntheticResourceSpec} instances. If multiple specs for the same resource path exist,
 * only the first such spec is used.
 * <p>
 * The following configuration options are available:
 * <ul>
 * <li> The property suffix {@link MCRSyntheticResourceLocator#SPECS_KEY} can be used to
 * specify the list of specs to be used.
 * <li> For each spec, the property suffix {@link MCRSyntheticResourceSpec#PREFIX_KEY} can be used to
 * specify the prefix to be used.
 * <li> For each spec, the property suffix {@link MCRSyntheticResourceSpec#PATH_KEY} can be used to
 * specify the path to be used.
 * </ul>
 * Example:
 * <pre><code>
 * [...].Class=org.mycore.resource.locator.MCRSyntheticResourceLocator
 * [...].Specs.10.Prefix=test:
 * [...].Specs.10.Path=/foo
 * [...].Specs.20.Prefix=test:
 * [...].Specs.20.Path=/bar
 * </code></pre>
 */
@MCRConfigurationProxy(proxyClass = MCRSyntheticResourceLocator.Factory.class)
public class MCRSyntheticResourceLocator extends MCRResourceLocatorBase {

    public static final String SPECS_KEY = "Specs";

    private final Map<MCRResourcePath, List<MCRSyntheticResourceSpec>> specs;

    private final List<PrefixStripper> strippers;

    public MCRSyntheticResourceLocator(MCRSyntheticResourceSpec... specs) {
        this(Arrays.asList(specs));
    }

    public MCRSyntheticResourceLocator(List<MCRSyntheticResourceSpec> specs) {
        Set<String> prefixes = new HashSet<>();
        this.specs = Objects.requireNonNull(specs, "Specs must not be null").stream()
            .peek(entry -> Objects.requireNonNull(entry, "Spec must not be null"))
            .peek(entry -> prefixes.add(entry.prefix()))
            .collect(Collectors.groupingBy(MCRSyntheticResourceSpec::path));
        this.strippers = prefixes.stream()
            .map(PrefixPrefixStripper::new)
            .map(PrefixStripper.class::cast)
            .toList();
    }

    @Override
    protected Stream<URL> doLocate(MCRResourcePath path, MCRHints hints, MCRResourceTracer tracer) {
        URLStreamHandlerFactory factory = hints.get(MCRSyntheticResourceSpec.URL_STREAM_HANDLER_FACTORY).orElse(null);
        return specs.getOrDefault(path, List.of()).stream()
            .map(entry -> entry.toUrl(factory));
    }

    @Override
    public Stream<PrefixStripper> prefixStrippers(MCRHints hints) {
        return strippers.stream();
    }

    public static class Factory implements Supplier<MCRSyntheticResourceLocator> {

        @MCRInstanceList(name = SPECS_KEY, valueClass = MCRSyntheticResourceSpec.class)
        public List<MCRSyntheticResourceSpec> specs;

        @Override
        public MCRSyntheticResourceLocator get() {
            return new MCRSyntheticResourceLocator(specs);
        }

    }

}
