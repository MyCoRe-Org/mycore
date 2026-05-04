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
import java.lang.reflect.Method;
import java.util.Optional;

import org.mycore.common.config.instantiator.source.MCRSource;
import org.mycore.common.config.instantiator.source.MCRSourceType;
import org.mycore.common.config.instantiator.target.MCRTarget;

/**
 * Common abstraction for components of a class (i.e., a {@link Field} or a {@link Method}) that can,
 * in principle, be used for injection. It provides abstractions for obtaining a value ({@link MCRSource})
 * based on present annotations, and injecting obtained values ({@link MCRTarget}).
 */
public interface MCRInjectable {

    Optional<MCRSource> toSource(MCRSourceType sourceType);

    MCRTarget toTarget();

}
