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

import org.mycore.common.config.annotation.MCRInstanceMap;
import org.mycore.common.config.instantiator.target.MCRTarget;

/**
 * A {@link MCRInstanceMapSource} is a {@link MCRSource} that interprets a {@link MCRInstanceMap}.
 */
final class MCRInstanceMapSource extends MCRValueMapSourceBase<Object> {

    private final MCRInstanceMap annotation;

    MCRInstanceMapSource(MCRInstanceMap annotation, MCRAnnotationProvider annotationProvider) {
        super(annotationProvider, new MCRInstanceExtractor(annotation.valueClass()));
        this.annotation = annotation;
    }

    @Override
    public Type type() {
        return Type.INSTANCE_MAP;
    }

    @Override
    public Class<MCRInstanceMap> annotationClass() {
        return MCRInstanceMap.class;
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
        return "instance map";
    }

    @Override
    protected String name() {
        return annotation.name();
    }

    @Override
    protected String defaultName() {
        return "";
    }

    @Override
    protected boolean supportsEmptyName() {
        return true;
    }

    @Override
    protected boolean supportsAbsoluteName() {
        return false;
    }

    @Override
    protected boolean supportsShortForm() {
        return false;
    }

    @Override
    protected boolean required() {
        return annotation.required();
    }

}
