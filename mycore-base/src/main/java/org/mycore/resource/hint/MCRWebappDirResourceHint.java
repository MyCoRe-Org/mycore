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

import java.nio.file.Path;
import java.util.Optional;

import org.mycore.common.events.MCRServletContextHolder;
import org.mycore.common.hint.MCRHint;
import org.mycore.common.hint.MCRHintKey;

import jakarta.servlet.ServletContext;

/**
 * A {@link MCRWebappDirResourceHint} is a {@link MCRHint} for {@link MCRResourceHintKeys#WEBAPP_DIR}
 * that uses {@link MCRServletContextHolder} to obtain a {@link ServletContext} and resolve the webapp directory.
 * <p>
 * No configuration options are available.
 * <p>
 * Example:
 * <pre><code>
 * [...].Class=org.mycore.resource.hint.MCRWebappDirResourceHint
 * </code></pre>
 */
public final class MCRWebappDirResourceHint implements MCRHint<Path> {

    @Override
    public MCRHintKey<Path> key() {
        return MCRResourceHintKeys.WEBAPP_DIR;
    }

    @Override
    public Optional<Path> value() {
        return MCRServletContextHolder.getInstance().get().map(context -> context.getRealPath("/")).map(Path::of);
    }

}
