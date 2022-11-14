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

import java.io.IOException;
import java.io.StringWriter;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;

import org.jdom2.JDOMException;
import org.mycore.common.MCRException;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRStringContent;
import org.mycore.common.content.transformer.MCRContentTransformer;
import org.mycore.common.content.transformer.MCRContentTransformerFactory;
import org.orcid.jaxb.model.v3.release.record.summary.WorkSummary;
import org.xml.sax.SAXException;

public class MCRORCIDWorkSummaryTransformer extends org.mycore.orcid2.transformer.MCRORCIDWorkTransformer<WorkSummary> {

    private static final MCRContentTransformer T_WORK2MCR
        = MCRContentTransformerFactory.getTransformer("v3Work2MyCoRe");

    private final JAXBContext jaxbContext;

    protected MCRORCIDWorkSummaryTransformer() throws MCRException {
        try {
            jaxbContext = JAXBContext.newInstance(WorkSummary.class);
        } catch (JAXBException e) {
            throw new MCRException("Could not initialize JAXBContext.", e);
        }
    }

    public static MCRORCIDWorkSummaryTransformer getInstance() {
        return LazyInstanceHelper.INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected MCRContent transform(WorkSummary work) throws JAXBException, JDOMException, SAXException, IOException {
        final Marshaller marshaller = jaxbContext.createMarshaller();
        final StringWriter stringWriter = new StringWriter();
        marshaller.marshal(work, stringWriter);
        return T_WORK2MCR.transform(new MCRStringContent(stringWriter.toString()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected WorkSummary transform(MCRContent content) throws JAXBException, IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getBibTeX(WorkSummary work) {
        return null;
    }

    private static class LazyInstanceHelper {
        static final MCRORCIDWorkSummaryTransformer INSTANCE = new MCRORCIDWorkSummaryTransformer();
    }
}
