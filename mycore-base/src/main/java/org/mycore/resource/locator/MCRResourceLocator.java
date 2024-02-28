/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
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
import java.util.stream.Stream;

import org.apache.logging.log4j.Level;
import org.mycore.common.hint.MCRHints;
import org.mycore.common.log.MCRTreeMessage;
import org.mycore.resource.MCRResourcePath;
import org.mycore.resource.filter.MCRResourceFilter;
import org.mycore.resource.provider.MCRLFSResourceProvider;
import org.mycore.resource.provider.MCRResourceProvider.PrefixStripper;

/**
 * A {@link MCRResourceFilter} implements the <em>locate</em>-phase for a {@link MCRLFSResourceProvider}.
 */
public interface MCRResourceLocator {

    /**
     * Resolves a {@link MCRResourcePath}, locating possible candidates.
     */
    Stream<URL> locate(MCRResourcePath path, MCRHints hints);

    /**
     * Returns a description of this {@link MCRResourceLocator}.
     */
    MCRTreeMessage compileDescription(Level level);

    Stream<PrefixStripper> prefixStrippers(MCRHints hints);

}
