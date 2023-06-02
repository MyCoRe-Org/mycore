package org.mycore.mods.csl;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.junit.Before;
import org.mycore.common.MCRException;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRStoreTestCase;
import org.mycore.common.MCRSystemUserInformation;
import org.mycore.common.content.MCRByteContent;
import org.mycore.common.content.MCRContent;
import org.mycore.datamodel.common.MCRXMLMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;

public class MCRMODSCSLTest extends MCRStoreTestCase {

    private static final String TEST_ID_1 = "junit_mods_00002050";
    private static final String TEST_ID_2 = "junit_mods_00002056";
    private static final String TEST_ID_3 = "junit_mods_00002489";
    private static final String TEST_ID_4 = "junit_mods_00002052";

    protected static String getIDFromContent(MCRContent c) {
        try {
            return c.asXML().getRootElement().getAttributeValue("ID");
        } catch (JDOMException | IOException e) {
            throw new MCRException(e);
        }
    }

    protected List<MCRContent> getTestContent() throws IOException {
        return Stream.of(loadContent(TEST_ID_1), loadContent(TEST_ID_2), loadContent(TEST_ID_3), loadContent(TEST_ID_4))
            .collect(Collectors.toList());
    }

    @Before
    public void setUp() throws Exception {
        super.setUp();
        MCRSessionMgr.getCurrentSession().setUserInformation(MCRSystemUserInformation.getSuperUserInstance());
        List<MCRContent> testContent = getTestContent();
        for (MCRContent content : testContent) {
            Document jdom = content.asXML();
            MCRXMLMetadataManager.instance().create(MCRObjectID.getInstance(getIDFromContent(content)), jdom,
                new Date());
        }
    }

    protected MCRContent loadContent(String id) throws IOException {

        try (InputStream is
            = MCRMODSCSLTest.class.getClassLoader().getResourceAsStream("MCRMODSCSLTest/" + id + ".xml")) {
            byte[] bytes = is.readAllBytes();
            return new MCRByteContent(bytes);
        }
    }

}
