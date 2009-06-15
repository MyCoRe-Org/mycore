package org.mycore.frontend.redundancy;

import org.mycore.common.MCRConfiguration;

public abstract class MCRRedundancyUtil {

    private static final String FS = System.getProperty("file.seperator", "/");
    public static final String DIR = MCRConfiguration.instance().getString("MCR.doubletFinder.path") + FS;

    public static MCRRedundancyAbstractMapGenerator getMapGenerator(String alias)
        throws ClassNotFoundException, IllegalAccessException, InstantiationException {

        // try to resolve the generator
        Class<?> redunGenClass = resolveAlias(alias);
        return (MCRRedundancyAbstractMapGenerator)redunGenClass.newInstance();
    }

    protected static Class<?> resolveAlias(String alias) throws ClassNotFoundException {
        String classPath = MCRConfiguration.instance().getString("MCR.doubletFinder.mapGenerator." + alias + ".class");
        return Class.forName(classPath);
    }

}