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

import static org.mycore.common.config.MCRRuntimeComponentDetector.ComponentOrder.HIGHEST_PRIORITY_FIRST;

import java.util.Optional;
import java.util.SortedSet;

import org.mycore.common.config.MCRComponent;
import org.mycore.common.config.MCRRuntimeComponentDetector;
import org.mycore.common.hint.MCRHint;
import org.mycore.common.hint.MCRHintKey;

/**
 * A {@link MCRComponentsResourceHint} is a {@link MCRHint} for {@link MCRResourceHintKeys#COMPONENTS}
 * that uses {@link MCRRuntimeComponentDetector#getAllComponents(MCRRuntimeComponentDetector.ComponentOrder)}
 * to obtain a sorted list of components.
 * <p>
 * No configuration options are available.
 * <p>
 * Example:
 * <pre><code>
 * [...].Class=org.mycore.resource.hint.MCRComponentsResourceHint
 * </code></pre>
 */
public final class MCRComponentsResourceHint implements MCRHint<SortedSet<MCRComponent>> {

    @Override
    public MCRHintKey<SortedSet<MCRComponent>> key() {
        return MCRResourceHintKeys.COMPONENTS;
    }

    @Override
    public Optional<SortedSet<MCRComponent>> value() {
        return Optional.ofNullable(MCRRuntimeComponentDetector.getAllComponents(HIGHEST_PRIORITY_FIRST));
    }

}
