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

package org.mycore.resource.provider;

import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.hint.MCRHints;
import org.mycore.common.log.MCRTreeMessage;
import org.mycore.resource.MCRResourcePath;

/**
 * {@link MCRResourceProviderBase} is a base implementation of {@link MCRResourceProvider} that
 * facilitates consistent logging. Implementors must provide the actual lookup strategy
 * ({@link MCRResourceProviderBase#doProvide(MCRResourcePath, MCRHints)},
 * {@link MCRResourceProviderBase#doProvideAll(MCRResourcePath, MCRHints)}).
 */
public abstract class MCRResourceProviderBase implements MCRResourceProvider {

    public static final String COVERAGE_KEY = "Coverage";

    protected final Logger logger = LogManager.getLogger(getClass());

    private final String coverage;

    public MCRResourceProviderBase(String coverage) {
        this.coverage = Objects.requireNonNull(coverage, "Coverage must not be null").trim();
    }

    @Override
    public final Optional<URL> provide(MCRResourcePath path, MCRHints hints) {
        logger.debug("Providing resource URLs for path {} [{}]", path, coverage);
        Optional<URL> resourceUrl = doProvide(path, hints);
        if (logger.isDebugEnabled()) {
            return logResourceUrl(resourceUrl);
        } else {
            return resourceUrl;
        }
    }

    private Optional<URL> logResourceUrl(Optional<URL> resourceUrl) {
        resourceUrl.ifPresent(url -> logger.debug("Providing resource URL {} [{}]", url, coverage));
        return resourceUrl;
    }

    @Override
    public final List<ProvidedUrl> provideAll(MCRResourcePath path, MCRHints hints) {
        logger.debug("Providing all resource URLs for path {} [{}]", path, coverage);
        List<ProvidedUrl> resourceUrls = doProvideAll(path, hints);
        if (logger.isDebugEnabled()) {
            logResourceUrls(resourceUrls);
        }
        return resourceUrls;

    }

    private void logResourceUrls(List<ProvidedUrl> resourceUrl) {
        resourceUrl.forEach(url -> logger.debug("Providing resource URL {} [{}]", url.url(), coverage));
    }

    protected abstract Optional<URL> doProvide(MCRResourcePath path, MCRHints hints);

    protected abstract List<ProvidedUrl> doProvideAll(MCRResourcePath path, MCRHints hints);

    protected final ProvidedUrl providedUrl(URL url) {
        return new ProvidedUrl(url, coverage());
    }

    public String coverage() {
        return coverage;
    }

    @Override
    public MCRTreeMessage compileDescription(Level level) {
        MCRTreeMessage description = new MCRTreeMessage();
        description.add("Class", getClass().getName());
        description.add("Coverage", coverage());
        return description;
    }

}
