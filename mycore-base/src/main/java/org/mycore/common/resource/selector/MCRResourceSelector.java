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

package org.mycore.common.resource.selector;

import java.net.URL;
import java.util.List;

import org.apache.logging.log4j.Level;
import org.mycore.common.hint.MCRHints;
import org.mycore.common.log.MCRTreeMessage;
import org.mycore.common.resource.filter.MCRResourceFilter;
import org.mycore.common.resource.provider.MCRLFSResourceProvider;

/**
 * A {@link MCRResourceFilter} implements the <em>select</em>-phase for a {@link MCRLFSResourceProvider}.
 * <p>
 * The <em>select</em>-phase differs from the <em>filter</em>-phase in that in the <em>filter</em>-phase
 * resources are evaluated as they are, in the <em>select</em>-phase resources are compared to other
 * resources that have been located and filtered.
 */
public interface MCRResourceSelector {

    /**
     * Selects prioritized resources from the result of the <em>filter</em>-phase, dropping unprioritized
     * resources. Returns a subset of the given resources that must not be empty. If no prioritization
     * can be made, the given resources must be returned.
     */
    List<URL> select(List<URL> resourceUrls, MCRHints hints);

    /**
     * Returns a description of this {@link MCRCombinedResourceSelector}.
     */
    MCRTreeMessage compileDescription(Level level);

}
