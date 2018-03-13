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

package org.mycore.common;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

public class MCRClassTools {
    public static Object loadClassFromURL(String classPath, String className)
        throws MalformedURLException, ReflectiveOperationException {
        return loadClassFromURL(new File(classPath), className);
    }

    public static Object loadClassFromURL(File file, String className)
        throws MalformedURLException, ReflectiveOperationException {
        if (file.exists()) {
            URL url = file.toURI().toURL();
            URLClassLoader urlClassLoader = new URLClassLoader(new URL[] { url },
                Thread.currentThread().getContextClassLoader());
            Class<?> clazz = urlClassLoader.loadClass(className);
            return clazz.getDeclaredConstructor().newInstance();
        }

        return null;
    }
}
