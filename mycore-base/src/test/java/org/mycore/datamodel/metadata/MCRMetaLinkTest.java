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

package org.mycore.datamodel.metadata;

import static org.junit.Assert.*;

import java.util.Map;

import org.jdom2.Element;
import org.junit.Test;
import org.mycore.common.MCRTestCase;

/**
 * This class is a JUnit test case for org.mycore.datamodel.metadata.MCRMetaLinkID and MCRMetaLinkID.
 * 
 * @author Jens Kupferschmidt
 *
 */
public class MCRMetaLinkTest extends MCRTestCase {

    @Override
    protected Map<String, String> getTestProperties() {
        Map<String, String> testProperties = super.getTestProperties();
        testProperties.put("MCR.Metadata.Type.mods", "true");
        testProperties.put("MCR.Metadata.Type.derivate", "true");
        return testProperties;
    }

    /**
     * check createXML, setFromDom, equals and clone
     */
    @Test
    public void checkCreateParseEqualsCloneLink() {
        MCRMetaLink link1 = new MCRMetaLink("subtag", 0);
        link1.setReference("https://www.zoje.de", "ZOJE", "IV Zittauer Schmalspurbahnen e. V.");

        Element link1_xml = link1.createXML();
        MCRMetaLink link1_read = new MCRMetaLink();
        link1_read.setFromDOM(link1_xml);
        assertEquals("read objects from XML should be equal", link1, link1_read);

        MCRMetaLink link1_clone = link1_read.clone();
        assertEquals("cloned object should be equal with original", link1_read, link1_clone);

        MCRMetaLink link2 = new MCRMetaLink("subtag", 0);
        link2.setBiLink("ZOJE", "SOEG", "Partner Zittauer Schmalspurbahnen");

        Element link2_xml = link2.createXML();
        MCRMetaLink link2_read = new MCRMetaLink();
        link2_read.setFromDOM(link2_xml);
        assertEquals("read objects from XML should be equal", link2, link2_read);

        MCRMetaLink link2_clone = link2_read.clone();
        assertEquals("cloned object should be equal with original", link2_read, link2_clone);
    }

    /**
     * check createXML, setFromDom, equals and clone
     */
    @Test
    public void checkCreateParseEqualsCloneLinkID() {
        MCRMetaLinkID link1 = new MCRMetaLinkID("subtag", 0);
        link1.setReference("MIR_mods_00000001", "MODS", "MODS-Objekt 1");
        link1.setType("mytype");

        Element link1_xml = link1.createXML();
        MCRMetaLinkID link1_read = new MCRMetaLinkID();
        link1_read.setFromDOM(link1_xml);
        assertEquals("read objects from XML should be equal", link1, link1_read);

        MCRMetaLinkID link1_clone = link1_read.clone();
        assertEquals("cloned object should be equal with original", link1_read, link1_clone);

        MCRMetaLinkID link2 = new MCRMetaLinkID("subtag", 0);
        link2.setReference(MCRObjectID.getInstance("MIR_mods_00000001"), "MODS", "MODS-Objekt 1");
        link2.setType("mytype");
        assertEquals("MCRID object should be equal with original", link1, link2);

        MCRMetaLinkID link3 = new MCRMetaLinkID("subtag", 0);
        link3.setBiLink("MIR_mods_00000001", "MIR_derivate_00000001", "Derivate link");

        Element link3_xml = link3.createXML();
        MCRMetaLinkID link3_read = new MCRMetaLinkID();
        link3_read.setFromDOM(link3_xml);
        assertEquals("read objects from XML should be equal", link3, link3_read);

        MCRMetaLinkID link3_clone = link3_read.clone();
        assertEquals("cloned object should be equal with original", link3_read, link3_clone);

        MCRMetaLinkID link4 = new MCRMetaLinkID("subtag", 0);
        link4.setBiLink(MCRObjectID.getInstance("MIR_mods_00000001"),
            MCRObjectID.getInstance("MIR_derivate_00000001"), "Derivate link");
        assertEquals("MCRID object should be equal with original", link3, link4);

    }
}
