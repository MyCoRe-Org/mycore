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

package org.mycore.frontend.xeditor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.jaxen.JaxenException;
import org.jdom2.Document;
import org.jdom2.Element;
import org.junit.Before;
import org.junit.Test;
import org.mycore.common.MCRTestCase;
import org.mycore.common.xml.MCRNodeBuilder;

/**
 * @author Frank Lützenkirchen
 */
public class MCRBindingTest extends MCRTestCase {

    private MCRBinding binding;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        Element root = new Element("document");
        root.addContent(new Element("title").setAttribute("type", "main").setText("title1"));
        root.addContent(new Element("title").setAttribute("type", "alternative").setText("title2"));
        Element author = new Element("author");
        root.addContent(author);
        author.addContent(new Element("firstName").setText("John"));
        author.addContent(new Element("lastName").setText("Doe"));
        binding = new MCRBinding(new Document(root));
    }

    @Test
    public void testSimpleBindings() throws JaxenException {
        binding = new MCRBinding("document", true, binding);
        binding = new MCRBinding("title", true, binding);
        assertEquals("title1", binding.getValue());
        binding = binding.getParent();
        binding = binding.getParent();

        binding = new MCRBinding("document/title[2]", true, binding);
        assertEquals("title2", binding.getValue());
        binding = binding.getParent();

        binding = new MCRBinding("document/title[@type='main']", true, binding);
        assertEquals("title1", binding.getValue());
        binding = binding.getParent();

        binding = new MCRBinding("document/title[@type='alternative']", true, binding);
        assertEquals("title2", binding.getValue());
        binding = binding.getParent();

        binding = new MCRBinding("document/author", true, binding);
        binding = new MCRBinding("firstName", true, binding);
        assertEquals("John", binding.getValue());
        binding = binding.getParent();
        binding = binding.getParent();
    }

    @Test
    public void testComplexBindings() throws JaxenException {
        binding = new MCRBinding("document/title[contains(text(),'1')]", true, binding);
        assertEquals("title1", binding.getValue());
        binding = binding.getParent();

        binding = new MCRBinding("//title[@type]", true, binding);
        assertEquals("title1", binding.getValue());
        binding = binding.getParent();

        binding = new MCRBinding("//title[not(@type='main')]", true, binding);
        assertEquals("title2", binding.getValue());
        binding = binding.getParent();

        binding = new MCRBinding("/document/title[../author/lastName='Doe'][2]", true, binding);
        assertEquals("title2", binding.getValue());
        binding = binding.getParent();

        binding = new MCRBinding("//*", true, binding);
        binding = new MCRBinding("*[name()='title'][2]", true, binding);
        assertEquals("title2", binding.getValue());
        binding = binding.getParent();
        binding = binding.getParent();
    }

    @Test
    public void testHasValue() throws JaxenException {
        binding = new MCRBinding("document/title", true, binding);
        assertTrue(binding.hasValue("title1"));
        assertTrue(binding.hasValue("title2"));
        assertFalse(binding.hasValue("other"));
        binding = binding.getParent();

        binding = new MCRBinding("document/author/*", true, binding);
        assertTrue(binding.hasValue("John"));
        assertTrue(binding.hasValue("Doe"));
        assertFalse(binding.hasValue("other"));
    }

    @Test
    public void testCollectorVariables() throws JaxenException {
        Map<String, Object> variables = new HashMap<>();
        variables.put("type", "main");

        binding = new MCRBinding("document", true, binding);
        binding.setVariables(variables);
        binding = new MCRBinding("title[@type=$type]", true, binding);
        assertTrue(binding.hasValue("title1"));
        assertEquals(1, binding.getBoundNodes().size());
    }

    @Test
    public void testDefiningVariables() throws JaxenException {
        binding = new MCRBinding("document", true, binding);
        new MCRBinding("title[1]", null, "inheritMe", binding);
        new MCRBinding("title[2]", null, "overwriteMe", binding);
        assertEquals("title1", new MCRBinding("$inheritMe", true, binding).getValue());
        assertEquals("title2", new MCRBinding("$overwriteMe", true, binding).getValue());
        binding = new MCRBinding("author", true, binding);
        new MCRBinding("firstName", null, "overwriteMe", binding);
        assertEquals("title1", new MCRBinding("$inheritMe", true, binding).getValue());
        assertEquals("John", new MCRBinding("$overwriteMe", true, binding).getValue());
    }

    @Test
    public void testGroupByReferencedID() throws JaxenException {
        String builder =
            "document[name/@id='n1'][note/@href='#n1'][location/@href='#n1'][name[@id='n2']][location[@href='#n2']]";
        Element document = new MCRNodeBuilder().buildElement(builder, null, null);
        MCRBinding rootBinding = new MCRBinding(new Document(document));
        MCRBinding documentBinding = new MCRBinding("document", true, rootBinding);

        MCRBinding id = new MCRBinding("name[@id][1]/@id", null, "id", documentBinding);
        assertEquals("/document/name[1]/@id", id.getAbsoluteXPath());
        assertEquals("n1", id.getValue());
        binding = new MCRBinding("note[@href=concat('#',$id)]", true, documentBinding);
        Element note = (Element) (binding.getBoundNode());
        assertEquals("note", note.getName());
        assertEquals("#n1", note.getAttributeValue("href"));
        binding = new MCRBinding("location[@href=concat('#',$id)]", true, documentBinding);
        Element location = (Element) (binding.getBoundNode());
        assertEquals("location", location.getName());
        assertEquals("#n1", location.getAttributeValue("href"));

        id = new MCRBinding("name[@id][2]/@id", null, "id", documentBinding);
        assertEquals("/document/name[2]/@id", id.getAbsoluteXPath());
        assertEquals("n2", id.getValue());

        binding = new MCRBinding("note[@href=concat('#',$id)]", true, documentBinding);
        note = (Element) (binding.getBoundNode());
        assertEquals("note", note.getName());
        assertEquals("#n2", note.getAttributeValue("href"));

        binding = new MCRBinding("location[@href=concat('#',$id)]", true, documentBinding);
        location = (Element) (binding.getBoundNode());
        assertEquals("location", location.getName());
        assertEquals("#n2", location.getAttributeValue("href"));
    }
}
