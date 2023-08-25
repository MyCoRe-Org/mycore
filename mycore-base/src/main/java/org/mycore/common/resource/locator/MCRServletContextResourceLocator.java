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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import org.mycore.common.MCRException;
import org.mycore.common.hint.MCRHints;
import org.mycore.common.resource.MCRResourcePath;
import org.mycore.common.resource.hint.MCRResourceHintKeys;
import org.mycore.common.resource.provider.MCRResourceProvider;

import jakarta.servlet.ServletContext;

/**
 * A {@link MCRServletContextResourceLocator} is a {@link MCRResourceLocator} that uses
 * {@link ServletContext#getResource(String)}} to locate a resources.
 * <p>
 * It uses the {@link ServletContext} hinted at by {@link MCRResourceHintKeys#SERVLET_CONTEXT}, if present.
 */
public class MCRServletContextResourceLocator extends MCRResourceLocatorBase {

    @Override
    protected Stream<URL> doLocate(MCRResourcePath path, MCRHints hints) {
        return combine(getServletContext(hints), getWebResourcePath(path), this::getResourceUrl).stream();
    }

    private static Optional<ServletContext> getServletContext(MCRHints hints) {
        return hints.get(MCRResourceHintKeys.SERVLET_CONTEXT);
    }

    private static Optional<String> getWebResourcePath(MCRResourcePath path) {
        return path.asAbsoluteWebPath();
    }

    private Optional<URL> getResourceUrl(ServletContext context, String path) {
        try {
            return Optional.ofNullable(context.getResource(path));
        } catch (MalformedURLException e) {
            throw new MCRException("Failed to convert path to URL: " + path, e);
        }
    }

    private static <L, R, T> Optional<T> combine(Optional<L> l, Optional<R> r, BiFunction<L, R, Optional<T>> f) {
        return l.flatMap(leftValue -> r.flatMap(rightValue -> f.apply(leftValue, rightValue)));
    }

    @Override
    public Set<MCRResourceProvider.PrefixStripper> prefixPatterns(MCRHints hints) {
        return MCRResourceProvider.JarUrlPrefixStripper.INSTANCE_SET;
    }

}
