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

import org.mycore.common.config.annotation.MCRPropertyMap;
import org.mycore.common.config.instantiator.target.MCRTarget;

/**
 * A {@link MCRPropertyMapSource} is a {@link MCRSource} that interprets a {@link MCRPropertyMap}.
 */
final class MCRPropertyMapSource extends MCRValueMapSourceBase<String> {

    private final MCRPropertyMap annotation;

    MCRPropertyMapSource(MCRPropertyMap annotation, MCRAnnotationProvider annotationProvider) {
        super(annotationProvider, new MCRPropertyExtractor());
        this.annotation = annotation;
    }

    @Override
    public Type type() {
        return Type.PROPERTY_MAP;
    }

    @Override
    public Class<MCRPropertyMap> annotationClass() {
        return MCRPropertyMap.class;
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
        return Map.class;
    }

    @Override
    protected String description() {
        return "property map";
    }

    @Override
    protected String name() {
        return annotation.name();
    }

    @Override
    protected String defaultName() {
        return annotation.defaultName();
    }

    @Override
    protected boolean supportsEmptyName() {
        return true;
    }

    @Override
    protected boolean supportsAbsoluteName() {
        return annotation.absolute();
    }

    @Override
    protected boolean supportsShortForm() {
        return true;
    }

    @Override
    protected boolean required() {
        return annotation.required();
    }

}
