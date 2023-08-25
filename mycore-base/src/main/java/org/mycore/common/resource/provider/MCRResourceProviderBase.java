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

package org.mycore.common.resource.provider;

import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.hint.MCRHints;
import org.mycore.common.log.MCRTreeMessage;
import org.mycore.common.resource.MCRResourcePath;

/**
 * {@link MCRResourceProviderBase} is a base implementation of {@link MCRResourceProvider} that
 * facilitates consistent logging. Implementors must provide a class-specific {@link Logger} and the
 * actual lookup strategy ({@link MCRResourceProviderBase#doProvide(MCRResourcePath, MCRHints)},
 * {@link MCRResourceProviderBase#doProvideAll(MCRResourcePath, MCRHints)}).
 */
public abstract class MCRResourceProviderBase implements MCRResourceProvider {

    private final Logger logger = LogManager.getLogger();

    private final String coverage;

    public MCRResourceProviderBase(String coverage) {
        this.coverage = Objects.requireNonNull(coverage);
    }

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

    public final List<ProvidedUrl> provideAll(MCRResourcePath path, MCRHints hints) {
        logger.debug("Providing all resource URLs for path {} [{}]", path, coverage);
        List<ProvidedUrl> resourceUrls = doProvideAll(path, hints);
        if (logger.isDebugEnabled()) {
            return logResourceUrls(resourceUrls);
        } else {
            return resourceUrls;
        }
    }

    private List<ProvidedUrl> logResourceUrls(List<ProvidedUrl> resourceUrl) {
        resourceUrl.forEach(url -> logger.debug("Providing resource URL {} [{}]", url.url, coverage));
        return resourceUrl;
    }

    protected final Logger getLogger() {
        return logger;
    }

    protected abstract Optional<URL> doProvide(MCRResourcePath path, MCRHints hints);

    protected abstract List<ProvidedUrl> doProvideAll(MCRResourcePath path, MCRHints hints);

    protected final ProvidedUrl providedURL(URL url) {
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
