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

import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mycore.datamodel.legalentity.MCRIdentifier;
import org.mycore.datamodel.legalentity.MCRLegalEntityService;
import org.mycore.orcid2.client.MCRORCIDCredential;
import org.mycore.orcid2.exception.MCRORCIDException;
import org.mycore.test.MCRJPAExtension;
import org.mycore.test.MyCoReTest;
import org.mycore.user2.MCRUser;
import org.mycore.user2.MCRUserAttribute;
import org.mycore.user2.MCRUserIdentifierService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

@MyCoReTest
@ExtendWith(MCRJPAExtension.class)
public class MCRORCIDUserTest {

    private static final String ORCID = "0000-0001-2345-6789";
    private static final String ORCID_2 = "0000-0002-2345-6789";
    private static final String ORCID_3 = "0000-0003-2345-6789";

    private static final String ACCESS_TOKEN = "accessToken";

    private static MCRORCIDUser orcidUser;

    private static MCRUser userMock;

    @BeforeEach
    public void prepare() {
        userMock = new MCRUser("junit");
        MCRLegalEntityService legalEntityServiceMock = Mockito.mock(MCRUserIdentifierService.class);
        orcidUser = new MCRORCIDUser(userMock, legalEntityServiceMock);

        when(legalEntityServiceMock.findAllIdentifiers(any(MCRIdentifier.class))).thenAnswer(
            invocation -> userMock.getAttributes()
        .stream().map(attr -> new MCRIdentifier(stripPrefix(attr.getName()), attr.getValue()))
        .collect(Collectors.toSet()));

        doAnswer(invocation -> {
            MCRIdentifier identifierToAdd = invocation.getArgument(1);
            userMock.getAttributes().add(new MCRUserAttribute(
                "id_" + identifierToAdd.getType(), identifierToAdd.getValue()));
            return null;
        }).when(legalEntityServiceMock).addIdentifier(any(MCRIdentifier.class), any(MCRIdentifier.class));
    }

    @Test
    public void testStoreGetCredentials() {
        assertEquals(0, orcidUser.getCredentials().size());
        final MCRORCIDCredential credential = new MCRORCIDCredential(ACCESS_TOKEN);
        orcidUser.addCredential(ORCID, credential);
        // id_orcid + orcid_credential_orcid
        assertEquals(2, userMock.getAttributes().size());
        assertNotNull(userMock.getUserAttribute("orcid_credential_" + ORCID));
        assertEquals(ORCID, userMock.getUserAttribute("id_orcid"));
        assertEquals(credential, orcidUser.getCredentialByORCID(ORCID));
    }

    @Test
    public void testRemoveAllCredentials() {
        final MCRORCIDCredential credential = new MCRORCIDCredential(ACCESS_TOKEN);
        orcidUser.addCredential(ORCID, credential);
        userMock.setUserAttribute("test", "test");
        orcidUser.removeAllCredentials();
        // id_orcid + test
        assertEquals(2, userMock.getAttributes().size());
        assertEquals(ORCID, userMock.getUserAttribute("id_orcid"));
        assertEquals("test", userMock.getUserAttribute("test"));
    }

    @Test
    public void testAddInvalidCredentials() {
        assertEquals(0, orcidUser.getCredentials().size());
        final MCRORCIDCredential credential = new MCRORCIDCredential(null);
        assertThrows(MCRORCIDException.class, () -> orcidUser.addCredential(ORCID, credential));
        assertEquals(0, orcidUser.getCredentials().size());
    }

    @Test
    public void testGetORCIDs() {
        userMock.setUserAttribute("test", "test");
        userMock.getAttributes().add(new MCRUserAttribute("id_orcid", ORCID));
        userMock.getAttributes().add(new MCRUserAttribute("id_orcid", ORCID_2));
        userMock.getAttributes().add(new MCRUserAttribute("id_orcid", ORCID_3));
        Set<String> orcids = orcidUser.getORCIDs();
        assertEquals(Set.of(ORCID, ORCID_2, ORCID_3), orcids);
    }

    @Test
    public void testAddORCID() {
        assertEquals(0, orcidUser.getORCIDs().size());
        orcidUser.addORCID(ORCID);
        assertEquals(1, orcidUser.getORCIDs().size());
        assertEquals(Set.of(ORCID), orcidUser.getORCIDs());
    }

    @Test
    public void testAddInvalidORCID() {
        assertEquals(0, orcidUser.getORCIDs().size());
        assertThrows(MCRORCIDException.class, () -> orcidUser.addORCID("1234"));
        assertEquals(0, orcidUser.getORCIDs().size());
    }

    @Test
    public void testGetIdentifiers() {
        userMock.setUserAttribute("test", "test");
        userMock.getAttributes().add(new MCRUserAttribute("id_orcid", ORCID));
        Set<MCRIdentifier> identifiers = orcidUser.getIdentifiers();
        assertEquals(Set.of(new MCRIdentifier("test", "test"),
        new MCRIdentifier("orcid", ORCID)), identifiers);
    }

    private String stripPrefix(String name) {
        return name.startsWith(MCRORCIDUser.ATTR_ID_PREFIX) ?
               name.substring(MCRORCIDUser.ATTR_ID_PREFIX.length()) : name;
    }
}
