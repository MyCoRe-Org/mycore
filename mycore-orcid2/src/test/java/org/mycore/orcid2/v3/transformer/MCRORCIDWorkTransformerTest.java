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

import java.net.URL;

import jakarta.xml.bind.JAXBContext;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;
import org.jdom2.output.XMLOutputter;
import org.junit.Test;
import org.mycore.common.MCRTestCase;
import org.orcid.jaxb.model.v3.release.record.Work;

public class MCRORCIDWorkTransformerTest extends MCRTestCase {

    private static final Logger LOGGER = LogManager.getLogger();

    @Test
    public void testTransformContent() throws Exception {
        final JAXBContext jaxbContext = JAXBContext.newInstance(Work.class);
        final URL inputUrl = MCRORCIDWorkTransformerTest.class.getResource("/work_example.xml");
        final Work work = (Work) jaxbContext.createUnmarshaller().unmarshal(inputUrl);
        LOGGER.info(work);
        final MCRORCIDWorkTransformer transformer = MCRORCIDWorkTransformer.getInstance();
        final Element mods = transformer.transformToMODS(work);
        LOGGER.info(new XMLOutputter().outputString(mods));
        final Work result = transformer.transformToWork(mods);
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
}
