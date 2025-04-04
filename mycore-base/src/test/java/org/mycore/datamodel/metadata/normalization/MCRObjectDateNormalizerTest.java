package org.mycore.datamodel.metadata.normalization;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Date;

import org.junit.Test;
import org.mycore.common.MCRTestCase;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectService;

public class MCRObjectDateNormalizerTest extends MCRTestCase {

    @Test
    public void testNormalize() throws Exception {
        MCRObject mcrObject = new MCRObject();
        MCRObjectDateNormalizer normalizer = new MCRObjectDateNormalizer();
        normalizer.normalize(mcrObject);

        assertNotNull(mcrObject.getService().getDate(MCRObjectService.DATE_TYPE_CREATEDATE));
        assertNotNull(mcrObject.getService().getDate(MCRObjectService.DATE_TYPE_MODIFYDATE));

        // test with existing dates
        Date date = new Date();

        date.setTime(0);
        mcrObject.getService().setDate(MCRObjectService.DATE_TYPE_CREATEDATE, date);
        mcrObject.getService().setDate(MCRObjectService.DATE_TYPE_MODIFYDATE, date);
        normalizer.normalize(mcrObject);

        assertEquals(mcrObject.getService().getDate(MCRObjectService.DATE_TYPE_CREATEDATE), date);
        assertEquals(mcrObject.getService().getDate(MCRObjectService.DATE_TYPE_MODIFYDATE), date);
    }

}
