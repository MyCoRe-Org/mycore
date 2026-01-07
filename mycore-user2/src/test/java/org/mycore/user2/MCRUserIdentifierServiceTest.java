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

package org.mycore.user2;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mycore.datamodel.legalentity.MCRIdentifier;
import org.mycore.test.MCRJPAExtension;
import org.mycore.test.MyCoReTest;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

@MyCoReTest
@ExtendWith({ MCRJPAExtension.class, MCRUserExtension.class })
public class MCRUserIdentifierServiceTest {

    private static final String ORCID_1 = "0000-0001-2345-6789";

    private static final String ORCID_2 = "0000-0002-3456-7895";

    private static final String ORCID_3 = "0000-0003-4567-8985";

    private static final String SCOPUS = "87654321";

    MCRUserIdentifierService service;

    MCRUser user;

    @BeforeEach
    public void setUp() throws Exception {
        user = new MCRUser("john");
        user.setRealName("John Doe");
        user.getAttributes().add(new MCRUserAttribute("id_orcid", ORCID_1));
        user.getAttributes().add(new MCRUserAttribute("id_orcid", ORCID_2));
        user.getAttributes().add(new MCRUserAttribute("id_orcid", ORCID_3));
        user.setUserAttribute("id_scopus", SCOPUS);
        user.setUserAttribute("other", "abc");
        MCRUserManager.createUser(user);
        service = new MCRUserIdentifierService();
    }

    @Test
    public final void testFindAllIdentifiers() {
        Set<MCRIdentifier> allIdentifiers = service.findAllIdentifiers(
            new MCRIdentifier("userid", user.getUserID()));
        Set<MCRIdentifier> expected = Set.of(new MCRIdentifier("orcid", ORCID_1),
            new MCRIdentifier("orcid", ORCID_2),
            new MCRIdentifier("orcid", ORCID_3),
            new MCRIdentifier("scopus", SCOPUS));
        assertEquals(expected, allIdentifiers);
    }

    @Test
    public final void testFindTypedIdentifiers() {
        final MCRIdentifier userid = new MCRIdentifier("userid", user.getUserID());
        Set<MCRIdentifier> typedIdentifiers = service.findTypedIdentifiers(userid, "orcid");
        Set<MCRIdentifier> expected = Set.of(new MCRIdentifier("orcid", ORCID_1),
            new MCRIdentifier("orcid", ORCID_2),
            new MCRIdentifier("orcid", ORCID_3));
        assertEquals(expected, typedIdentifiers);

        typedIdentifiers = service.findTypedIdentifiers(userid, "id_orcid");
        assertEquals(0, typedIdentifiers.size());

        typedIdentifiers = service.findTypedIdentifiers(userid, "scopus");
        assertEquals(Set.of(new MCRIdentifier("scopus", SCOPUS)), typedIdentifiers);

        typedIdentifiers = service.findTypedIdentifiers(userid, "other");
        assertEquals(Set.of(new MCRIdentifier("other", "abc")), typedIdentifiers);
    }

    @Test
    public final void testAddIdentifier() {
        MCRUser user1 = new MCRUser("jane");
        user1.setRealName("Jane Doe");
        user1.getAttributes().add(new MCRUserAttribute("id_orcid", ORCID_1));
        user1.getAttributes().add(new MCRUserAttribute("id_orcid", ORCID_2));
        MCRUserManager.createUser(user1);

        final MCRIdentifier userid = new MCRIdentifier("userid", user1.getUserID());

        Set<MCRIdentifier> allIdentifiers = service.findAllIdentifiers(userid);
        assertEquals(2, allIdentifiers.size());

        service.addIdentifier(userid, new MCRIdentifier("orcid", ORCID_1)); // don't add id twice
        allIdentifiers = service.findAllIdentifiers(userid);
        assertEquals(2, allIdentifiers.size());

        service.addIdentifier(userid, new MCRIdentifier("orcid", ORCID_3));
        allIdentifiers = service.findAllIdentifiers(userid);
        assertEquals(3, allIdentifiers.size());
        Set<MCRIdentifier> expected = Set.of(new MCRIdentifier("orcid", ORCID_1),
            new MCRIdentifier("orcid", ORCID_2),
            new MCRIdentifier("orcid", ORCID_3));
        assertEquals(expected, allIdentifiers);
    }

    @Test
    public final void testNoUser() {
        MCRIdentifier nonameUserid = new MCRIdentifier("userid", "noname");
        Set<MCRIdentifier> allIdentifiers = service.findAllIdentifiers(nonameUserid);
        assertEquals(0, allIdentifiers.size());

        Set<MCRIdentifier> typedIdentifiers = service.findTypedIdentifiers(nonameUserid, "orcid");
        assertEquals(0, typedIdentifiers.size());

        service.addIdentifier(nonameUserid, new MCRIdentifier("orcid", ORCID_1));
        allIdentifiers = service.findAllIdentifiers(nonameUserid);
        assertEquals(0, allIdentifiers.size());
    }
}
