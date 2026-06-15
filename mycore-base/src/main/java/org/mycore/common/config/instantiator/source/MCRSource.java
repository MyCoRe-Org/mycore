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
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.mycore.common.config.annotation.MCRInstance;
import org.mycore.common.config.annotation.MCRInstanceList;
import org.mycore.common.config.annotation.MCRInstanceMap;
import org.mycore.common.config.annotation.MCRPostConstruction;
import org.mycore.common.config.annotation.MCRProperty;
import org.mycore.common.config.annotation.MCRPropertyList;
import org.mycore.common.config.annotation.MCRPropertyMap;
import org.mycore.common.config.annotation.MCRRawProperties;
import org.mycore.common.config.instantiator.MCRInstanceConfiguration;
import org.mycore.common.config.instantiator.target.MCRTarget;

/**
 * Common abstraction for components of a class (i.e., a {@link Field} or a {@link Method}) that can,
 * in principle, be used for injection. It provides an abstraction for obtaining a value based on present annotations.
 */
public sealed interface MCRSource permits MCRInstanceListSource, MCRInstanceMapSource, MCRInstanceSource,
    MCRPostConstructionSource, MCRPropertyListSource, MCRPropertyMapSource, MCRPropertySource, MCRRawPropertiesSource {

    Type type();

    Class<? extends Annotation> annotationClass();

    int order();

    Set<MCRTarget.Type> allowedTargetTypes();

    Class<?> valueClass();

    Object get(MCRInstanceConfiguration<?> configuration, MCRTarget target);

    enum Type {

        PROPERTY(
            Group.VALUE_INJECTION,
            new Mapper<>(MCRProperty.class, MCRPropertySource::new)),

        PROPERTY_MAP(
            Group.VALUE_INJECTION,
            new Mapper<>(MCRPropertyMap.class, MCRPropertyMapSource::new)),

        PROPERTY_LIST(
            Group.VALUE_INJECTION,
            new Mapper<>(MCRPropertyList.class, MCRPropertyListSource::new)),

        RAW_PROPERTIES(
            Group.VALUE_INJECTION,
            new Mapper<>(MCRRawProperties.class, MCRRawPropertiesSource::new)),

        INSTANCE(
            Group.VALUE_INJECTION,
            new Mapper<>(MCRInstance.class, MCRInstanceSource::new)),

        INSTANCE_MAP(
            Group.VALUE_INJECTION,
            new Mapper<>(MCRInstanceMap.class, MCRInstanceMapSource::new)),

        INSTANCE_LIST(
            Group.VALUE_INJECTION,
            new Mapper<>(MCRInstanceList.class, MCRInstanceListSource::new)),

        POST_CONSTRUCTION(
            Group.POST_CONSTRUCTION,
            new Mapper<>(MCRPostConstruction.class, MCRPostConstructionSource::new));

        private final Group group;

        private final Mapper<? extends Annotation> mapper;

        Type(Group group, Mapper<? extends Annotation> mapper) {
            this.group = group;
            this.mapper = mapper;
        }

        public int order() {
            return group.ordinal();
        }

        public Optional<MCRSource> toSource(MCRAnnotationProvider annotationProvider) {
            return mapper.toSource(annotationProvider);
        }

        private enum Group {

            VALUE_INJECTION,

            POST_CONSTRUCTION

        }

        private record Mapper<A extends Annotation>(Class<A> annotationClass,
            BiFunction<A, MCRAnnotationProvider, MCRSource> factory) implements Serializable {

            @Serial
            private static final long serialVersionUID = 1L;

            private Mapper(Class<A> annotationClass, Function<A, MCRSource> factory) {
                this(annotationClass, (annotation, _) -> factory.apply(annotation));
            }

            private Optional<MCRSource> toSource(MCRAnnotationProvider annotationProvider) {
                return Optional.ofNullable(annotationProvider.get(annotationClass()))
                    .map(annotation -> factory.apply(annotation, annotationProvider));
            }

        }

    }

}
