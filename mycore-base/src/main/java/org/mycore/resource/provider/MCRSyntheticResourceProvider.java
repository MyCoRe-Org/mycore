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

package org.mycore.resource.provider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import org.apache.logging.log4j.Level;
import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRInstanceList;
import org.mycore.common.config.annotation.MCRProperty;
import org.mycore.common.log.MCRTreeMessage;
import org.mycore.resource.common.MCRSyntheticResourceSpec;
import org.mycore.resource.locator.MCRSyntheticResourceLocator;

/**
 * A {@link MCRSyntheticResourceProvider} is a {@link MCRResourceProvider} that looks up resources
 * in a given list of {@link MCRSyntheticResourceSpec} instances. If multiple specs for the same resource path exist,
 * only the first such spec is used.
 * <p>
 * The following configuration options are available:
 * <ul>
 * <li> The property suffix {@link MCRResourceProviderBase#COVERAGE_KEY} can be used to
 * provide a short description of the providers purpose; used in log messages.
 * <li> The property suffix {@link MCRSyntheticResourceProvider#SPECS_KEY} can be used to
 * specify the list of specs to be used.
 * <li> For each spec, the property suffix {@link MCRSyntheticResourceSpec#PREFIX_KEY} can be used to
 * specify the prefix to be used.
 * <li> For each spec, the property suffix {@link MCRSyntheticResourceSpec#PATH_KEY} can be used to
 * specify the path to be used.
 * </ul>
 * Example:
 * <pre><code>
 * [...].Class=org.mycore.resource.provider.MCRSyntheticResourceProvider
 * [...].Coverage=Lorem ipsum dolor sit amet
 * [...].Specs.10.Prefix=test:
 * [...].Specs.10.Path=/foo
 * [...].Specs.20.Prefix=test:
 * [...].Specs.20.Path=/bar
 * </code></pre>
 */
@MCRConfigurationProxy(proxyClass = MCRSyntheticResourceProvider.Factory.class)
public final class MCRSyntheticResourceProvider extends MCRLocatorResourceProvider {

    public static final String SPECS_KEY = "Specs";

    private final List<MCRSyntheticResourceSpec> specs;

    public MCRSyntheticResourceProvider(String coverage, MCRSyntheticResourceSpec... specs) {
        this(coverage, Arrays.asList(specs));
    }

    public MCRSyntheticResourceProvider(String coverage, List<MCRSyntheticResourceSpec> specs) {
        super(coverage, new MCRSyntheticResourceLocator(specs));
        this.specs = new ArrayList<>(Objects.requireNonNull(specs, "Specs must not be null"));
    }

    @Override
    public MCRTreeMessage compileDescription(Level level) {
        MCRTreeMessage entries = new MCRTreeMessage();
        this.specs.forEach(spec -> entries.add(spec.path().asAbsolutePath(),
            spec.prefix() + spec.path().asAbsolutePath()));
        MCRTreeMessage description = super.compileDescription(level);
        description.add("Specs", entries);
        return description;
    }

    public static class Factory implements Supplier<MCRSyntheticResourceProvider> {

        @MCRProperty(name = COVERAGE_KEY, defaultName = "MCR.Resource.Provider.Default.Synthetic.Coverage")
        public String coverage;

        @MCRInstanceList(name = SPECS_KEY, valueClass = MCRSyntheticResourceSpec.class)
        public List<MCRSyntheticResourceSpec> specs;

        @Override
        public MCRSyntheticResourceProvider get() {
            return new MCRSyntheticResourceProvider(coverage, specs);
        }

    }

}
