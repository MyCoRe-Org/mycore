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

package org.mycore.resource.filter;

import static org.mycore.resource.common.MCRTraceLoggingHelper.traceStream;

import java.net.URL;
import java.util.stream.Stream;

import org.apache.logging.log4j.Level;
import org.mycore.common.hint.MCRHints;
import org.mycore.common.log.MCRTreeMessage;

/**
 * {@link MCRResourceFilterBase} is a base implementation of {@link MCRResourceFilter} that
 * facilitates consistent logging. Implementors must provide the actual filtering strategy
 * ({@link MCRResourceFilterBase#doFilter(Stream, MCRHints)}).
 */
public abstract class MCRResourceFilterBase implements MCRResourceFilter {

    @Override
    public Stream<URL> filter(Stream<URL> resourceUrls, MCRHints hints) {
        return traceStream(hints, doFilter(resourceUrls, hints), (message, filteredResourceUrls) -> {
            if (!filteredResourceUrls.isEmpty()) {
                filteredResourceUrls.forEach(url -> message.add("Keeping resource URL " + url));
            } else {
                message.add("Keeping no resource URL");
            }
        });
    }

    protected abstract Stream<URL> doFilter(Stream<URL> resourceUrls, MCRHints hints);

    @Override
    public MCRTreeMessage compileDescription(Level level) {
        MCRTreeMessage description = new MCRTreeMessage();
        description.add("Class", getClass().getName());
        return description;
    }

}
