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
package org.mycore.access.facts.condition.combined;

import org.mycore.access.facts.MCRFactsHolder;

/**
 * This condition negates its child condition
 * (boolean NOT)
 * <p>
 * Only the first child condition will be evaluated.
 * Further child conditions will be ignored.
 *
 * @author Robert Stephan
 *
 */
public final class MCRNotCondition extends MCRAbstractCombinedCondition {

    @Override
    public boolean matches(MCRFactsHolder facts) {
        return conditions.stream()
            .limit(1)
            .filter(c -> !addDebugInfoIfRequested(c, facts))
            .count() == 1;
    }
}
