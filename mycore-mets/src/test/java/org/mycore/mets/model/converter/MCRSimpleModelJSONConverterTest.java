package org.mycore.mets.model.converter;

import org.junit.Before;
import org.junit.Test;
import org.mycore.mets.model.simple.MCRMetsSimpleModel;

public class MCRSimpleModelJSONConverterTest {

    private MCRMetsSimpleModel metsSimpleModel;

    @Before
    public void buildModel() {
        metsSimpleModel = MCRMetsTestUtil.buildMetsSimpleModel();
    }

    @Test
    public void testToJSON() throws Exception {
        String resultJson = MCRSimpleModelJSONConverter.toJSON(metsSimpleModel);
    }
}
