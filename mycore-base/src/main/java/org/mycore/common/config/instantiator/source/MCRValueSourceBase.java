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

package org.mycore.common.config.instantiator.source;

import java.util.Map;

import org.mycore.common.config.MCRConfigurationException;
import org.mycore.common.config.annotation.MCRSentinel;

/**
 * A {@link MCRValueSourceBase} is a base implementation of {@link MCRSource} that
 * obtains a single value for annotation based injection from properties.
 * It provides support for {@link MCRSentinel} and
 * uses a {@link MCRValueExtractor} to obtain a single value from properties.
 *
 * @param <Value> the type of injected value.
 */
abstract class MCRValueSourceBase<Value> extends MCRSourceBase<Value> {

    private final MCRSentinel sentinel;

    private final MCRValueExtractor<Value> extractor;

    MCRValueSourceBase(MCRAnnotationProvider annotationProvider, MCRValueExtractor<Value> extractor) {
        this.sentinel = annotationProvider.get(MCRSentinel.class);
        this.extractor = extractor;
    }

    @Override
    protected final Value getResult(MCRSourceContext context, Map<String, String> properties,
        Map<String, String> fullProperties) {

        if (rejectedBySentinel(sentinel, context, properties)) {
            return null;
        }

        return extractor.toValue(context, properties, fullProperties);

    }

    @Override
    protected final boolean isMissingResult(Value result) {
        return result == null;
    }

    @Override
    protected final MCRConfigurationException missingResultException(MCRSourceContext context) {
        return context.missingException();
    }

    @Override
    protected final Value missingResultReplacement() {
        return null;
    }

}
