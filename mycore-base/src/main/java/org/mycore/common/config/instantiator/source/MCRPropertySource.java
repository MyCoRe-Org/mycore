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
import java.util.Set;

import org.mycore.common.config.MCRConfigurationException;
import org.mycore.common.config.annotation.MCRProperty;
import org.mycore.common.config.annotation.MCRSentinel;
import org.mycore.common.config.instantiator.MCRInstanceConfiguration;
import org.mycore.common.config.instantiator.target.MCRTarget;

/**
 * A {@link MCRPropertySource} is a {@link MCRSource} that interprets a {@link MCRProperty}.
 */
final class MCRPropertySource extends MCRSourceBase<String> {

    private final MCRProperty annotation;

    private final MCRSentinel sentinel;

    MCRPropertySource(MCRProperty annotation, MCRAnnotationProvider annotationProvider) {
        this.annotation = annotation;
        this.sentinel = annotationProvider.get(MCRSentinel.class);
    }

    @Override
    public Type type() {
        return Type.PROPERTY;
    }

    @Override
    public Class<MCRProperty> annotationClass() {
        return MCRProperty.class;
    }

    @Override
    public int order() {
        return annotation.order();
    }

    @Override
    public Set<MCRTarget.Type> allowedTargetTypes() {
        return MCRTarget.Types.ALL;
    }

    @Override
    public Class<?> valueClass() {
        return String.class;
    }

    @Override
    protected String description() {
        return "property";
    }

    @Override
    protected String name() {
        return annotation.name();
    }

    @Override
    protected boolean allowsEmptyName() {
        return false;
    }

    @Override
    protected boolean absolute() {
        return annotation.absolute();
    }

    @Override
    protected boolean required() {
        return annotation.required();
    }

    @Override
    protected String defaultName() {
        return annotation.defaultName();
    }

    @Override
    protected String getResult(MCRSourceContext context, MCRInstanceConfiguration<?> configuration,
        Map<String, String> properties, String prefix) {

        if (rejectedBySentinel(sentinel, context, properties, prefix + ".")) {
            return null;
        }

        return properties.get(prefix);

    }

    @Override
    protected boolean isMissingResult(String result) {
        return result == null;
    }

    @Override
    protected MCRConfigurationException missingException(MCRSourceContext context) {
        return context.missingException();
    }

    @Override
    protected String nullResultReplacement() {
        return null;
    }

}
