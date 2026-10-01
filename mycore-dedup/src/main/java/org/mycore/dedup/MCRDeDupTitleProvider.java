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

import org.mycore.common.config.MCRConfiguration2;
import org.mycore.datamodel.metadata.MCRObject;

/**
 * Resolves the display title of an {@link MCRObject} by dispatching to the {@link MCRDeDupTitleResolver}
 * configured for the object's type. The title is stored alongside the deduplication keys and shown in
 * the deduplication API next to the object id, so that possible-duplicate lists can be rendered without
 * loading the full object metadata per row.
 * <p>
 * For an object of type {@code <typeId>} the resolver configured via
 * <pre>
 * MCR.DeDup.TitleResolver.&lt;typeId&gt;.Class=org.example.MyTitleResolver
 * </pre>
 * is used. This makes title resolution configurable per object type, exactly like the deduplication
 * criteria (see {@link MCRDeDupCriteriaProvider}): a metadata model contributes a resolver that knows
 * how to extract a meaningful title from its objects. Types without a configured resolver produce no
 * title.
 * <p>
 * The provider itself is configurable through {@code MCR.DeDup.TitleProvider.Class} so that applications
 * may replace the default dispatch strategy.
 */
public class MCRDeDupTitleProvider {

    /**
     * Prefix of the properties configuring the {@link MCRDeDupTitleResolver} per object type. The object
     * type id is appended, followed by {@code .Class}.
     */
    public static final String RESOLVER_PROPERTY_PREFIX = "MCR.DeDup.TitleResolver.";

    /**
     * @return the configured instance of the deduplication title provider
     */
    public static MCRDeDupTitleProvider obtainInstance() {
        return MCRConfiguration2.getSingleInstanceOfOrThrow(MCRDeDupTitleProvider.class,
            "MCR.DeDup.TitleProvider.Class");
    }

    /**
     * Resolves the display title of the given object using the {@link MCRDeDupTitleResolver} configured
     * for its type.
     *
     * @param object the object to resolve the title for
     * @return the title, or an empty {@link Optional} if no resolver is configured for the object's type
     *     or the resolver yields no title
     */
    public Optional<String> resolveTitle(MCRObject object) {
        return getTitleResolver(object.getId().getTypeId())
            .flatMap(resolver -> resolver.resolveTitle(object));
    }

    /**
     * Returns the {@link MCRDeDupTitleResolver} configured for the given object type, if any.
     *
     * @param typeId the object type id, e.g. {@code mods}
     * @return the configured title resolver, or an empty {@link Optional} if none is configured
     */
    public Optional<MCRDeDupTitleResolver> getTitleResolver(String typeId) {
        return MCRConfiguration2.getSingleInstanceOf(MCRDeDupTitleResolver.class,
            RESOLVER_PROPERTY_PREFIX + typeId + ".Class");
    }
}
