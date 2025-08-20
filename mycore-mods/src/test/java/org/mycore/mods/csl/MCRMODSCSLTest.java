/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mycore.mods.csl;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.junit.jupiter.api.BeforeEach;
import org.mycore.common.MCRException;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRSystemUserInformation;
import org.mycore.common.content.MCRByteContent;
import org.mycore.common.content.MCRContent;
import org.mycore.datamodel.common.MCRXMLMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.test.MyCoReTest;

public abstract class MCRMODSCSLTest {

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

    @BeforeEach
    public void setUp() throws Exception {
        MCRSessionMgr.getCurrentSession().setUserInformation(MCRSystemUserInformation.SUPER_USER);
        List<MCRContent> testContent = getTestContent();
        for (MCRContent content : testContent) {
            Document jdom = content.asXML();
            MCRXMLMetadataManager.getInstance().create(MCRObjectID.getInstance(getIDFromContent(content)), jdom,
                new Date());
        }
    }

    protected MCRContent loadContent(String id) throws IOException {

        try (InputStream is =
            MCRMODSCSLTest.class.getClassLoader().getResourceAsStream("MCRMODSCSLTest/" + id + ".xml")) {
            byte[] bytes = is.readAllBytes();
            return new MCRByteContent(bytes);
        }
    }

}
