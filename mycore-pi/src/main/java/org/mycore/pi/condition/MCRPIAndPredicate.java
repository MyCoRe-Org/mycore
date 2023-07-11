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

package org.mycore.pi.condition;

import java.util.Collections;
import java.util.List;

import org.mycore.common.config.annotation.MCRInstanceList;
import org.mycore.datamodel.metadata.MCRBase;

public final class MCRPIAndPredicate extends MCRPIPredicateBase {

    @MCRInstanceList(valueClass = MCRPIPredicate.class)
    public List<MCRPIPredicate> predicates = Collections.emptyList();

    @Override
    public boolean test(MCRBase mcrBase) {
        return predicates.stream().allMatch(predicate -> predicate.test(mcrBase));
    }

}
