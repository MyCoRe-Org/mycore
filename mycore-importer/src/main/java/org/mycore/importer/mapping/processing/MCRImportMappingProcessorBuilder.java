package org.mycore.importer.mapping.processing;

import org.apache.log4j.Logger;

/**
 * This class creates instances of <code>MCRImportMappingProcessor</code>.
 * 
 * @author Matthias Eichner
 */
public abstract class MCRImportMappingProcessorBuilder {

    private static final Logger LOGGER = Logger.getLogger(MCRImportMappingProcessorBuilder.class);

    /**
     * Creates a <code>MCRImportMappingProcessor</code> instance by reflection.
     * 
     * @param className the class name which have to be resolved
     * @return a new instance of <code>MCRImportMappingProcessor</code>
     */
    public static MCRImportMappingProcessor createProcessorInstance(String className) {
        try {
            Class<?> c = Class.forName(className);
            Object o = c.newInstance();
            if(o instanceof MCRImportMappingProcessor)
                return (MCRImportMappingProcessor)o;
            LOGGER.error("The class " + className + " doesnt implement MCRImportMappingProcessor!");
        } catch(Exception e) {
            LOGGER.error(e);
        }
        return null;
    }

}
