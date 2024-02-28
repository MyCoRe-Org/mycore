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
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.hint.MCRHints;
import org.mycore.common.log.MCRTreeMessage;
import org.mycore.resource.MCRResourcePath;

/**
 * {@link MCRResourceLocatorBase} is a base implementation of {@link MCRResourceLocator} that
 * facilitates consistent logging. Implementors must provide a class-specific {@link Logger} and the
 * actual locating strategy ({@link MCRResourceLocatorBase#doLocate(MCRResourcePath, MCRHints)}).
 */
public abstract class MCRResourceLocatorBase implements MCRResourceLocator {

    private final Logger logger = LogManager.getLogger(getClass());

    public final Stream<URL> locate(MCRResourcePath path, MCRHints hints) {
        logger.debug("Locating resource URLs for path {}", path);
        Stream<URL> locatedResourceUrls = doLocate(path, hints);
        if (logger.isDebugEnabled()) {
            return logResourceUrls(locatedResourceUrls.collect(Collectors.toList())).stream();
        } else {
            return locatedResourceUrls;
        }
    }

    private List<URL> logResourceUrls(List<URL> resourceUrls) {
        for (URL resourceUrl : resourceUrls) {
            logger.debug("Located resource URL {}", resourceUrl);
        }
        return resourceUrls;
    }

    protected final Logger getLogger() {
        return logger;
    }

    protected abstract Stream<URL> doLocate(MCRResourcePath path, MCRHints hints);

    @Override
    public MCRTreeMessage compileDescription(Level level) {
        MCRTreeMessage description = new MCRTreeMessage();
        description.add("Class", getClass().getName());
        return description;
    }

}
