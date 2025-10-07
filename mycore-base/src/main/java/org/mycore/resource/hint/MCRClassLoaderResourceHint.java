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

package org.mycore.resource.hint;

import java.util.Optional;

import org.mycore.common.MCRClassTools;
import org.mycore.common.hint.MCRHint;
import org.mycore.common.hint.MCRHintKey;

/**
 * A {@link MCRClassLoaderResourceHint} is a {@link MCRHint} for {@link MCRResourceHintKeys#CLASS_LOADER}
 * that uses {@link MCRClassTools#getClassLoader()}to obtain a {@link ClassLoader}.
 * <p>
 * No configuration options are available.
 * <p>
 * Example:
 * <pre><code>
 * [...].Class=org.mycore.resource.hint.MCRClassLoaderResourceHint
 * </code></pre>
 */
public final class MCRClassLoaderResourceHint implements MCRHint<ClassLoader> {

    @Override
    public MCRHintKey<ClassLoader> key() {
        return MCRResourceHintKeys.CLASS_LOADER;
    }

    @Override
    public Optional<ClassLoader> value() {
        return Optional.ofNullable(MCRClassTools.getClassLoader());
    }

}
