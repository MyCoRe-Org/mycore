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

import org.mycore.common.events.MCRServletContextHolder;
import org.mycore.common.hint.MCRHint;
import org.mycore.common.hint.MCRHintKey;

import jakarta.servlet.ServletContext;

/**
 * A {@link MCRServletContextResourceHint} is a {@link MCRHint} for {@link MCRResourceHintKeys#SERVLET_CONTEXT}
 * that uses {@link MCRServletContextHolder} to obtain a {@link ServletContext}.
 * <p>
 * No configuration options are available.
 * <p>
 * Example:
 * <pre><code>
 * [...].Class=org.mycore.resource.hint.MCRServletContextResourceHint
 * </code></pre>
 */
public final class MCRServletContextResourceHint implements MCRHint<ServletContext> {

    @Override
    public MCRHintKey<ServletContext> key() {
        return MCRResourceHintKeys.SERVLET_CONTEXT;
    }

    @Override
    public Optional<ServletContext> value() {
        return MCRServletContextHolder.getInstance().get();
    }

}
