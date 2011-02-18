package org.mycore.mets.model;

import static org.junit.Assert.assertTrue;

import java.util.Set;

import org.jdom.Document;
import org.junit.Test;
import org.mycore.common.MCRConfiguration;
import org.mycore.datamodel.ifs.MCRDirectory;
import org.mycore.datamodel.ifs.MCRFilesystemNode;

public class METSGeneratorTest {

    @Test
    public void getGenerator() throws Exception {
        // prepare config
        MCRConfiguration.instance().set("MCR.Component.MetsMods.generator", TestGenerator.class.getName());
        // check getGenerator
        MCRMETSGenerator gen = MCRMETSGenerator.getGenerator();
        if(gen instanceof TestGenerator) {
            assertTrue(true);
        } else {
            assertTrue(false);
        }
    }

    public static class TestGenerator extends MCRMETSGenerator {
        @Override
        public Document getMETS(MCRDirectory dir, Set<MCRFilesystemNode> ignoreNodes) {
            return new Document();
        }
    }
}
