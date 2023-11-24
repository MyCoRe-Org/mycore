/*
 *
 * $Revision$ $Date$
 *
 * This file is part of *** M y C o R e *** See http://www.mycore.de/ for
 * details.
 *
 * This program is free software; you can use it, redistribute it and / or
 * modify it under the terms of the GNU General Public License (GPL) as
 * published by the Free Software Foundation; either version 2 of the License or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program, in a file called gpl.txt or license.txt. If not, write to the
 * Free Software Foundation Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307 USA
 */

package org.mycore.resource.provider;

import java.net.URL;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.mycore.common.config.annotation.MCRConfigurationProxy;
import org.mycore.common.config.annotation.MCRProperty;
import org.mycore.common.hint.MCRHints;
import org.mycore.resource.MCRResourcePath;
import org.mycore.resource.hint.MCRResourceHintKeys;

/**
 * A {@link MCRClassLoaderResourceProvider} is a {@link MCRResourceProvider} that uses
 * {@link ClassLoader#getResource(String)} to lookup a resource.
 * <p>
 * It uses the {@link ClassLoader} hinted at by {@link MCRResourceHintKeys#CLASS_LOADER}, if present.
 */
@MCRConfigurationProxy(proxyClass = MCRClassLoaderResourceProvider.Factory.class)
public class MCRClassLoaderResourceProvider extends MCRResourceProviderBase {

    public MCRClassLoaderResourceProvider(String coverage) {
        super(coverage);
    }

    @Override
    protected final Optional<URL> doProvide(MCRResourcePath path, MCRHints hints) {
        return getClassloader(hints).map(classLoader -> classLoader.getResource(path.asRelativePath()));
    }

    @Override
    protected final List<ProvidedUrl> doProvideAll(MCRResourcePath path, MCRHints hints) {
        return doProvide(path, hints).stream().map(this::providedURL).collect(Collectors.toList());
    }

    private Optional<ClassLoader> getClassloader(MCRHints hints) {
        return hints.get(MCRResourceHintKeys.CLASS_LOADER);
    }

    @Override
    public Set<PrefixStripper> prefixStrippers(MCRHints hints) {
        Set<PrefixStripper> strippers = new LinkedHashSet<>(JarUrlPrefixStripper.INSTANCE_SET);
        hints.get(MCRResourceHintKeys.CLASS_LOADER).ifPresent(classLoader ->
            strippers.addAll(BaseDirPrefixStripper.ofClassLoader(classLoader)));
        return strippers;
    }

    public static class Factory implements Supplier<MCRClassLoaderResourceProvider> {

        @MCRProperty(name = "Coverage", defaultName = "MCR.Resource.Provider.Default.ClassLoader.Coverage")
        public String coverage;

        @Override
        public MCRClassLoaderResourceProvider get() {
            return new MCRClassLoaderResourceProvider(coverage);
        }

    }

}
