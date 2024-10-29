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

import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRProperty;
import org.mycore.common.hint.MCRHints;
import org.mycore.resource.MCRResourcePath;
import org.mycore.resource.hint.MCRResourceHintKeys;

/**
 * {@link MCRClassLoaderResourceProvider} is an implementation of {@link MCRResourceProvider} hat uses
 * {@link ClassLoader#getResource(String)} to look up a resource.
 * <p>
 * It uses the {@link ClassLoader} hinted at by {@link MCRResourceHintKeys#CLASS_LOADER}, if present.
 * <p>
 * The following configuration options are available, if configured automatically:
 * <ul>
 * <li> The property suffix {@link MCRClassLoaderResourceProvider#COVERAGE_KEY} can be used to provide short
 * description for human beings in order to better understand the providers use case.
 * </ul>
 * Example:
 * <pre>
 * [...].Class=org.mycore.resource.provider.MCRClassLoaderResourceProvider
 * [...].Coverage=Lorem ipsum dolor sit amet
 * </pre>
 */
@MCRConfigurationProxy(proxyClass = MCRClassLoaderResourceProvider.Factory.class)
public class MCRClassLoaderResourceProvider extends MCRResourceProviderBase {

    public static final String COVERAGE_KEY = "Coverage";

    public MCRClassLoaderResourceProvider(String coverage) {
        super(coverage);
    }

    @Override
    protected final Optional<URL> doProvide(MCRResourcePath path, MCRHints hints) {
        return getClassloader(hints).map(classLoader -> classLoader.getResource(path.asRelativePath()));
    }

    @Override
    protected final List<ProvidedUrl> doProvideAll(MCRResourcePath path, MCRHints hints) {
        return doProvide(path, hints).stream().map(this::providedURL).toList();
    }

    private Optional<ClassLoader> getClassloader(MCRHints hints) {
        return hints.get(MCRResourceHintKeys.CLASS_LOADER);
    }

    @Override
    public Stream<PrefixStripper> prefixStrippers(MCRHints hints) {
        return Stream.concat(
            Stream.of(JarUrlPrefixStripper.INSTANCE),
            hints.get(MCRResourceHintKeys.CLASS_LOADER).map(ClassLoaderPrefixStripper::new).stream()
        );
    }

    public static class Factory implements Supplier<MCRClassLoaderResourceProvider> {

        @MCRProperty(name = COVERAGE_KEY, defaultName = "MCR.Resource.Provider.Default.ClassLoader.Coverage")
        public String coverage;

        @Override
        public MCRClassLoaderResourceProvider get() {
            return new MCRClassLoaderResourceProvider(coverage);
        }

    }

}
