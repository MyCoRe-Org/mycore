package org.mycore.mets.model;

import static org.junit.Assert.assertTrue;

import java.util.Set;

import org.junit.Test;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.datamodel.ifs.MCRDirectory;
import org.mycore.datamodel.ifs.MCRFilesystemNode;

public class METSGeneratorTest {

    @Test
    public void getGenerator() throws Exception {
        // prepare config
        MCRConfiguration.instance().set("MCR.Component.MetsMods.generator", TestGenerator.class.getName());
        // check getGenerator
        MCRMETSGenerator gen = MCRMETSGenerator.getGenerator();
        assertTrue(gen instanceof TestGenerator);
    }

    public static class TestGenerator extends MCRMETSGenerator {
        @Override
        public Mets getMETS(MCRDirectory dir, Set<MCRFilesystemNode> ignoreNodes) {
            return new Mets();
        }
    }
}
