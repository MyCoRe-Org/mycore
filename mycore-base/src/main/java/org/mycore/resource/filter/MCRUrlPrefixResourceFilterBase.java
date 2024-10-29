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

import java.net.URL;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.logging.log4j.Level;
import org.mycore.common.hint.MCRHints;
import org.mycore.common.log.MCRTreeMessage;

/**
 * {@link MCRUrlPrefixResourceFilterBase} is a base implementation of a {@link MCRResourceFilter} that
 * filters resource candidates by comparing them against a given URL prefix. Implementors must provide the
 * URL prefix to compare to.
 **/
public abstract class MCRUrlPrefixResourceFilterBase extends MCRResourceFilterBase {

    private final MCRResourceFilterMode mode;

    public MCRUrlPrefixResourceFilterBase(MCRResourceFilterMode mode) {
        this.mode = Objects.requireNonNull(mode);
    }

    @Override
    protected final Stream<URL> doFilter(Stream<URL> resourceUrls, MCRHints hints) {
        return getPrefix(hints).map(prefix -> filter(resourceUrls, prefix)).orElse(Stream.empty());
    }

    protected abstract Optional<String> getPrefix(MCRHints hints);

    private Stream<URL> filter(Stream<URL> resourceUrls, String prefix) {
        return resourceUrls.filter(url -> filter(prefix, url));
    }

    private boolean filter(String prefix, URL url) {
        return url.toString().startsWith(prefix) == mode.expectedComparisonResult;
    }

    @Override
    public MCRTreeMessage compileDescription(Level level) {
        MCRTreeMessage description = super.compileDescription(level);
        description.add("Mode", mode.name());
        return description;
    }

}
