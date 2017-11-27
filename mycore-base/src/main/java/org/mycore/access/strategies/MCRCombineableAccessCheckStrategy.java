/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
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

package org.mycore.access.strategies;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public interface MCRCombineableAccessCheckStrategy extends MCRAccessCheckStrategy {

    /**
     * Checks if this strategy has a rule mapping defined.
     * Can be used by other more complex strategies that require this information to decide if this strategy should be used.
     * @param id 
     *              a possible MCRObjectID of the object or any other "id"
     * @param permission
     *              the access permission for the rule
     * @return true if there is a mapping to a rule defined
     */
    boolean hasRuleMapping(String id, String permission);

}
