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

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;

import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.mycore.common.MCRConstants;
import org.mycore.common.MCRException;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRJAXBContent;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.common.content.transformer.MCRContentTransformer;
import org.mycore.common.content.transformer.MCRContentTransformerFactory;
import org.mycore.mods.merger.MCRMergeTool;
import org.mycore.orcid2.MCRORCIDTransformerHelper;
import org.orcid.jaxb.model.common.CitationType;
import org.orcid.jaxb.model.v3.release.record.Citation;
import org.orcid.jaxb.model.v3.release.record.Work;
import org.xml.sax.SAXException;

public class MCRORCIDWorkTransformerHelper {

    private static JAXBContext context = null;

    static {
        try {
            context = JAXBContext.newInstance(Work.class);
        } catch (JAXBException e) {
            throw new IllegalArgumentException("Could not init jaxb context");
        }
    }

    public static Work transformContent(MCRContent content) throws JAXBException, IOException {
        final MCRContentTransformer transformer = MCRContentTransformerFactory.getTransformer("MODS2ORCIDv3Work");
        final MCRContent transformedContent = transformer.transform(content);
        return unmarshalWork(transformedContent);
    }

    private static Work unmarshalWork(MCRContent content) throws IOException, JAXBException, MCRException {
        checkContext();
        final Unmarshaller unmarshaller = context.createUnmarshaller();
        return (Work) unmarshaller.unmarshal(content.getInputSource());
    }

    public static MCRContent transformWork(Work work)
        throws JAXBException, IOException, JDOMException, SAXException, MCRException {
        checkContext();
        final MCRJAXBContent<Work> workContent = new MCRJAXBContent(context, work);
        final MCRContentTransformer transformer = MCRContentTransformerFactory.getTransformer("BaseORCIDv3Work2MODS");
        final MCRContent transformed = transformer.transform(workContent);
        final Element mods
            = transformed.asXML().detachRootElement().getChild("mods", MCRConstants.MODS_NAMESPACE).detach();
        final Citation citation = work.getWorkCitation();
        if (citation != null && CitationType.BIBTEX.equals(citation.getWorkCitationType())) {
            final Element modsBibTeX = MCRORCIDTransformerHelper.transformBibTeXToMODS(citation.getCitation());
            MCRMergeTool.merge(mods, modsBibTeX);
        }
        return new MCRJDOMContent(mods);
    }

    private static void checkContext() throws MCRException {
        if (context == null) {
            throw new MCRException("Jaxb context is not inited");
        }
    }
}
