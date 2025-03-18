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

package org.mycore.common;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Optional;

public class MCRClassTools {
    private static volatile ClassLoader extendedClassLoader;

    static {
        updateClassLoader(); //first init
    }

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

    /**
     * Loads a class via default ClassLoader or <code>Thread.currentThread().getContextClassLoader()</code>.
     * @param classname Name of class
     * @param <T> Type of Class
     * @return the initialized class
     * @throws ClassNotFoundException if both ClassLoader cannot load the Class
     */
    public static <T> Class<? extends T> forName(String classname) throws ClassNotFoundException {
        @SuppressWarnings("unchecked")
        Class<? extends T> forName;
        try {
            forName = (Class<? extends T>) Class.forName(classname);
        } catch (ClassNotFoundException cnfe) {
            forName = (Class<? extends T>) Class.forName(classname, true, extendedClassLoader);
        }
        return forName;
    }

    /**
     * @return a ClassLoader that should be used to load resources
     */
    public static ClassLoader getClassLoader() {
        return extendedClassLoader;
    }

    public static void updateClassLoader() {
        extendedClassLoader = Optional.ofNullable(Thread.currentThread().getContextClassLoader())
            .orElseGet(MCRClassTools.class::getClassLoader);
    }

    /**
     * This is a helper function for top most classes in class hierarchies that implement
     * {@link Cloneable} and expect every subclass to implement a proper {@link Object#clone()}
     * method, i.e. to never throw a {@link CloneNotSupportedException}.
     * <p>
     * It is meant to simplify and harmonize suppression of this exception in the method
     * signatures of the clone-method of such classes and needs to be called as
     * <code>MCRClassTools.clone(getClass(), super::clone)</code> exactly.
     *
     * @param cloneClass the class to be cloned
     * @param superClone a method reference to the super classes clone-methode
     */
    @SuppressWarnings("unchecked")
    public static <T> T clone(Class<T> cloneClass, SuperClone<T> superClone) {
        try {
            return (T) superClone.getClone();
        } catch (CloneNotSupportedException e) {
            throw new MCRException(cloneClass.getName() + " doesn't implement a proper clone-method"
                + " - this is an implementation mistake", e);
        }
    }

    public interface SuperClone<T> {
        Object getClone() throws CloneNotSupportedException;
    }

}
