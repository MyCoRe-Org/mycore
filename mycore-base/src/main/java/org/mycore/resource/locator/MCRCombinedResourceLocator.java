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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRInstanceList;
import org.mycore.common.hint.MCRHints;
import org.mycore.resource.MCRResourcePath;
import org.mycore.resource.common.MCRResourceTracer;
import org.mycore.resource.provider.MCRResourceProvider.PrefixStripper;

/**
 * A {@link MCRCombinedResourceLocator} is a {@link MCRResourceLocator} that delegates to multiple
 * other {@link MCRResourceLocator} instances, one after another.
 * <p>
 * The following configuration options are available:
 * <ul>
 * <li> The property suffix {@link MCRCombinedResourceLocator#LOCATORS_KEY} can be used to
 * specify the list of locators to be used.
 * </ul>
 * Example:
 * <pre><code>
 * [...].Class=org.mycore.resource.locator.MCRCombinedResourceLocator
 * [...].Locators.10.Class=foo.bar.FooLocator
 * [...].Locators.10.Key1=Value1
 * [...].Locators.10.Key2=Value2
 * [...].Locators.20.Class=foo.bar.BarLocator
 * [...].Locators.20.Key1=Value1
 * [...].Locators.20.Key2=Value2
 * </code></pre>
 */
@MCRConfigurationProxy(proxyClass = MCRCombinedResourceLocator.Factory.class)
public class MCRCombinedResourceLocator extends MCRResourceLocatorBase {

    public static final String LOCATORS_KEY = "Locators";

    private final List<MCRResourceLocator> locators;

    public MCRCombinedResourceLocator(MCRResourceLocator... locators) {
        this(Arrays.asList(locators));
    }

    public MCRCombinedResourceLocator(List<MCRResourceLocator> locators) {
        this.locators = new ArrayList<>(Objects.requireNonNull(locators, "Locators must not be null"));
        this.locators.forEach(locator -> Objects.requireNonNull(locator, "Locator must not be null"));
        Collections.reverse(this.locators);
    }

    @Override
    protected Stream<URL> doLocate(MCRResourcePath path, MCRHints hints, MCRResourceTracer tracer) {
        Stream<URL> resourceUrls = Stream.empty();
        for (MCRResourceLocator locator : locators) {
            resourceUrls = Stream.concat(locator.locate(path, hints, tracer.update(locator)), resourceUrls);
        }
        return resourceUrls;
    }

    @Override
    public Stream<PrefixStripper> prefixStrippers(MCRHints hints) {
        return locators.stream().flatMap(locator -> locator.prefixStrippers(hints));
    }

    public static class Factory implements Supplier<MCRCombinedResourceLocator> {

        @MCRInstanceList(name = LOCATORS_KEY, valueClass = MCRResourceLocator.class, required = false)
        public List<MCRResourceLocator> locators;

        @Override
        public MCRCombinedResourceLocator get() {
            return new MCRCombinedResourceLocator(locators);
        }

    }

}
