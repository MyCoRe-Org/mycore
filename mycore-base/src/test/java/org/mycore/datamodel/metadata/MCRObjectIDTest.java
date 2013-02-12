package org.mycore.datamodel.metadata;

import static org.junit.Assert.assertEquals;

import java.util.Date;

import org.jdom2.Document;
import org.jdom2.Element;
import org.junit.Before;
import org.junit.Test;
import org.mycore.common.MCRStoreTestCase;
import org.mycore.datamodel.common.MCRXMLMetadataManager;

public class MCRObjectIDTest extends MCRStoreTestCase {

    private static final String BASE_ID = "MyCoRe_test";

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        setProperty("MCR.Metadata.Type.test", Boolean.TRUE.toString(), true);
    }

    @Test
    public void setNextFreeIdString() {
        MCRObjectID id1 = MCRObjectID.getNextFreeId(BASE_ID);
        assertEquals("First id should be int 1", 1, id1.getNumberAsInteger());
        MCRObjectID id2 = MCRObjectID.getNextFreeId(BASE_ID);
        assertEquals("Second id should be int 2", 2, id2.getNumberAsInteger());
        MCRXMLMetadataManager.instance().create(id2, new Document(new Element("test")), new Date());
        MCRObjectID id3 = MCRObjectID.getNextFreeId(BASE_ID);
        assertEquals("Second id should be int 3", 3, id3.getNumberAsInteger());
    }

}
