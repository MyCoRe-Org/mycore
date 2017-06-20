package org.mycore.common;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

public class MCRClassTools {
    public static Object loadClassFromURL(String classPath, String className)
        throws MalformedURLException, ClassNotFoundException, InstantiationException, IllegalAccessException {

        return loadClassFromURL(new File(classPath), className);
    }

    public static Object loadClassFromURL(File file, String className)
        throws MalformedURLException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        if (file.exists()) {
            URL url = file.toURI().toURL();
            URLClassLoader urlClassLoader = new URLClassLoader(new URL[] { url },
                Thread.currentThread().getContextClassLoader());
            Class<?> clazz = urlClassLoader.loadClass(className);
            return clazz.newInstance();
        }

        return null;
    }
}
