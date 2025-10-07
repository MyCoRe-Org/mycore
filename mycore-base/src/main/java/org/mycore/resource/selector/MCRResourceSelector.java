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

package org.mycore.resource.selector;

import java.net.URL;
import java.util.List;

import org.apache.logging.log4j.Level;
import org.mycore.common.hint.MCRHints;
import org.mycore.common.log.MCRTreeMessage;
import org.mycore.resource.common.MCRResourceTracer;
import org.mycore.resource.filter.MCRResourceFilter;
import org.mycore.resource.provider.MCRLFSResourceProvider;

/**
 * A {@link MCRResourceFilter} implements the <em>select</em>-phase for a {@link MCRLFSResourceProvider}.
 * <p>
 * The <em>select</em>-phase differs from the <em>filter</em>-phase in that in the <em>filter</em>-phase
 * resources are evaluated as they are, in the <em>select</em>-phase resources are compared to other
 * resources that have been located and filtered and potentially reordered.
 */
public interface MCRResourceSelector {

    /**
     * Selects prioritized resources from the result of the <em>filter</em>-phase, dropping unprioritized
     * resources. Returns a non-empty subset of the given resources. If no prioritization can be made,
     * the whole set of resources must be returned.
     */
    List<URL> select(List<URL> resourceUrls, MCRHints hints);

    /**
     * Selects prioritized resources from the result of the <em>filter</em>-phase, dropping unprioritized
     * resources. Returns a non-empty subset of the given resources. If no prioritization can be made,
     * the whole set of resources must be returned.
     */
    List<URL> select(List<URL> resourceUrls, MCRHints hints, MCRResourceTracer tracer);

    /**
     * Returns a description of this {@link MCRCombinedResourceSelector}.
     */
    MCRTreeMessage compileDescription(Level level);

}
