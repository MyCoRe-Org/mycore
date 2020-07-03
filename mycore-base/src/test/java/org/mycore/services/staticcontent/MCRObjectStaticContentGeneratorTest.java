package org.mycore.services.staticcontent;

import java.nio.file.Paths;

import org.junit.Assert;
import org.junit.Test;
import org.mycore.common.MCRTestCase;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.datamodel.metadata.MCRObjectID;

public class MCRObjectStaticContentGeneratorTest extends MCRTestCase {

    @Test
    public void getSlotDirPath() {

        final MCRObjectStaticContentGenerator generator = new MCRObjectStaticContentGenerator(
            null, Paths.get("/"));

        MCRConfiguration2.set("MCR.Metadata.ObjectID.NumberPattern", "00000");
        MCRObjectID derivate = MCRObjectID.getInstance("mcr_derivate_00001");
        Assert.assertEquals("Paths should match", generator.getSlotDirPath(derivate).toString(), "/000/01");

        MCRConfiguration2.set("MCR.Metadata.ObjectID.NumberPattern", "000000");
        derivate = MCRObjectID.getInstance("mcr_derivate_000001");
        Assert.assertEquals("Paths should match", generator.getSlotDirPath(derivate).toString(), "/000/001");

        MCRConfiguration2.set("MCR.Metadata.ObjectID.NumberPattern", "0000000");
        derivate = MCRObjectID.getInstance("mcr_derivate_0000001");
        Assert.assertEquals("Paths should match", generator.getSlotDirPath(derivate).toString(), "/000/000/1");

    }
}
