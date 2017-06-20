/*
 * $Id$
 * $Revision: 5697 $ $Date: 08.02.2012 $
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */

package org.mycore.user2.utils;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlValue;
import javax.xml.transform.stream.StreamSource;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.junit.Test;
import org.mycore.common.xml.MCRURIResolver;
import org.mycore.common.xml.MCRXMLHelper;
import org.mycore.user2.MCRUser;
import org.mycore.user2.MCRUserTestCase;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRUserTransformerTest extends MCRUserTestCase {

    /**
     * Test method for {@link org.mycore.user2.utils.MCRUserTransformer#buildMCRUser(org.jdom2.Element)}.
     * @throws IOException 
     * @throws JAXBException 
     */
    @Test
    public final void testBuildMCRUser() throws IOException, JAXBException {
        Element input = MCRURIResolver.instance().resolve("resource:test-user.xml");
        MCRUser mcrUser = MCRUserTransformer.buildMCRUser(input);
        Document output = MCRUserTransformer.buildExportableXML(mcrUser);
        XMLOutputter xout = new XMLOutputter(Format.getPrettyFormat());
        xout.output(input, System.out);
        System.out.println();
        xout.output(output, System.out);
        assertTrue("Input element is not the same as outputElement",
            MCRXMLHelper.deepEqual(input, output.getRootElement()));
    }

    @XmlRootElement(name = "root")
    @XmlAccessorType(XmlAccessType.NONE)
    public static class HashMapTest2 {

        public Map<String, String> map = new HashMap<String, String>();

        @XmlElement(name = "entry")
        public MapEntry[] getMap() {
            List<MapEntry> list = new ArrayList<MapEntry>();
            for (Entry<String, String> entry : map.entrySet()) {
                MapEntry mapEntry = new MapEntry();
                mapEntry.key = entry.getKey();
                mapEntry.value = entry.getValue();
                list.add(mapEntry);
            }
            return list.toArray(new MapEntry[list.size()]);
        }

        public void setMap(MapEntry[] arr) {
            for (MapEntry entry : arr) {
                this.map.put(entry.key, entry.value);
            }
        }

        public static class MapEntry {
            @XmlAttribute
            public String key;

            @XmlValue
            public String value;
        }

    }

    @Test
    public void main() throws Exception {
        HashMapTest2 mp = new HashMapTest2();
        mp.map.put("key1", "value1");
        mp.map.put("key2", "value2");

        JAXBContext jc = JAXBContext.newInstance(HashMapTest2.class);
        Marshaller m = jc.createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        m.marshal(mp, System.out);

        Unmarshaller u = jc.createUnmarshaller();
        String xmlStr = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><root><entry key=\"key2\">value2</entry><entry key=\"key1\">value1</entry></root>";
        HashMapTest2 mp2 = (HashMapTest2) u.unmarshal(new StreamSource(new StringReader(xmlStr.toString())));
        m.marshal(mp2, System.out);
    }
}
