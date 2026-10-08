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

package org.mycore.dedup;

import java.util.Optional;

import org.mycore.datamodel.metadata.MCRObject;

/**
 * Resolves the display title of an {@link MCRObject} that is stored alongside its deduplication keys and
 * shown in the deduplication API next to the object id. The title is computed once when the
 * deduplication keys of an object are (re)built, so that the possible-duplicate lists can be rendered
 * without loading the full object metadata per row.
 * <p>
 * Which resolver is applied to which object type is configured per type via properties of the form
 * {@code MCR.DeDup.TitleResolver.<typeId>.Class}, see {@link MCRDeDupTitleProvider}. The generic
 * {@link MCRDeDupLabelTitleResolver} returns the object label; metadata models with a richer structure
 * contribute a resolver that extracts a more meaningful title.
 * <p>
 * Implementations must be robust against objects they cannot handle and return an empty
 * {@link Optional} in that case instead of throwing an exception.
 */
public interface MCRDeDupTitleResolver {

    /**
     * Resolves the display title of the given object.
     *
     * @param object the object to resolve the title for
     * @return the title, or an empty {@link Optional} if no title can be resolved
     */
    Optional<String> resolveTitle(MCRObject object);
}
