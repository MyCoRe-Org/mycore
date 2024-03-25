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

package org.mycore.mods.csl;

import de.undercouch.citeproc.csl.CSLItemData;
import de.undercouch.citeproc.csl.CSLItemDataBuilder;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.junit.Assert;
import org.junit.Test;
import org.mycore.common.MCRTestCase;

import java.io.IOException;
import java.io.StringReader;

public class MCRModsItemDataProviderTest extends MCRTestCase {

    @Test
    public void testProcessModsPart() throws IOException, JDOMException {

        final String testData = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<mycoreobject xmlns:xlink=\"http://www.w3.org/1999/xlink\" " +
            "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
            " xsi:noNamespaceSchemaLocation=\"datamodel-mods.xsd\" ID=\"bibthk_mods_00001066\">\n" +
            "  <metadata>\n" +
            "    <def.modsContainer class=\"MCRMetaXML\" heritable=\"false\" notinherit=\"true\">\n" +
            "      <modsContainer inherited=\"0\">\n" +
            "        <mods:mods xmlns:mods=\"http://www.loc.gov/mods/v3\">\n" +
            "          <mods:relatedItem xlink:href=\"bibthk_mods_00001057\" type=\"host\">\n" +
            "            <mods:part>\n" +
            "              <mods:detail type=\"volume\">\n" +
            "                <mods:number>80</mods:number>\n" +
            "              </mods:detail>\n" +
            "              <mods:extent unit=\"pages\">\n" +
            "                <mods:start>711</mods:start>\n" +
            "                <mods:end>718</mods:end>\n" +
            "              </mods:extent>\n" +
            "            </mods:part>\n" +
            "          </mods:relatedItem>\n" +
            "        </mods:mods>\n" +
            "      </modsContainer>\n" +
            "    </def.modsContainer>\n" +
            "  </metadata>\n" +
            "</mycoreobject>\n";

        final String testData2 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<mycoreobject xmlns:xlink=\"http://www.w3.org/1999/xlink\" " +
            "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
            " xsi:noNamespaceSchemaLocation=\"datamodel-mods.xsd\" ID=\"bibthk_mods_00001056\">\n" +
            "  <metadata>\n" +
            "    <def.modsContainer class=\"MCRMetaXML\" heritable=\"false\" notinherit=\"true\">\n" +
            "      <modsContainer inherited=\"0\">\n" +
            "        <mods:mods xmlns:mods=\"http://www.loc.gov/mods/v3\">\n" +
            "<mods:relatedItem type=\"series\">\n" +
            "<mods:titleInfo>\n" +
            "<mods:title>Security and Cryptology</mods:title>\n" +
            "</mods:titleInfo>\n" +
            "<mods:part>\n" +
            "<mods:detail type=\"volume\">\n" +
            "<mods:number>11875</mods:number>\n" +
            "</mods:detail>\n" +
            "</mods:part>\n" +
            "</mods:relatedItem>" +
            "        </mods:mods>\n" +
            "      </modsContainer>\n" +
            "    </def.modsContainer>\n" +
            "  </metadata>\n" +
            "</mycoreobject>\n";

        CSLItemData build1 = testModsPart(testData);
        CSLItemData build2 = testModsPart(testData2);

        Assert.assertEquals("Volumes should equal", "80", build1.getVolume());
        Assert.assertEquals("start should equal", "711", build1.getPageFirst());
        Assert.assertEquals("end should equal", "711-718", build1.getPage());

        Assert.assertEquals("Volumes should equal", "11875", build2.getVolume());

    }

    @Test
    public void testConference() throws IOException, JDOMException {
        String conferenceTitle = "International Renewable Energy Storage Conference (IRES) 2021";
        String testData = "<mycoreobject xmlns:xlink=\"http://www.w3.org/1999/xlink\" " +
            "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
            " xsi:noNamespaceSchemaLocation=\"datamodel-mods.xsd\" ID=\"bibthk_mods_00001056\">\n" +
            "  <metadata>\n" +
            "    <def.modsContainer class=\"MCRMetaXML\" heritable=\"false\" notinherit=\"true\">\n" +
            "      <modsContainer inherited=\"0\">\n" +
            "        <mods:mods xmlns:mods=\"http://www.loc.gov/mods/v3\">\n" +
            "<mods:name type=\"conference\">\n" +
            "<mods:namePart>" + conferenceTitle + "</mods:namePart>\n" +
            "</mods:name>" +
            "        </mods:mods>\n" +
            "      </modsContainer>\n" +
            "    </def.modsContainer>\n" +
            "  </metadata>\n" +
            "</mycoreobject>\n";

        CSLItemData build1 = testModsNames(testData);
        Assert.assertEquals("Conference should be equal", conferenceTitle, build1.getEvent());
    }

    @Test
    public void testInventor() throws IOException, JDOMException {
        String familyName = "Rolfer";
        String givenName = "Rolf";
        String testData2 = "<mycoreobject xmlns:xlink=\"http://www.w3.org/1999/xlink\" " +
            "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
            " xsi:noNamespaceSchemaLocation=\"datamodel-mods.xsd\" ID=\"bibthk_mods_00001056\">\n" +
            "  <metadata>\n" +
            "    <def.modsContainer class=\"MCRMetaXML\" heritable=\"false\" notinherit=\"true\">\n" +
            "      <modsContainer inherited=\"0\">\n" +
            "        <mods:mods xmlns:mods=\"http://www.loc.gov/mods/v3\">\n" +
            "<mods:name type=\"personal\">\n" +
            "<mods:role>\n" +
            "<mods:roleTerm type=\"code\" authority=\"marcrelator\">inv</mods:roleTerm>\n" +
            "</mods:role>\n" +
            "<mods:namePart type=\"given\">" + givenName + "</mods:namePart>\n" +
            "<mods:namePart type=\"family\">" + familyName + "</mods:namePart>\n" +
            "</mods:name>" + "        </mods:mods>\n" +
            "      </modsContainer>\n" +
            "    </def.modsContainer>\n" +
            "  </metadata>\n" +
            "</mycoreobject>\n";
        CSLItemData build2 = testModsNames(testData2);
        Assert.assertEquals("Pantent Inventor should author (family)", familyName, build2.getAuthor()[0].getFamily());
        Assert.assertEquals("Pantent Inventor should author (given)", givenName, build2.getAuthor()[0].getGiven());
    }

    private CSLItemData testModsPart(String testData) throws JDOMException, IOException {
        MCRModsItemDataProvider midp = new MCRModsItemDataProvider();
        Document testDataDoc = new SAXBuilder().build(new StringReader(testData));

        CSLItemDataBuilder dataBuilder = new CSLItemDataBuilder();
        midp.addContent(testDataDoc);
        midp.processModsPart(dataBuilder);
        CSLItemData build = dataBuilder.build();
        return build;
    }

    private CSLItemData testModsNames(String testData) throws JDOMException, IOException {
        MCRModsItemDataProvider midp = new MCRModsItemDataProvider();
        Document testDataDoc = new SAXBuilder().build(new StringReader(testData));

        CSLItemDataBuilder dataBuilder = new CSLItemDataBuilder();
        midp.addContent(testDataDoc);
        midp.processNames(dataBuilder);
        CSLItemData build = dataBuilder.build();
        return build;
    }
}
