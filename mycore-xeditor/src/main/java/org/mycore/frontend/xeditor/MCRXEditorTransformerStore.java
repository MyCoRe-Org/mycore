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

package org.mycore.frontend.xeditor;

import org.mycore.common.MCRCache;
import org.mycore.common.MCRSessionMgr;

/**
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRXEditorTransformerStore {

    private static MCRCache<String, MCRXEditorTransformer> cache = new MCRCache<String, MCRXEditorTransformer>(100,
        "XEditorTransformers");

    public static String storeTransformer(MCRXEditorTransformer transformer) {
        String key = MCRSessionMgr.getCurrentSession().getID() + "-" + String.valueOf(System.nanoTime());
        cache.put(key, transformer);
        return key;
    }

    public static MCRXEditorTransformer getAndRemoveTransformer(String key) {
        MCRXEditorTransformer transformer = cache.get(key);
        cache.remove(key);
        return transformer;
    }
}
