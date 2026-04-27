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

import java.io.Serial;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.Optional;
import java.util.function.Function;

import org.mycore.common.config.annotation.MCRInstance;
import org.mycore.common.config.annotation.MCRInstanceList;
import org.mycore.common.config.annotation.MCRInstanceMap;
import org.mycore.common.config.annotation.MCRPostConstruction;
import org.mycore.common.config.annotation.MCRProperty;
import org.mycore.common.config.annotation.MCRPropertyList;
import org.mycore.common.config.annotation.MCRPropertyMap;
import org.mycore.common.config.annotation.MCRRawProperties;

public enum MCRSourceType {

    PROPERTY(Group.VALUE_INJECTION, new Mapper<>(MCRProperty.class, MCRPropertySource::new)),

    PROPERTY_MAP(Group.VALUE_INJECTION, new Mapper<>(MCRPropertyMap.class, MCRPropertyMapSource::new)),

    PROPERTY_LIST(Group.VALUE_INJECTION, new Mapper<>(MCRPropertyList.class, MCRPropertyListSource::new)),

    RAW_PROPERTIES(Group.VALUE_INJECTION, new Mapper<>(MCRRawProperties.class, MCRRawPropertiesSource::new)),

    INSTANCE(Group.VALUE_INJECTION, new Mapper<>(MCRInstance.class, MCRInstanceSource::new)),

    INSTANCE_MAP(Group.VALUE_INJECTION, new Mapper<>(MCRInstanceMap.class, MCRInstanceMapSource::new)),

    INSTANCE_LIST(Group.VALUE_INJECTION, new Mapper<>(MCRInstanceList.class, MCRInstanceListSource::new)),

    POST_CONSTRUCTION(Group.POST_CONSTRUCTION, new Mapper<>(MCRPostConstruction.class, MCRPostConstructionSource::new));

    private final Group group;

    private final Mapper<? extends Annotation> mapper;

    MCRSourceType(Group group, Mapper<? extends Annotation> mapper) {
        this.group = group;
        this.mapper = mapper;
    }

    public int order() {
        return group.ordinal();
    }

    public Optional<MCRSource> toSource(AnnotationProvider annotationProvider) {
        return mapper.toSource(annotationProvider);
    }

    public interface AnnotationProvider {

        <A extends Annotation> A get(Class<A> annotationClass);

    }

    private enum Group {

        VALUE_INJECTION,

        POST_CONSTRUCTION

    }

    private record Mapper<A extends Annotation>(Class<A> annotationClass, Function<A, MCRSource> factory)
        implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private Optional<MCRSource> toSource(AnnotationProvider annotationProvider) {
            return Optional.ofNullable(annotationProvider.get(annotationClass())).map(factory);
        }

    }

}
