/*
 * 
 * $Revision: 18725 $ $Date: 2010-09-21 11:21:23 +0200 (Di, 21 Sep 2010) $
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

package org.mycore.datamodel.metadata;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * holds weak references to generated {@link MCRObjectID} instances.
 * @author Thomas Scheffler (yagee)
 *
 */
class MCRObjectIDPool {
    private static Map<String, WeakReference<MCRObjectID>> map = Collections
        .synchronizedMap(new WeakHashMap<String, WeakReference<MCRObjectID>>());

    static MCRObjectID getMCRObjectID(String id) {
        WeakReference<MCRObjectID> ref = map.get(id);
        if (ref != null) {
            MCRObjectID mcrId = ref.get();
            if (mcrId != null) {
                return mcrId;
            }
        }
        //does not exist (anymore)
        MCRObjectID mcrId = new MCRObjectID(id);
        map.put(mcrId.toString(), new WeakReference<MCRObjectID>(mcrId));
        return mcrId;
    }

    static int getSize() {
        return map.size();
    }
}
