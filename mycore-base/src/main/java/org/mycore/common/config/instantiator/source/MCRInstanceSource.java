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
import org.mycore.common.config.annotation.MCRInstance;
import org.mycore.common.config.annotation.MCRSentinel;
import org.mycore.common.config.instantiator.MCRInstanceConfiguration;
import org.mycore.common.config.instantiator.target.MCRTarget;

/**
 * A {@link MCRInstanceSource} is a {@link MCRSource} that interprets a {@link MCRInstance}.
 */
final class MCRInstanceSource extends MCRSourceBase<Object> {

    private final MCRInstance annotation;

    private final MCRSentinel sentinel;

    MCRInstanceSource(MCRInstance annotation, MCRAnnotationProvider annotationProvider) {
        this.annotation = annotation;
        this.sentinel = annotationProvider.get(MCRSentinel.class);
    }

    @Override
    public Type type() {
        return Type.INSTANCE;
    }

    @Override
    public Class<MCRInstance> annotationClass() {
        return MCRInstance.class;
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
        return annotation.valueClass();
    }

    @Override
    protected String description() {
        return "instance";
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
        return false;
    }

    @Override
    protected boolean required() {
        return annotation.required();
    }

    @Override
    protected String defaultName() {
        return "";
    }

    @Override
    protected Object getResult(MCRSourceContext context, MCRInstanceConfiguration<?> configuration,
        Map<String, String> properties, String prefix) {

        MCRInstanceConfiguration<?> nestedConfiguration = configuration.nested(annotation.valueClass(), name());
        return createInstance(context, nestedConfiguration, sentinel);

    }

    @Override
    protected boolean isMissingResult(Object result) {
        return result == null;
    }

    @Override
    protected MCRConfigurationException missingResultException(MCRSourceContext context) {
        return context.missingException();
    }

    @Override
    protected Object missingResultReplacement() {
        return null;
    }

}
