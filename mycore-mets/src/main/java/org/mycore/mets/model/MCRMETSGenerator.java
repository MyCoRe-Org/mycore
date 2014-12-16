package org.mycore.mets.model;

import java.io.IOException;
import java.util.Set;

import org.mycore.common.config.MCRConfiguration;
import org.mycore.datamodel.niofs.MCRPath;

public abstract class MCRMETSGenerator {

    private static Class<? extends MCRMETSGenerator> metsGeneratorClass = null;

    public static synchronized MCRMETSGenerator getGenerator() throws ClassNotFoundException, IllegalAccessException,
        InstantiationException {
        if (metsGeneratorClass == null) {
            String cn = MCRConfiguration.instance().getString("MCR.Component.MetsMods.generator",
                MCRMETSDefaultGenerator.class.getName());
            Class<?> classToCheck = Class.forName(cn);
            metsGeneratorClass = classToCheck.asSubclass(MCRMETSGenerator.class);
        }
        return metsGeneratorClass.newInstance();
    }

    public abstract Mets getMETS(MCRPath dir, Set<MCRPath> ignoreNodes) throws IOException;

}
