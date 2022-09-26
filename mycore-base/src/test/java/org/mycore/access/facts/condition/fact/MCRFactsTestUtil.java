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

package org.mycore.access.facts.condition.fact;

import java.lang.reflect.Field;

import org.mycore.access.facts.MCRObjectCacheFactory;
import org.mycore.common.MCRCache;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;

public class MCRFactsTestUtil {

    public static void hackObjectIntoCache(MCRObject object, MCRObjectID testId) throws NoSuchFieldException,
        IllegalAccessException {
        MCRObjectCacheFactory instance = MCRObjectCacheFactory.instance();
        Field objectCacheField = instance.getClass().getDeclaredField("objectCache");
        objectCacheField.setAccessible(true);
        MCRCache<MCRObjectID, MCRObject> o = (MCRCache<MCRObjectID, MCRObject>) objectCacheField.get(instance);
        objectCacheField.setAccessible(false);
        o.put(testId, object);

    }
}
