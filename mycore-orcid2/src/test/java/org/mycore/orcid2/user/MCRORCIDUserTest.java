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

package org.mycore.orcid2.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;
import static org.mycore.orcid2.user.MCRORCIDUser.ATTR_ORCID_ID;
import static org.mycore.resource.MCRResourceHelper.getResourceUrl;

import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mycore.access.MCRAccessException;
import org.mycore.common.MCRConstants;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRSystemUserInformation;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.mods.MCRMODSWrapper;
import org.mycore.orcid2.client.MCRORCIDCredential;
import org.mycore.orcid2.exception.MCRORCIDException;
import org.mycore.orcid2.util.MCRIdentifier;
import org.mycore.test.MCRJPAExtension;
import org.mycore.test.MCRMetadataExtension;
import org.mycore.test.MyCoReTest;
import org.mycore.user2.MCRUser;
import org.mycore.user2.MCRUserAttribute;
import org.mycore.user2.MCRUserManager;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

@MyCoReTest
@ExtendWith(MCRJPAExtension.class)
@ExtendWith(MCRMetadataExtension.class)
public class MCRORCIDUserTest {

    private static final String ORCID_1 = "0000-0001-2345-6789";

    private static final String ORCID_2 = "0000-0002-3456-7895";

    private static final String ORCID_3 = "0000-0003-4567-8985";

    private static final String ACCESS_TOKEN = "accessToken";

    private static final String HANDLER_ATTRIBUTE_NAME = "MCR.ORCID2.AttributeHandler.Class";

    private static MCRUser mcrUserMock;

    @BeforeAll
    public static void prepare() throws Exception {
        mcrUserMock = Mockito.mock(MCRUser.class);
        SortedSet<MCRUserAttribute> attrs = new TreeSet<>(
            List.of(
                new MCRUserAttribute(ATTR_ORCID_ID, ORCID_1),
                new MCRUserAttribute(ATTR_ORCID_ID, ORCID_2),
                new MCRUserAttribute(ATTR_ORCID_ID, ORCID_3)
            )
        );
        when(mcrUserMock.getAttributes()).thenReturn(attrs);
    }

    @Test
    public void testStoreGetCredentials() {
        MCRUser user = new MCRUser("junit");
        MCRORCIDUser orcidUser = new MCRORCIDUser(user);
        assertEquals(0, orcidUser.getCredentials().size());
        final MCRORCIDCredential credential = new MCRORCIDCredential(ACCESS_TOKEN);
        orcidUser.addCredential(ORCID_1, credential);
        // id_orcid + orcid_credential_orcid
        assertEquals(2, user.getAttributes().size());
        assertNotNull(user.getUserAttribute("orcid_credential_" + ORCID_1));
        assertEquals(ORCID_1, user.getUserAttribute("id_orcid"));
        assertEquals(credential, orcidUser.getCredentialByORCID(ORCID_1));
    }

    @Test
    public void testRemoveAllCredentials() {
        MCRUser user = new MCRUser("junit");
        MCRORCIDUser orcidUser = new MCRORCIDUser(user);
        final MCRORCIDCredential credential = new MCRORCIDCredential(ACCESS_TOKEN);
        orcidUser.addCredential(ORCID_1, credential);
        user.setUserAttribute("test", "test");
        orcidUser.removeAllCredentials();
        // id_orcid + test
        assertEquals(2, user.getAttributes().size());
        assertEquals(ORCID_1, user.getUserAttribute("id_orcid"));
        assertEquals("test", user.getUserAttribute("test"));
    }

    @Test
    public void testGetORCIDsUserAttributeImpl() {

        MCRConfiguration2.set(HANDLER_ATTRIBUTE_NAME,
            "org.mycore.orcid2.user.MCRORCIDIDAttributeHandlerUserAttributeImpl");

        MCRORCIDUser orcidUser = new MCRORCIDUser(mcrUserMock);

        // Get ORCIDs with MCRORCIDAccessUserAttributeImpl implementation
        Set<String> orcids = orcidUser.getORCIDs();
        assertEquals(3, orcids.size());

        Set<String> expected = Set.of(ORCID_1, ORCID_2, ORCID_3);
        assertEquals(expected, orcids);
    }

    @Test
    public void testGetORCIDsModspersonImpl()
        throws IOException, JDOMException, MCRAccessException, URISyntaxException {
        MCRSessionMgr.getCurrentSession().setUserInformation(MCRSystemUserInformation.SUPER_USER);
        MCRConfiguration2.set(HANDLER_ATTRIBUTE_NAME,
            "org.mycore.orcid2.user.MCRORCIDIDAttributeHandlerModspersonImpl");

        MCRUser user = new MCRUser("junit");
        user.getAttributes().add(new MCRUserAttribute(ATTR_ORCID_ID, ORCID_1));
        user.getAttributes().add(new MCRUserAttribute(ATTR_ORCID_ID, ORCID_2));
        user.getAttributes().add(new MCRUserAttribute(ATTR_ORCID_ID, ORCID_3));
        MCRORCIDUser orcidUser = new MCRORCIDUser(user);

        // Get ORCIDs with MCRORCIDAccessModspersonImpl implementation but no reference
        Set<String> orcids = orcidUser.getORCIDs();
        assertEquals(0, orcids.size());

        user.setUserAttribute("id_modsperson", "junit_modsperson_00000001");
        MCRUserManager.updateUser(user);

        MCRObject obj1 = new MCRObject(getResourceUrl(buildFilePath("junit_modsperson_00000001.xml")).toURI());
        MCRMetadataManager.create(obj1);
        // Get ORCIDs with MCRORCIDAccessModspersonImpl implementation and reference
        orcids = orcidUser.getORCIDs();
        assertEquals(3, orcids.size());
        Set<String> expected = Set.of(ORCID_1, ORCID_2, ORCID_3);
        assertEquals(expected, orcids);
    }

    @Test
    public void testAddORCIDUserAttributeImpl() {
        MCRConfiguration2.set(HANDLER_ATTRIBUTE_NAME,
            "org.mycore.orcid2.user.MCRORCIDIDAttributeHandlerUserAttributeImpl");

        MCRUser user = new MCRUser("junit");
        MCRORCIDUser orcidUser = new MCRORCIDUser(user);

        orcidUser.addORCID(ORCID_1);
        orcidUser.addORCID(ORCID_2);
        orcidUser.addORCID(ORCID_3);

        Set<MCRUserAttribute> attributes = orcidUser.getUser().getAttributes();
        assertEquals(3, attributes.size());

        Set<MCRUserAttribute> expected = Set.of(new MCRUserAttribute("id_orcid", ORCID_1),
            new MCRUserAttribute("id_orcid", ORCID_2),
            new MCRUserAttribute("id_orcid", ORCID_3));
        assertEquals(expected, attributes);
    }

    @Test
    public void testAddORCIDModspersonImpl() throws MCRAccessException, URISyntaxException, IOException, JDOMException {
        MCRSessionMgr.getCurrentSession().setUserInformation(MCRSystemUserInformation.SUPER_USER);
        MCRConfiguration2.set(HANDLER_ATTRIBUTE_NAME,
            "org.mycore.orcid2.user.MCRORCIDIDAttributeHandlerModspersonImpl");

        MCRUser user = new MCRUser("junit");
        MCRORCIDUser orcidUser = new MCRORCIDUser(user);

        orcidUser.addORCID(ORCID_1); // check for no error if no reference
        MCRObject modsperson = new MCRObject(getResourceUrl(
            buildFilePath("junit_modsperson_00000002.xml")).toURI());
        MCRMetadataManager.create(modsperson);
        user.setUserAttribute("id_modsperson", "junit_modsperson_00000002");
        MCRUserManager.updateUser(user);

        orcidUser.addORCID(ORCID_1);
        orcidUser.addORCID(ORCID_2);
        orcidUser.addORCID(ORCID_3);

        MCRUser userUpdated = MCRUserManager.getUser("junit");
        Set<MCRUserAttribute> attributes = userUpdated.getAttributes();
        assertEquals(1, attributes.size());
        assertEquals("id_modsperson", attributes.iterator().next().getName());
        assertEquals("junit_modsperson_00000002", attributes.iterator().next().getValue());

        MCRObject modspersonUpdated = MCRMetadataManager.retrieveMCRObject(MCRObjectID
            .getInstance("junit_modsperson_00000002"));
        assertNotNull(modspersonUpdated);

        MCRMODSWrapper wrapper = new MCRMODSWrapper(modspersonUpdated);
        Element personName = wrapper.getElement("mods:name[@type='personal']");
        List<Element> nameIdentifiers = personName.getChildren("nameIdentifier", MCRConstants.MODS_NAMESPACE);
        assertEquals(3, nameIdentifiers.size());
        nameIdentifiers.forEach(a -> assertEquals("orcid", a.getAttributeValue("type")));

        Set<String> actualValues = nameIdentifiers.stream()
            .map(Element::getText)
            .collect(Collectors.toSet());
        Set<String> expectedValues = Set.of(
            ORCID_1,
            ORCID_2,
            ORCID_3);
        assertEquals(expectedValues, actualValues);

    }

    @Test
    public void testAddORCIDInvalid() {
        MCRUser user = new MCRUser("junit");
        MCRORCIDUser orcidUser = new MCRORCIDUser(user);
        MCRConfiguration2.set(HANDLER_ATTRIBUTE_NAME,
            "org.mycore.orcid2.user.MCRORCIDIDAttributeHandlerUserAttributeImpl");
        Assertions.assertThrows(MCRORCIDException.class, () -> {
            orcidUser.addORCID("abcd");
        });

    }

    @Test
    public void testGetIdentifiersUserAttributeImpl() {
        MCRConfiguration2.set(HANDLER_ATTRIBUTE_NAME,
            "org.mycore.orcid2.user.MCRORCIDIDAttributeHandlerUserAttributeImpl");

        MCRUser user = new MCRUser("junit");
        user.getAttributes().add(new MCRUserAttribute("id_scopus", "87654321"));
        user.getAttributes().add(new MCRUserAttribute("id_something", "abcd"));
        user.getAttributes().add(new MCRUserAttribute("just_something", "1234"));

        MCRORCIDUser orcidUser = new MCRORCIDUser(user);

        orcidUser.addORCID(ORCID_1);

        Set<MCRIdentifier> identifiers = orcidUser.getIdentifiers();
        assertEquals(3, identifiers.size());
        Set<MCRIdentifier> expected = new HashSet<>(Set.of(new MCRIdentifier("scopus", "87654321"),
            new MCRIdentifier("something", "abcd"),
            new MCRIdentifier("orcid", "0000-0001-2345-6789")));
        assertEquals(expected, identifiers);
    }

    @Test
    public void testGetIdentifiersModspersonImpl()
        throws MCRAccessException, URISyntaxException, IOException, JDOMException {
        MCRSessionMgr.getCurrentSession().setUserInformation(MCRSystemUserInformation.SUPER_USER);
        // getIdentifiers with MCRORCIDAccessModspersonImpl implementation but no reference
        MCRConfiguration2.set(HANDLER_ATTRIBUTE_NAME,
            "org.mycore.orcid2.user.MCRORCIDIDAttributeHandlerModspersonImpl");

        MCRUser user = new MCRUser("junit");
        user.getAttributes().add(new MCRUserAttribute("id_scopus", "87654321"));
        user.getAttributes().add(new MCRUserAttribute("id_something", "abcd"));
        user.getAttributes().add(new MCRUserAttribute("just_something", "1234"));

        MCRORCIDUser orcidUser = new MCRORCIDUser(user);
        orcidUser.addORCID(ORCID_1);

        Set<MCRIdentifier> identifiers = orcidUser.getIdentifiers();
        assertEquals(0, identifiers.size());

        Set<MCRIdentifier> expected = new HashSet<>(Set.of(new MCRIdentifier("scopus", "87654321"),
            new MCRIdentifier("something", "abcd"),
            new MCRIdentifier("orcid", "0000-0001-2345-6789")));

        user.setUserAttribute("id_modsperson", "junit_modsperson_00000001");
        MCRUserManager.updateUser(user);

        MCRObject obj1 = new MCRObject(getResourceUrl(
            buildFilePath("junit_modsperson_00000001.xml")).toURI());
        MCRMetadataManager.create(obj1);

        // getIdentifiers with MCRORCIDAccessModspersonImpl implementation and reference
        identifiers = orcidUser.getIdentifiers();
        assertEquals(5, identifiers.size());
        expected.add(new MCRIdentifier("orcid", ORCID_2));
        expected.add(new MCRIdentifier("orcid", ORCID_3));
        assertEquals(expected, identifiers);
    }

    private String buildFilePath(String fileName) {
        return new MessageFormat("/{0}/{1}", Locale.ROOT).format(
            new Object[] { this.getClass().getSimpleName(), fileName });
    }
}
