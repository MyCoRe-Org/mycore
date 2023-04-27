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

package org.mycore.common.resource.provider;

import java.net.URL;
import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.Level;
import org.mycore.common.hint.MCRHints;
import org.mycore.common.log.MCRTreeMessage;
import org.mycore.common.resource.MCRResourcePath;

/**
 * A {@link MCRResourceProvider} implements a resource lookup strategy.
 */
public interface MCRResourceProvider {

    /**
     * Resolves a {@link MCRResourcePath} using the given hints.
     */
    Optional<URL> provide(MCRResourcePath path, MCRHints hints);

    /**
     * Resolves a {@link MCRResourcePath}, returning all alternatives (i.e. because one module
     * overrides a resource that is also provided by another module). Intended for introspective purposes only.
     */
    List<ProvidedURL> provideAll(MCRResourcePath path, MCRHints hints);

    /**
     * Returns a description of this {@link MCRResourceProvider}.
     */
    MCRTreeMessage compileDescription(Level level);

    final class ProvidedURL {

        public final URL url;

        public final String origin;

        public ProvidedURL(URL url, String origin) {
            this.url = url;
            this.origin = origin;
        }

        @Override
        public String toString() {
            return origin + ": " + url;
        }

    }

}
