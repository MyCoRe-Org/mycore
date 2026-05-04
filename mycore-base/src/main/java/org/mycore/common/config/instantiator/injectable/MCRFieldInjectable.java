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

package org.mycore.common.config.instantiator.injectable;

import java.lang.reflect.Field;
import java.util.Optional;

import org.mycore.common.config.instantiator.source.MCRSource;
import org.mycore.common.config.instantiator.source.MCRSourceType;
import org.mycore.common.config.instantiator.target.MCRFieldTarget;
import org.mycore.common.config.instantiator.target.MCRTarget;

/**
 * A {@link MCRFieldInjectable} is a {@link MCRFieldInjectable} that abstracts a {@link Field}.
 */
public final class MCRFieldInjectable implements MCRInjectable {

    private final Field field;

    public MCRFieldInjectable(Field field) {
        this.field = field;
    }

    @Override
    public Optional<MCRSource> toSource(MCRSourceType sourceType) {
        return sourceType.toSource(field::getAnnotation);
    }

    @Override
    public MCRTarget toTarget() {
        return new MCRFieldTarget(field);
    }

}
