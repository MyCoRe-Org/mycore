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

package org.mycore.common.resource.locator;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.mycore.common.MCRException;
import org.mycore.common.MCRStreamUtils;
import org.mycore.common.hint.MCRHints;
import org.mycore.common.resource.MCRResourcePath;
import org.mycore.common.resource.hint.MCRResourceHintKeys;
import org.mycore.common.resource.provider.MCRResourceProvider;

/**
 * A {@link MCRClassLoaderResourceLocator} is a {@link MCRResourceLocator} that uses
 * {@link ClassLoader#getResources(String)} to locate resources.
 * <p>
 * It uses the {@link ClassLoader} hinted at by {@link MCRResourceHintKeys#CLASS_LOADER}, if present.
 */
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
    public Set<MCRResourceProvider.PrefixStripper> prefixPatterns(MCRHints hints) {
        return MCRResourceProvider.JarUrlPrefixStripper.INSTANCE_SET;
    }

}
