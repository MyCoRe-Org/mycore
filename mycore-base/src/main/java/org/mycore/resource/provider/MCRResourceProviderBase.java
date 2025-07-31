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
import org.mycore.common.hint.MCRHints;
import org.mycore.common.log.MCRTreeMessage;
import org.mycore.resource.MCRResourcePath;
import org.mycore.resource.common.MCRResourceTracer;

/**
 * {@link MCRResourceProviderBase} is a base implementation of {@link MCRResourceProvider} that
 * facilitates consistent logging. Implementors must provide the actual lookup strategy
 * ({@link MCRResourceProviderBase#doProvide(MCRResourcePath, MCRHints, MCRResourceTracer)},
 * {@link MCRResourceProviderBase#doProvideAll(MCRResourcePath, MCRHints, MCRResourceTracer)}).
 */
public abstract class MCRResourceProviderBase implements MCRResourceProvider {

    public static final String COVERAGE_KEY = "Coverage";

    private final String coverage;

    public MCRResourceProviderBase(String coverage) {
        this.coverage = Objects.requireNonNull(coverage, "Coverage must not be null").trim();
    }

    @Override
    public final Optional<URL> provide(MCRResourcePath path, MCRHints hints, MCRResourceTracer tracer) {
        return tracer.trace(hints, doProvide(path, hints, tracer), (appender, providedResourceUrl) -> {
            providedResourceUrl.ifPresentOrElse(url -> appender.append("Providing resource URL " + url),
                () -> appender.append("Providing no resource URL"));
        });
    }

    @Override
    public final List<ProvidedUrl> provideAll(MCRResourcePath path, MCRHints hints, MCRResourceTracer tracer) {
        return tracer.trace(hints, doProvideAll(path, hints, tracer), (appender, providedResourceUrls) -> {
            if (!providedResourceUrls.isEmpty()) {
                providedResourceUrls.forEach(url -> appender.append("Providing resource URL " + url));
            } else {
                appender.append("Providing no resource URLs");
            }
        });
    }

    protected abstract Optional<URL> doProvide(MCRResourcePath path, MCRHints hints, MCRResourceTracer tracer);

    protected abstract List<ProvidedUrl> doProvideAll(MCRResourcePath path, MCRHints hints, MCRResourceTracer tracer);

    protected final ProvidedUrl providedUrl(URL url) {
        return new ProvidedUrl(url, coverage());
    }

    @Override
    public final String coverage() {
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
