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

package org.mycore.orcid2.v3.transformer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.StringReader;
import java.net.URL;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.XMLOutputter;
import org.junit.Test;
import org.mycore.common.MCRTestCase;
import org.mycore.common.content.MCRJDOMContent;
import org.orcid.jaxb.model.v3.release.record.Work;

import jakarta.xml.bind.JAXBContext;

public class MCRORCIDWorkTransformerHelperTest extends MCRTestCase {

    private static final Logger LOGGER = LogManager.getLogger();

    @Test
    public void testTransformContent() throws Exception {
        final JAXBContext jaxbContext = JAXBContext.newInstance(Work.class);
        final URL inputUrl = MCRORCIDWorkTransformerHelperTest.class.getResource("/work_example.xml");
        final Work work = (Work) jaxbContext.createUnmarshaller().unmarshal(inputUrl);
        LOGGER.info(work);

        final Element mods = MCRORCIDWorkTransformerHelper.transformWork(work).asXML().detachRootElement();
        LOGGER.info(new XMLOutputter().outputString(mods));
        final Work result = MCRORCIDWorkTransformerHelper.transformContent(new MCRJDOMContent(mods));
        LOGGER.info(result);
        assertEquals(work.getWorkType(), result.getWorkType());
        LOGGER.warn("Skipping contributors...");
        // assertEquals(work.getWorkContributors(), result.getWorkContributors());
        LOGGER.warn("Skipping journal title...");
        // assertEquals(work.getJournalTitle(), result.getJournalTitle());
        assertEquals(work.getPublicationDate(), result.getPublicationDate());
        assertEquals(work.getShortDescription(), result.getShortDescription());
        assertEquals(work.getUrl(), result.getUrl());
        assertEquals(work.getLanguageCode(), result.getLanguageCode());
        assertEquals(work.getWorkExternalIdentifiers(), result.getWorkExternalIdentifiers());
    }

    @Test
    public void testTransformContentJournalTitle() throws Exception {
        final Work work = transformMods("""
            <mods:relatedItem type="host">
              <mods:titleInfo type="abbreviated">
                <mods:title>J. Test</mods:title>
              </mods:titleInfo>
              <mods:titleInfo>
                <mods:nonSort>The</mods:nonSort>
                <mods:title>Journal of Testing</mods:title>
                <mods:subTitle>Theory and Practice</mods:subTitle>
              </mods:titleInfo>
              <mods:titleInfo type="translated" xml:lang="de">
                <mods:title>Zeitschrift für Tests</mods:title>
              </mods:titleInfo>
            </mods:relatedItem>
            <mods:relatedItem type="series">
              <mods:titleInfo>
                <mods:title>Series Title</mods:title>
              </mods:titleInfo>
            </mods:relatedItem>
            <mods:relatedItem type="host">
              <mods:titleInfo>
                <mods:title>Second Host</mods:title>
              </mods:titleInfo>
            </mods:relatedItem>
            """);
        assertEquals("The Journal of Testing: Theory and Practice", work.getJournalTitle().getContent());
    }

    @Test
    public void testTransformContentJournalTitleFallback() throws Exception {
        final Work work = transformMods("""
            <mods:relatedItem type="host">
              <mods:titleInfo type="abbreviated">
                <mods:title>J. Test</mods:title>
              </mods:titleInfo>
            </mods:relatedItem>
            """);
        assertEquals("J. Test", work.getJournalTitle().getContent());
    }

    @Test
    public void testTransformContentWithoutJournalTitle() throws Exception {
        final Work work = transformMods("""
            <mods:relatedItem type="series">
              <mods:titleInfo>
                <mods:title>Series Title</mods:title>
              </mods:titleInfo>
            </mods:relatedItem>
            """);
        assertNull(work.getJournalTitle());
    }

    private static Work transformMods(String relatedItems) throws Exception {
        final String mods = """
            <mods:mods xmlns:mods="http://www.loc.gov/mods/v3">
              <mods:titleInfo>
                <mods:title>Title</mods:title>
              </mods:titleInfo>
              %s
            </mods:mods>
            """.formatted(relatedItems);
        final Element root = new SAXBuilder().build(new StringReader(mods)).detachRootElement();
        return MCRORCIDWorkTransformerHelper.transformContent(new MCRJDOMContent(root));
    }
}
