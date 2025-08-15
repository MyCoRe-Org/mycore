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
package org.mycore.datamodel.classifications2.mapping;

import java.util.Collections;
import java.util.Set;

import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.classifications2.mapping.MCRXMappingClassificationGeneratorBase.Evaluator;
import org.mycore.datamodel.metadata.MCRObject;

/**
 * A {@link MCRNoOpXMappingEvaluator} is a {@link Evaluator} that returns an empty set of category IDs.
 * <p>
 * The returned classification IDs are the same for all MyCoRe objects.
 * <p>
 * No configuration options are available.
 * Example:
 * <pre><code>
 * [...].Class=org.mycore.resource.filter.MCRNoOpResourceFilter
 * </code></pre>
 */
public final class MCRNoOpXMappingEvaluator implements Evaluator {

    @Override
    public Set<MCRCategoryID> getCategoryIds(String mapping, MCRObject object) {
        return Collections.emptySet();
    }

}
