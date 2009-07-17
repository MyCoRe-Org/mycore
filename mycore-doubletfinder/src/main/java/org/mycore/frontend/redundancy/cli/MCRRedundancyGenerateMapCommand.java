package org.mycore.frontend.redundancy.cli;

import org.mycore.frontend.redundancy.MCRRedundancyAbstractMapGenerator;
import org.mycore.frontend.redundancy.MCRRedundancyTypeData;
import org.mycore.frontend.redundancy.MCRRedundancyUtil;

/**
 * Generates a redundancy map for a given type processed by
 * the alias map generator.
 * 
 * @author Matthias Eichner
 */
public class MCRRedundancyGenerateMapCommand {

    public static void generate(String typeAlias, String generatorAlias) throws Exception {
        MCRRedundancyTypeData typeData = new MCRRedundancyTypeData(typeAlias);
        MCRRedundancyAbstractMapGenerator generator = MCRRedundancyUtil.getMapGenerator(generatorAlias);
        generator.setTypeData(typeData);
        generator.createRedundancyMap();
        generator.saveToFile();
    }

}