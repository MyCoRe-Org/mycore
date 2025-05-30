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

package org.mycore.user2.utils;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.transform.stream.StreamSource;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mycore.common.xml.MCRURIResolver;
import org.mycore.common.xml.MCRXMLHelper;
import org.mycore.test.MCRJPAExtension;
import org.mycore.test.MyCoReTest;
import org.mycore.user2.MCRUser;
import org.mycore.user2.MCRUserExtension;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlValue;

/**
 * @author Thomas Scheffler (yagee)
 */
@MyCoReTest
@ExtendWith({MCRJPAExtension.class, MCRUserExtension.class})
public class MCRUserTransformerTest {

    /**
     * Test method for {@link org.mycore.user2.utils.MCRUserTransformer#buildMCRUser(org.jdom2.Element)}.
     * @throws IOException
     */
    @Test
    public final void testBuildMCRUser() throws IOException {
        Element input = MCRURIResolver.obtainInstance().resolve("resource:test-user.xml");
        MCRUser mcrUser = MCRUserTransformer.buildMCRUser(input);
        Document output = MCRUserTransformer.buildExportableXML(mcrUser);
        XMLOutputter xout = new XMLOutputter(Format.getPrettyFormat());
        xout.output(input, System.out);
        System.out.println();
        xout.output(output, System.out);
        assertTrue(MCRXMLHelper.deepEqual(input, output.getRootElement()),
            "Input element is not the same as outputElement");
    }

    @XmlRootElement(name = "root")
    @XmlAccessorType(XmlAccessType.NONE)
    public static class HashMapTest2 {

        public Map<String, String> map = new HashMap<>();

        @XmlElement(name = "entry")
        public MapEntry[] getMap() {
            List<MapEntry> list = new ArrayList<>();
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
        String xmlStr =
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><root><entry key=\"key2\">value2</entry><entry key=\"key1\">value1</entry></root>";
        HashMapTest2 mp2 = (HashMapTest2) u.unmarshal(new StreamSource(new StringReader(xmlStr)));
        m.marshal(mp2, System.out);
    }
}
