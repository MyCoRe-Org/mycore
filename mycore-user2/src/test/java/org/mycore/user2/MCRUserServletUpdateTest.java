/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.SortedSet;

import org.jdom2.Element;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Oleksiy (Alex) Levshyn
 *
 */

public class MCRUserServletUpdateTest extends MCRUserTestCase {

    private MCRUserServlet servlet;
    private MCRUser user;
    private Element userElement;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        servlet = new MCRUserServlet();
        user = new MCRUser("testuser", "local");
        userElement = new Element("user");
    }

    @Test
    public void testUpdateAttributesWithSameNameDifferentValue() {
        // Setup existing attribute
        user.getAttributes().add(new MCRUserAttribute("role", "admin"));

        // Create new attribute with same name but different value
        Element attributes = new Element("attributes");
        attributes.addContent(createAttribute("role", "editor")); // Different value
        userElement.addContent(attributes);

        servlet.updateBasicUserInfo(userElement, user);

        // Should replace the existing attribute
        SortedSet<MCRUserAttribute> result = user.getAttributes();
        assertEquals(1, result.size());
        assertContainsAttribute(result, "role", "editor");
        assertFalse(containsAttribute(result, "role", "admin"));
    }

    @Test
    public void testUpdateAttributesWithSameNameAndValue() {
        // Setup existing attribute
        MCRUserAttribute existing = new MCRUserAttribute("role", "admin");
        user.getAttributes().add(existing);

        // Create identical attribute
        Element attributes = new Element("attributes");
        attributes.addContent(createAttribute("role", "admin")); // Same value
        userElement.addContent(attributes);

        servlet.updateBasicUserInfo(userElement, user);

        // Should keep the existing attribute (no duplicate)
        SortedSet<MCRUserAttribute> result = user.getAttributes();
        assertEquals(1, result.size());
        assertContainsAttribute(result, "role", "admin");
        // Verify it's the same object (not replaced)
        assertTrue(result.first() == existing);
    }

    @Test
    public void testUpdateMultipleAttributesWithSameName() {
        // Setup existing attributes
        user.getAttributes().add(new MCRUserAttribute("role", "admin"));
        user.getAttributes().add(new MCRUserAttribute("department", "IT"));

        // Create new attributes including multiple with same name
        Element attributes = new Element("attributes");
        attributes.addContent(createAttribute("role", "editor")); // Update existing
        attributes.addContent(createAttribute("role", "reviewer")); // Second with same name
        userElement.addContent(attributes);

        servlet.updateBasicUserInfo(userElement, user);

        // Should keep only one attribute per name (last one wins)
        SortedSet<MCRUserAttribute> result = user.getAttributes();
        assertEquals(2, result.size()); // department + one role
        assertContainsAttribute(result, "department", "IT");
        // Either editor or reviewer should be present, but not both
        assertTrue(containsAttribute(result, "role", "editor") ||
            containsAttribute(result, "role", "reviewer"));
    }

    @Test
    public void testDatabasePersistenceScenario() throws Exception {
        // Setup initial user with attributes
        MCRUser initialUser = new MCRUser("dbuser", "local");
        initialUser.getAttributes().add(new MCRUserAttribute("role", "admin"));
        MCRUserManager.createUser(initialUser);

        // Retrieve user from "database"
        startNewTransaction();
        MCRUser storedUser = MCRUserManager.getUser("dbuser", "local");

        // Update with new attributes
        Element updateElement = new Element("user");
        Element attributes = new Element("attributes");
        attributes.addContent(createAttribute("role", "superadmin")); // Changed value
        attributes.addContent(createAttribute("department", "IT")); // New attribute
        updateElement.addContent(attributes);

        servlet.updateBasicUserInfo(updateElement, storedUser);
        MCRUserManager.updateUser(storedUser);

        // Verify changes persisted
        startNewTransaction();
        MCRUser updatedUser = MCRUserManager.getUser("dbuser", "local");
        assertEquals(2, updatedUser.getAttributes().size());
        assertContainsAttribute(updatedUser.getAttributes(), "role", "superadmin");
        assertContainsAttribute(updatedUser.getAttributes(), "department", "IT");
        assertFalse(containsAttribute(updatedUser.getAttributes(), "role", "admin"));
    }

    private Element createAttribute(String name, String value) {
        Element attr = new Element("attribute");
        attr.setAttribute("name", name);
        attr.setAttribute("value", value);
        return attr;
    }

    private boolean containsAttribute(SortedSet<MCRUserAttribute> attributes, String name, String value) {
        return attributes.stream()
            .anyMatch(attr -> attr.getName().equals(name) && attr.getValue().equals(value));
    }

    private void assertContainsAttribute(SortedSet<MCRUserAttribute> attributes, String name, String value) {
        assertTrue("Attribute " + name + "=" + value + " not found",
            containsAttribute(attributes, name, value));
    }
}
