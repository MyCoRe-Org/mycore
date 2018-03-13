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

package org.mycore.frontend.jersey.filter.access;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRResourceAccessCheckerFactory {
    private static ConcurrentHashMap<Class<? extends MCRResourceAccessChecker>, MCRResourceAccessChecker> implMap = new ConcurrentHashMap<>();

    public static <T extends MCRResourceAccessChecker> T getInstance(Class<T> clazz)
        throws ReflectiveOperationException {
        @SuppressWarnings("unchecked")
        T accessChecker = (T) implMap.get(clazz);
        if (accessChecker != null) {
            return accessChecker;
        }
        accessChecker = clazz.getDeclaredConstructor().newInstance();
        implMap.put(clazz, accessChecker);
        return accessChecker;
    }

}
