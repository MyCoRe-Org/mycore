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

package org.mycore.resource.locator;

import java.net.URL;
import java.util.stream.Stream;

import org.apache.logging.log4j.Level;
import org.mycore.common.hint.MCRHints;
import org.mycore.common.log.MCRTreeMessage;
import org.mycore.resource.MCRResourcePath;
import org.mycore.resource.common.MCRResourceTracer;

/**
 * {@link MCRResourceLocatorBase} is a base implementation of {@link MCRResourceLocator} that
 * facilitates consistent logging. Implementors must provide the actual locating strategy
 * ({@link MCRResourceLocatorBase#doLocate(MCRResourcePath, MCRHints, MCRResourceTracer)}).
 */
public abstract class MCRResourceLocatorBase implements MCRResourceLocator {

    @Override
    public final Stream<URL> locate(MCRResourcePath path, MCRHints hints, MCRResourceTracer tracer) {
        return tracer.traceStream(hints, doLocate(path, hints, tracer), (appender, locatedResourceUrls) -> {
            if (!locatedResourceUrls.isEmpty()) {
                locatedResourceUrls.forEach(url -> appender.append("Located resource URL " + url));
            } else {
                appender.append("Located no resource URL");
            }
        });
    }

    protected abstract Stream<URL> doLocate(MCRResourcePath path, MCRHints hints, MCRResourceTracer tracer);

    @Override
    public MCRTreeMessage compileDescription(Level level) {
        MCRTreeMessage description = new MCRTreeMessage();
        description.add("Class", getClass().getName());
        return description;
    }

}
