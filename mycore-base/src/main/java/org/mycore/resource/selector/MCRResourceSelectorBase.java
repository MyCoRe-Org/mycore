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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.hint.MCRHints;
import org.mycore.common.log.MCRTreeMessage;

/**
 * {@link MCRResourceSelectorBase} is a base implementation of {@link MCRResourceSelector} that
 * facilitates consistent logging. Implementors must provide the actual selection strategy
 * ({@link MCRResourceSelectorBase#doSelect(List, MCRHints)}).
 */
public abstract class MCRResourceSelectorBase implements MCRResourceSelector {

    protected final Logger logger = LogManager.getLogger(getClass());

    @Override
    public List<URL> select(List<URL> resourceUrls, MCRHints hints) {
        logger.debug("Selecting resource URLs");
        List<URL> selectedResourceUrls = doSelect(resourceUrls, hints);
        if (selectedResourceUrls.isEmpty()) {
            selectedResourceUrls = resourceUrls;
        }
        if (logger.isDebugEnabled()) {
            logResourceUrls(selectedResourceUrls);
        } 
        return selectedResourceUrls;
    }

    private void logResourceUrls(List<URL> resourceUrls) {
        for (URL resourceUrl : resourceUrls) {
            logger.debug("Selected resource URL {}", resourceUrl);
        }
    }

    /**
     * Selects prioritized resources from the result of the <em>filter</em>-phase, dropping unprioritized
     * resources. Returns a subset of the given resources. If no prioritization can be made, an unmodified list of
     * resources or an empty list can be returned.
     * <p>
     * This method has slightly different semantics from the public method
     * {@link MCRResourceSelector#select(List, MCRHints)}. The public method requires to return an unmodified list of
     * resources, if no prioritization can be made, whereas this method also allows returning an empty list to signal 
     * that condition. This is to allow simplified implementations that just filter the given list of resource.  
     */
    protected abstract List<URL> doSelect(List<URL> resourceUrls, MCRHints hints);

    @Override
    public MCRTreeMessage compileDescription(Level level) {
        MCRTreeMessage description = new MCRTreeMessage();
        description.add("Class", getClass().getName());
        return description;
    }

}
