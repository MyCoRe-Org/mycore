package org.mycore.mets.model;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.mycore.common.config.MCRConfiguration;

public class MCRMETSGeneratorFactoryTest {

    @Test
    public void getGenerator() throws Exception {
        // prepare config
        MCRConfiguration.instance().set("MCR.Component.MetsMods.Generator", TestGenerator.class.getName());
        // check getGenerator
        MCRMETSGenerator generator = MCRMETSGeneratorFactory.create(null);
        assertTrue(generator instanceof TestGenerator);
    }

    public static class TestGenerator implements MCRMETSGenerator {
        @Override
        public Mets generate() {
            return new Mets();
        }
    }

}
