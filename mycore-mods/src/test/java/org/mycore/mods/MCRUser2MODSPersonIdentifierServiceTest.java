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

package org.mycore.mods;

import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mycore.access.MCRAccessBaseImpl;
import org.mycore.access.MCRAccessException;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.common.content.MCRURLContent;
import org.mycore.datamodel.legalentity.MCRIdentifier;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectMetadataTest;
import org.mycore.test.MCRJPAExtension;
import org.mycore.test.MCRMetadataExtension;
import org.mycore.test.MyCoReTest;
import org.mycore.user2.MCRUser;
import org.mycore.user2.MCRUserManager;

import java.io.IOException;
import java.net.URL;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

@MyCoReTest
@ExtendWith({ MCRJPAExtension.class, MCRMetadataExtension.class })
@MCRTestConfiguration(properties = {
    @MCRTestProperty(key = "MCR.Metadata.Type.modsperson", string = "true"),
    @MCRTestProperty(key = "MCR.MODS.Types", string = "mods,modsperson"),
    @MCRTestProperty(key = "MCR.MODS.NewObjectType", string = "mods"),
    @MCRTestProperty(key = "MCR.Access.Class", classNameOf = MCRAccessBaseImpl.class)
})
public class MCRUser2MODSPersonIdentifierServiceTest {

    private static final String ORCID_1 = "0000-0001-2345-6789";

    private static final String ORCID_2 = "0000-0002-3456-7895";

    private static final String ORCID_3 = "0000-0003-4567-8985";

    private static final String SCOPUS = "87654321";

    MCRUser2MODSPersonIdentifierService service;

    MCRUser user;

    @BeforeEach
    public void setUp() throws Exception {
        URL url1 = MCRObjectMetadataTest.class.getResource(
            "/MCRUser2MODSPersonIdentifierServiceTest/junit_modsperson_00000001.xml");
        Document doc1 = new MCRURLContent(url1).asXML();
        MCRObject obj1 = new MCRObject(doc1);
        MCRMetadataManager.create(obj1);

        user = new MCRUser("john");
        user.setRealName("John Doe");
        user.setUserAttribute("id_modsperson", "junit_modsperson_00000001");
        MCRUserManager.createUser(user);

        service = new MCRUser2MODSPersonIdentifierService();
    }

    @Test
    public final void testGetAllIdentifiers() {
        Set<MCRIdentifier> allIdentifiers = service.getAllIdentifiers(
            new MCRIdentifier("userid", user.getUserID()));
        Set<MCRIdentifier> expected = Set.of(new MCRIdentifier("orcid", ORCID_1),
            new MCRIdentifier("orcid", ORCID_2),
            new MCRIdentifier("orcid", ORCID_3),
            new MCRIdentifier("scopus", SCOPUS),
            new MCRIdentifier("something", "abcd"));
        assertEquals(expected, allIdentifiers);
    }

    @Test
    public final void testGetTypedIdentifiers() {
        final MCRIdentifier userid = new MCRIdentifier("userid", user.getUserID());
        Set<MCRIdentifier> typedIdentifiers = service.getTypedIdentifiers(userid,"orcid");
        Set<MCRIdentifier> expected = Set.of(new MCRIdentifier("orcid", ORCID_1),
            new MCRIdentifier("orcid", ORCID_2),
            new MCRIdentifier("orcid", ORCID_3));
        assertEquals(expected, typedIdentifiers);

        typedIdentifiers = service.getTypedIdentifiers(userid,"id_orcid");
        assertEquals(0, typedIdentifiers.size());

        typedIdentifiers = service.getTypedIdentifiers(
            new MCRIdentifier("userid", user.getUserID()),"scopus");
        assertEquals(Set.of(new MCRIdentifier("scopus", SCOPUS)), typedIdentifiers);

        typedIdentifiers = service.getTypedIdentifiers(userid, "something");
        assertEquals(Set.of(new MCRIdentifier("something", "abcd")), typedIdentifiers);
    }

    @Test
    public final void testAddIdentifier() throws MCRAccessException, IOException, JDOMException {
        URL url2 = MCRObjectMetadataTest.class.getResource(
            "/MCRUser2MODSPersonIdentifierServiceTest/junit_modsperson_00000002.xml");
        Document doc2 = new MCRURLContent(url2).asXML();
        MCRObject obj2 = new MCRObject(doc2);
        MCRMetadataManager.create(obj2);

        MCRUser user2 = new MCRUser("jane");
        user2.setRealName("Jane Doe");
        user2.setUserAttribute("id_modsperson", "junit_modsperson_00000002");
        MCRUserManager.createUser(user2);

        final MCRIdentifier userid = new MCRIdentifier("userid", user2.getUserID());
        Set<MCRIdentifier> allIdentifiers = service.getAllIdentifiers(userid);
        assertEquals(2, allIdentifiers.size());

        service.addIdentifier(userid, new MCRIdentifier("orcid", ORCID_1)); // don't add id twice
        allIdentifiers = service.getAllIdentifiers(userid);
        assertEquals(2, allIdentifiers.size());

        service.addIdentifier(userid, new MCRIdentifier("orcid", ORCID_3));
        allIdentifiers = service.getAllIdentifiers(userid);
        assertEquals(3, allIdentifiers.size());
        Set<MCRIdentifier> expected = Set.of(new MCRIdentifier("orcid", ORCID_1),
            new MCRIdentifier("orcid", ORCID_2),
            new MCRIdentifier("orcid", ORCID_3));
        assertEquals(expected, allIdentifiers);
    }

    @Test
    public final void testAddIdentifierNoModsperson() {
        Set<MCRIdentifier> allIdentifiers = service.getAllIdentifiers(new MCRIdentifier("userid", "noname"));
        assertEquals(0, allIdentifiers.size());

        MCRUser user3 = new MCRUser("james");
        user3.setRealName("James Doe");
        user3.setUserAttribute("id_orcid", ORCID_1);
        MCRUserManager.createUser(user3);

        final MCRIdentifier userid = new MCRIdentifier("userid", user3.getUserID());
        allIdentifiers = service.getAllIdentifiers(userid);
        assertEquals(0, allIdentifiers.size());

        service.addIdentifier(userid, new MCRIdentifier("orcid", ORCID_2));
        allIdentifiers = service.getAllIdentifiers(userid);
        assertEquals(0, allIdentifiers.size());

        user3.setUserAttribute("id_modsperson", "junit_modsperson_00000404");
        MCRUserManager.updateUser(user3);
        allIdentifiers = service.getAllIdentifiers(userid);
        assertEquals(0, allIdentifiers.size());
    }

}
