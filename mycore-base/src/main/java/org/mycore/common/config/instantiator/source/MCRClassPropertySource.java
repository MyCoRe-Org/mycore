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

import java.util.Set;

import org.mycore.common.config.annotation.MCRClassProperty;
import org.mycore.common.config.instantiator.target.MCRTarget;

/**
 * A {@link MCRClassPropertySource} is a {@link MCRSource} that interprets a {@link MCRClassProperty}.
 */
final class MCRClassPropertySource extends MCRValueSourceBase<Class<?>> {

    private final MCRClassProperty annotation;

    MCRClassPropertySource(MCRClassProperty annotation, MCRAnnotationProvider annotationProvider) {
        super(annotationProvider, new MCRClassPropertyExtractor(annotation.valueClass()));
        this.annotation = annotation;
    }

    @Override
    public Type type() {
        return Type.PROPERTY;
    }

    @Override
    public Class<MCRClassProperty> annotationClass() {
        return MCRClassProperty.class;
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
        return Class.class;
    }

    @Override
    protected String description() {
        return "class";
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

}
