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

import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.mycore.datamodel.metadata.MCRBase;
import org.mycore.pi.MCRPIJobService;

public abstract class MCRPICombinedPredicate extends MCRPIPredicateBase {

    public MCRPICombinedPredicate(String propertyPrefix) {
        super(propertyPrefix);
    }

    protected Stream<Predicate<MCRBase>> getCombinedPredicates() {
        final Map<String, String> properties = getProperties();
        return properties
            .keySet()
            .stream()
            .filter(p -> {
                return !p.contains("."); // do not handle sub properties
            })
            .map(Integer::parseInt)
            .sorted()
            .map(Object::toString)
            .map((subProperty) -> MCRPIJobService.getPredicateInstance(getPropertyPrefix() + subProperty));
    }
}
