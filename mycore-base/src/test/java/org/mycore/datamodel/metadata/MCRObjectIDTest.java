package org.mycore.datamodel.metadata;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.jdom2.Document;
import org.jdom2.Element;
import org.junit.Before;
import org.junit.Test;
import org.mycore.common.MCRStoreTestCase;

public class MCRObjectIDTest extends MCRStoreTestCase {

    private static final String BASE_ID = "MyCoRe_test";

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    @Test
    public void setNextFreeIdString() throws IOException {
        MCRObjectID id1 = MCRObjectID.getNextFreeId(BASE_ID);
        assertEquals("First id should be int 1", 1, id1.getNumberAsInteger());
        MCRObjectID id2 = MCRObjectID.getNextFreeId(BASE_ID);
        assertEquals("Second id should be int 2", 2, id2.getNumberAsInteger());
        getStore().create(id2, new Document(new Element("test")), new Date());
        MCRObjectID id3 = MCRObjectID.getNextFreeId(BASE_ID);
        assertEquals("Second id should be int 3", 3, id3.getNumberAsInteger());
    }

    @Test
    public void compareTo() {
        Set<MCRObjectID> testIds = IntStream.range(0, 17)
            .mapToObj(i -> MCRObjectID.getInstance(MCRObjectID.formatID("MyCoRe", "test", i)))
            .flatMap(o -> Stream.of(o, MCRObjectID.getInstance(
                MCRObjectID.formatID(o.getProjectId(), "junit", o.getNumberAsInteger()))))
            .flatMap(o -> Stream.concat(Stream.of(o),
                Stream.of("junit", "mcr", "JUnit")
                    .map(projectId -> MCRObjectID
                        .getInstance(
                            MCRObjectID.formatID(
                                projectId,
                                o.getTypeId(),
                                o.getNumberAsInteger())))))
            .collect(Collectors.toSet());
        ArrayList<MCRObjectID> first = new ArrayList<>(testIds);
        ArrayList<MCRObjectID> test = new ArrayList<>(testIds);
        first.sort((f, o) -> f.toString().compareTo(o.toString()));
        test.sort(MCRObjectID::compareTo);
        assertArrayEquals("Order should be the same.", first.toArray(), test.toArray());
    }

    @Override
    protected Map<String, String> getTestProperties() {
        Map<String, String> testProperties = super.getTestProperties();
        testProperties.put("MCR.Metadata.Type.test", Boolean.TRUE.toString());
        testProperties.put("MCR.Metadata.Type.junit", Boolean.TRUE.toString());
        return testProperties;
    }

}
