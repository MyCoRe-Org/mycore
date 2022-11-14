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
import jakarta.xml.bind.Unmarshaller;

import org.jdom2.JDOMException;
import org.mycore.common.MCRException;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRStringContent;
import org.mycore.common.content.transformer.MCRContentTransformer;
import org.mycore.common.content.transformer.MCRContentTransformerFactory;
import org.orcid.jaxb.model.common.CitationType;
import org.orcid.jaxb.model.v3.release.record.Citation;
import org.orcid.jaxb.model.v3.release.record.Work;
import org.xml.sax.SAXException;

public class MCRORCIDWorkTransformer extends org.mycore.orcid2.transformer.MCRORCIDWorkTransformer<Work> {

    private static final MCRContentTransformer T_WORK2MCR
        = MCRContentTransformerFactory.getTransformer("v3Work2MyCoRe");

    private static final MCRContentTransformer T_MCR2WORK
        = MCRContentTransformerFactory.getTransformer("MyCoRe2v3Work");

    private final JAXBContext jaxbContext;

    protected MCRORCIDWorkTransformer() throws MCRException {
        try {
            jaxbContext = JAXBContext.newInstance(Work.class);
        } catch (JAXBException e) {
            throw new MCRException("Could not initialize JAXBContext.", e);
        }
    }

    public static MCRORCIDWorkTransformer getInstance() {
        return LazyInstanceHelper.INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected MCRContent transform(Work work) throws JAXBException, JDOMException, SAXException, IOException {
        final Marshaller marshaller = jaxbContext.createMarshaller();
        final StringWriter stringWriter = new StringWriter();
        marshaller.marshal(work, stringWriter);
        return T_WORK2MCR.transform(new MCRStringContent(stringWriter.toString()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Work transform(MCRContent content) throws JAXBException, IOException {
        MCRContent transformed = T_MCR2WORK.transform(content);
        final Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        return (Work) unmarshaller.unmarshal(transformed.getInputStream());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getBibTeX(Work work) {
        final Citation citation = work.getWorkCitation();
        if (citation != null && CitationType.BIBTEX.equals(citation.getWorkCitationType())) {
            return citation.getCitation();
        }
        return null;
    }

    private static class LazyInstanceHelper {
        static final MCRORCIDWorkTransformer INSTANCE = new MCRORCIDWorkTransformer();
    }
}
