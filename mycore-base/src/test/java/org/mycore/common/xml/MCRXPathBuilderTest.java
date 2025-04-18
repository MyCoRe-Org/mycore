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

package org.mycore.common.xml;

import static org.junit.Assert.assertEquals;

import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.junit.Test;
import org.mycore.common.MCRTestCase;

/**
 * @author Frank Lützenkirchen
 */
public class MCRXPathBuilderTest extends MCRTestCase {

    @Test
    public void testXPath() {
        Element root = new Element("root");
        Element title1 = new Element("title");
        Element title2 = new Element("title");
        Element author = new Element("contributor");
        Attribute role = new Attribute("role", "author");
        Attribute lang = new Attribute("lang", "de", Namespace.XML_NAMESPACE);
        author.setAttribute(role);
        author.setAttribute(lang);
        root.addContent(title1);
        root.addContent(author);
        root.addContent(title2);
        new Document(root);

        assertEquals("/root", MCRXPathBuilder.buildXPath(root));
        assertEquals("/root/contributor[1]", MCRXPathBuilder.buildXPath(author));
        assertEquals("/root/title[1]", MCRXPathBuilder.buildXPath(title1));
        assertEquals("/root/title[2]", MCRXPathBuilder.buildXPath(title2));
        assertEquals("/root/contributor[1]/@role", MCRXPathBuilder.buildXPath(role));
        assertEquals("/root/contributor[1]/@xml:lang", MCRXPathBuilder.buildXPath(lang));

        root.detach();
        assertEquals("root", MCRXPathBuilder.buildXPath(root));
        assertEquals("root/contributor[1]", MCRXPathBuilder.buildXPath(author));
    }

    @Test
    public void testMODS() {
        Element container = new Element("container");
        new Document(container);
        Element mods = new Element("mods", "http://www.loc.gov/mods/v3");
        Element name = new Element("name", "http://www.loc.gov/mods/v3");
        mods.addContent(name);
        container.addContent(mods);

        assertEquals("/container", MCRXPathBuilder.buildXPath(container));
        assertEquals("/container/mods:mods[1]", MCRXPathBuilder.buildXPath(mods));
        assertEquals("/container/mods:mods[1]/mods:name[1]", MCRXPathBuilder.buildXPath(name));
    }
}
