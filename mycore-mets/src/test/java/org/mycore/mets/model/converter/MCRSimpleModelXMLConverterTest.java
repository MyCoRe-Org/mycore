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

package org.mycore.mets.model.converter;

import java.util.Arrays;
import java.util.Collections;

import org.jdom2.Document;
import org.jdom2.Namespace;
import org.jdom2.filter.Filters;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.jdom2.xpath.XPathFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mycore.mets.model.simple.MCRMetsSimpleModel;

public class MCRSimpleModelXMLConverterTest {

    private static final String PATHS_TO_CHECK = "count(//mets:structLink/mets:smLink)=3;" +
        "count(//mets:structMap[@TYPE='LOGICAL']/mets:div[@LABEL='testRootLabel'])=1;" +
        "count(//mets:structMap[@TYPE='LOGICAL']/mets:div[@LABEL='testRootLabel']/mets:div[@LABEL='subSection1Label'])=1;"
        + "count(//mets:structMap[@TYPE='LOGICAL']/mets:div[@LABEL='testRootLabel']/mets:div[@LABEL='subSection2Label'])=1;"
        + "count(//mets:fileGrp[@USE='MASTER']/mets:file)=3;"
        + "count(//mets:fileGrp[@USE='ALTO']/mets:file)=3;"
        + "count(//mets:structMap[@TYPE='PHYSICAL']/mets:div[@TYPE='physSequence']/mets:div[@TYPE='page'])=3;"
        + "count(//mets:structMap[@TYPE='PHYSICAL']/mets:div[@TYPE='physSequence']/mets:div[@TYPE='page']/mets:fptr)=6;"
        + "count(//mets:structMap[@TYPE='PHYSICAL']/mets:div[@TYPE='physSequence']/mets:div[@TYPE='page' and "
        + "contains(@CONTENTIDS, 'URN:special-urn')])=3;";

    private MCRMetsSimpleModel metsSimpleModel;

    @Before
    public void createModel() {
        metsSimpleModel = MCRMetsTestUtil.buildMetsSimpleModel();
    }

    @Test
    public void testToXML() throws Exception {
        Document document = MCRSimpleModelXMLConverter.toXML(metsSimpleModel);

        XPathFactory xPathFactory = XPathFactory.instance();
        String documentAsString = new XMLOutputter(Format.getPrettyFormat()).outputString(document);

        Arrays.asList(PATHS_TO_CHECK.split(";")).stream()
            .map((String xpath) -> xPathFactory.compile(xpath, Filters.fboolean(), Collections.emptyMap(),
                Namespace.getNamespace("mets", "http://www.loc.gov/METS/")))
            .forEachOrdered(xPath -> {
                Boolean evaluate = xPath.evaluateFirst(document);
                Assert.assertTrue(
                    String.format("The xpath : %s is not true! %s %s", xPath, System.lineSeparator(), documentAsString),
                    evaluate);
            });
    }
}
