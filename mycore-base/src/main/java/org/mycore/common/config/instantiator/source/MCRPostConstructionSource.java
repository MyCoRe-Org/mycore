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

import org.mycore.common.config.annotation.MCRPostConstruction;
import org.mycore.common.config.instantiator.MCRInstanceConfiguration;
import org.mycore.common.config.instantiator.target.MCRTarget;

/**
 * A {@link MCRPostConstructionSource} is a {@link MCRSource} that interprets a {@link MCRPostConstruction}.
 */
final class MCRPostConstructionSource implements MCRSource {

    private final MCRPostConstruction annotation;

    MCRPostConstructionSource(MCRPostConstruction annotation) {
        this.annotation = annotation;
    }

    @Override
    public Type type() {
        return Type.POST_CONSTRUCTION;
    }

    @Override
    public Class<MCRPostConstruction> annotationClass() {
        return MCRPostConstruction.class;
    }

    @Override
    public int order() {
        return annotation.order();
    }

    @Override
    public Set<MCRTarget.Type> allowedTargetTypes() {
        return MCRTarget.Types.METHOD;
    }

    @Override
    public Class<?> valueClass() {
        return String.class;
    }

    @Override
    public String get(MCRInstanceConfiguration<?> configuration, MCRTarget target) {
        return switch (annotation.value()) {
            case ACTUAL -> configuration.name().actual();
            case CANONICAL -> configuration.name().canonical();
            case TAILING_NAME -> lastComponent(configuration.name().canonical());
        };
    }

    private String lastComponent(String name) {
        return name.substring(name.lastIndexOf('.') + 1);
    }

}
