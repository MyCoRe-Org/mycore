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
import org.mycore.resource.common.MCRNoOpResourceTracer;
import org.mycore.resource.common.MCRResourceTracer;

/**
 * {@link MCRResourceSelectorBase} is a base implementation of {@link MCRResourceSelector} that
 * facilitates consistent logging. Implementors must provide the actual selection strategy
 * ({@link MCRResourceSelectorBase#doSelect(List, MCRHints, MCRResourceTracer)}).
 */
public abstract class MCRResourceSelectorBase implements MCRResourceSelector {

    @Override
    public final List<URL> select(List<URL> resourceUrls, MCRHints hints) {
        return select(resourceUrls, hints, new MCRNoOpResourceTracer());
    }

    @Override
    public List<URL> select(List<URL> resourceUrls, MCRHints hints, MCRResourceTracer tracer) {
        return tracer.trace(hints, doSelectOrRestore(resourceUrls, hints, tracer), (appender, selectedResourceUrls) -> {
            selectedResourceUrls.forEach(url -> appender.append("Selecting resource URL " + url));
        });
    }

    private List<URL> doSelectOrRestore(List<URL> resourceUrls, MCRHints hints, MCRResourceTracer tracer) {
        List<URL> selectedResourceUrls = doSelect(resourceUrls, hints, tracer);
        if (selectedResourceUrls.isEmpty()) {
            selectedResourceUrls = resourceUrls;
        }
        return selectedResourceUrls;
    }

    /**
     * Selects prioritized resources from the result of the <em>filter</em>-phase, dropping unprioritized
     * resources. Returns a subset of the given resources.
     * <p>
     * This method has slightly different semantics compared to
     * {@link MCRResourceSelector#select(List, MCRHints, MCRResourceTracer)}. That method requires to return a
     * non-empty subset of the given resources. If no prioritization can be made, the whole set of resources must
     * be returned.
     * <p>
     * This method, however, allows an empty list to be returned instead, to signal that no prioritization could
     * be made. This allows simple filter-style implementations, where filtering out all resources means that no
     * prioritized resource could be found.
     */
    protected abstract List<URL> doSelect(List<URL> resourceUrls, MCRHints hints, MCRResourceTracer tracer);

    @Override
    public MCRTreeMessage compileDescription(Level level) {
        MCRTreeMessage description = new MCRTreeMessage();
        description.add("Class", getClass().getName());
        return description;
    }

}
