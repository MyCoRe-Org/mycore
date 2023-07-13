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
import java.util.Objects;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;

import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.mycore.common.MCRConstants;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRJAXBContent;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.common.content.transformer.MCRContentTransformer;
import org.mycore.common.content.transformer.MCRContentTransformerFactory;
import org.mycore.common.xml.MCRXMLParserFactory;
import org.mycore.mods.merger.MCRMergeTool;
import org.mycore.orcid2.MCRORCIDTransformerHelper;
import org.mycore.orcid2.exception.MCRORCIDTransformationException;
import org.orcid.jaxb.model.common.CitationType;
import org.orcid.jaxb.model.v3.release.record.Citation;
import org.orcid.jaxb.model.v3.release.record.Work;
import org.orcid.jaxb.model.v3.release.record.summary.WorkSummary;

/**
 * Provides helper functions to transform between Work or WorkSummary and MODS MCRContent.
 */
public class MCRORCIDWorkTransformerHelper {

    private static final MCRContentTransformer T_MODS_WORK
        = MCRContentTransformerFactory.getTransformer("MODS2ORCIDv3Work");

    private static final MCRContentTransformer T_WORK_MODS
        = MCRContentTransformerFactory.getTransformer("BaseORCIDv3Work2MODS");

    private static final MCRContentTransformer T_SUMMARY_MODS
        = MCRContentTransformerFactory.getTransformer("BaseORCIDv3WorkSummary2MODS");

    private static JAXBContext context = null;

    static {
        try {
            context = JAXBContext.newInstance(new Class[] { Work.class, WorkSummary.class });
        } catch (JAXBException e) {
            throw new IllegalArgumentException("Could not init jaxb context");
        }
    }

    /**
     * Transforms MODS MCRContent to Work.
     * 
     * @param content the MODS MCRContent
     * @return the Work
     * @throws MCRORCIDTransformationException if transformation failed
     */
    public static Work transformContent(MCRContent content) {
        try {
            return unmarshalWork(new MCRJDOMContent(MCRXMLParserFactory.getValidatingParser()
				    .parseXML(T_MODS_WORK.transform(content))));
        } catch (IOException | JDOMException e) {
            throw new MCRORCIDTransformationException(e);
        }
    }

    /**
     * Transforms Work to MODS MCRContent.
     * Merges BibLaTeX using transformer
     * 
     * @param work the Work
     * @return the MODS MCRContent
     * @throws MCRORCIDTransformationException if transformation failed
     */
    public static MCRContent transformWork(Work work) {
        checkContext();
        final MCRJAXBContent<Work> workContent = new MCRJAXBContent(context, work);
        Element mods = null;
        try {
            mods = T_WORK_MODS.transform(workContent).asXML().detachRootElement()
                .getChild("mods", MCRConstants.MODS_NAMESPACE).detach();
        } catch (IOException | JDOMException e) {
            throw new MCRORCIDTransformationException(e);
        }
        final Citation citation = work.getWorkCitation();
        if (citation != null && Objects.equals(citation.getWorkCitationType(), CitationType.BIBTEX)) {
            final Element modsBibTeX = MCRORCIDTransformerHelper.transformBibTeXToMODS(citation.getCitation());
            MCRMergeTool.merge(mods, modsBibTeX);
        }
        return new MCRJDOMContent(mods);
    }

    /**
     * Transforms WorkSummary to mods MCRContent.
     * 
     * @param work the WorkSummary
     * @return the MODS MCRContent
     * @throws MCRORCIDTransformationException if transformation failed
     */
    public static MCRContent transformWorkSummary(WorkSummary work) {
        checkContext();
        final MCRJAXBContent<WorkSummary> workContent = new MCRJAXBContent(context, work);
        Element mods = null;
        try {
            mods = T_SUMMARY_MODS.transform(workContent).asXML().detachRootElement()
                .getChild("mods", MCRConstants.MODS_NAMESPACE).detach();
        } catch (IOException | JDOMException e) {
            throw new MCRORCIDTransformationException(e);
        }
        return new MCRJDOMContent(mods);
    }

    private static Work unmarshalWork(MCRContent content) {
        checkContext();
        try {
            final Unmarshaller unmarshaller = context.createUnmarshaller();
            return (Work) unmarshaller.unmarshal(content.getInputSource());
        } catch(IOException | JAXBException e) {
            throw new MCRORCIDTransformationException(e);
        }
    }

    private static void checkContext() {
        if (context == null) {
            throw new MCRORCIDTransformationException("Jaxb context is not initialized");
        }
    }
}
