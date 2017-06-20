/*
 * $Id$
 * $Revision: 5697 $ $Date: Feb 19, 2013 $
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

package org.mycore.frontend.jersey.filter.access;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRResourceAccessCheckerFactory {
    private static ConcurrentHashMap<Class<? extends MCRResourceAccessChecker>, MCRResourceAccessChecker> implMap = new ConcurrentHashMap<>();

    public static <T extends MCRResourceAccessChecker> T getInstance(Class<T> clazz)
        throws InstantiationException, IllegalAccessException {
        @SuppressWarnings("unchecked")
        T accessChecker = (T) implMap.get(clazz);
        if (accessChecker != null) {
            return accessChecker;
        }
        accessChecker = clazz.newInstance();
        implMap.put(clazz, accessChecker);
        return accessChecker;
    }

}
