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

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.mycore.common.MCRException;
import org.mycore.common.MCRStreamUtils;
import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.hint.MCRHints;
import org.mycore.resource.MCRResourcePath;
import org.mycore.resource.hint.MCRResourceHintKeys;
import org.mycore.resource.provider.MCRResourceProvider.ClassLoaderPrefixStripper;
import org.mycore.resource.provider.MCRResourceProvider.JarUrlPrefixStripper;
import org.mycore.resource.provider.MCRResourceProvider.PrefixStripper;

/**
 * {@link MCRClassLoaderResourceLocator} is an implementation of {@link MCRResourceLocator} that uses
 * {@link ClassLoader#getResources(String)} to locate resources.
 * <p>
 * It uses the {@link ClassLoader} hinted at by {@link MCRResourceHintKeys#CLASS_LOADER}, if present.
 * <p>
 * No configuration options are available, if configured automatically.
 * <p>
 * Example:
 * <pre>
 * [...].Class=org.mycore.resource.locator.MCRClassLoaderResourceLocator
 * </pre>
 */
@MCRConfigurationProxy(proxyClass = MCRClassLoaderResourceLocator.Factory.class)
public class MCRClassLoaderResourceLocator extends MCRResourceLocatorBase {

    @Override
    protected Stream<URL> doLocate(MCRResourcePath path, MCRHints hints) {
        return getClassloader(hints)
            .map(classLoader -> getResources(path, classLoader))
            .map(MCRStreamUtils::asStream)
            .orElse(Stream.empty());
    }

    private Optional<ClassLoader> getClassloader(MCRHints hints) {
        return hints.get(MCRResourceHintKeys.CLASS_LOADER);
    }

    private static Enumeration<URL> getResources(MCRResourcePath path, ClassLoader classLoader) {
        try {
            return classLoader.getResources(path.asRelativePath());
        } catch (IOException e) {
            throw new MCRException(e);
        }
    }

    @Override
    public Stream<PrefixStripper> prefixStrippers(MCRHints hints) {
        return Stream.concat(
            Stream.of(JarUrlPrefixStripper.INSTANCE),
            hints.get(MCRResourceHintKeys.CLASS_LOADER).map(ClassLoaderPrefixStripper::new).stream()
        );
    }

    public static class Factory implements Supplier<MCRClassLoaderResourceLocator> {

        @Override
        public MCRClassLoaderResourceLocator get() {
            return new MCRClassLoaderResourceLocator();
        }

    }

}
