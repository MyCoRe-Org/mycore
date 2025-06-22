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

package org.mycore.common.content.transformer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.mycore.common.config.MCRConfiguration2;

/**
 * Creates and returns MCRContentTransformer instances by their ID.
 *
 * @author Frank Lützenkirchen
 */
public class MCRContentTransformerFactory {

    /** Map of transformer instances by ID */
    private static Map<String, MCRContentTransformer> transformers = new ConcurrentHashMap<>();

    /**
     * Returns the transformer with the given ID. If the transformer is not instantiated yet,
     * it is created and initialized.
     */
    public static MCRContentTransformer getTransformer(String id) {
        if (transformers.containsKey(id)) {
            return transformers.get(id);
        } else {
            return buildTransformer(id);
        }
    }

    /**
     * Creates and initializes the transformer with the given ID.
     */
    private static synchronized MCRContentTransformer buildTransformer(String id) {
        String property = "MCR.ContentTransformer." + id + ".Class";
        if (MCRConfiguration2.getString(property).isEmpty()
            && MCRConfiguration2.getString("MCR.ContentTransformer." + id + ".Stylesheet").isEmpty()) {
            //check for reasonable default:
            return null;
        }
        MCRContentTransformer transformer = MCRConfiguration2.getInstanceOf(MCRContentTransformer.class, property)
            .orElseGet(() -> MCRConfiguration2.getInstanceOfOrThrow(MCRContentTransformer.class,
                "MCR.ContentTransformer.Default.Class"));
        transformer.init(id);
        transformers.put(id, transformer);
        return transformer;
    }
}
