/*
 * $Revision$ 
 * $Date$
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */

package org.mycore.common.content.transformer;

import java.util.HashMap;

import org.mycore.common.config.MCRConfiguration;

/**
 * Creates and returns MCRContentTransformer instances by their ID.
 * 
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRContentTransformerFactory {

    /** Map of transformer instances by ID */
    private static HashMap<String, MCRContentTransformer> transformers = new HashMap<String, MCRContentTransformer>();

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
        MCRConfiguration config = MCRConfiguration.instance();

        if (config.getString(property, null) == null) {
            //check for reasonable default:
            String stylesheets = config.getString("MCR.ContentTransformer." + id + ".Stylesheet", null);
            if (stylesheets == null) {
                return null;
            }
        }
        MCRContentTransformer transformer = config.getInstanceOf(property, MCRXSLTransformer.class.getCanonicalName());
        transformer.init(id);
        transformers.put(id, transformer);
        return transformer;
    }
}
