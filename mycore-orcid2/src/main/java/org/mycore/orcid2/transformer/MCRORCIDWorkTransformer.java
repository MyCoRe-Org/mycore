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

package org.mycore.orcid2.transformer;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.mycore.common.MCRConstants;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRJDOMContent;
import org.mycore.common.content.MCRStringContent;
import org.mycore.common.content.transformer.MCRContentTransformer;
import org.mycore.common.content.transformer.MCRContentTransformerFactory;
import org.mycore.mods.merger.MCRMergeTool;
import org.xml.sax.SAXException;

import jakarta.xml.bind.JAXBException;

/**
 * Abstract class for transformations of work elements.
 */
public abstract class MCRORCIDWorkTransformer<T> {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final MCRContentTransformer T_BIBTEX2MODS
        = MCRContentTransformerFactory.getTransformer("BibTeX2MODS");

    /**
     * Transforms work to mods Element.
     * 
     * @param work the work
     * @return the work as mods Element
     * @throws JAXBException {@inheritDoc}
     * @throws JDOMException {@inheritDoc}
     * @throws SAXException {@inheritDoc}
     * @throws IOException {@inheritDoc}
     */
    public Element transformToMODS(T work) throws JAXBException, JDOMException, SAXException, IOException {
        final MCRContent transformed = transform(work);
        final Element mods
            = transformed.asXML().detachRootElement().getChild("mods", MCRConstants.MODS_NAMESPACE).detach();
        final String bibTex = getBibTeX(work);
        if (bibTex != null) {
            try {
                final Element modsBibTex = convertBibTeXToMODS(bibTex);
                if (modsBibTex != null) {
                    MCRMergeTool.merge(mods, modsBibTex);
                }
            } catch (IOException | JDOMException | SAXException e) {
                LOGGER.warn("Exception parsing BibTex: {} {}", bibTex, e.getMessage());
            }
        }
        return mods;
    }

    /**
     * Transforms Element to work.
     * 
     * @param element the element
     * @return transformed work
     * @throws JAXBException {@inheritDoc}
     * @throws IOException {@inheritDoc}
     */
    public T transformToWork(Element element) throws JAXBException, IOException {
        return transformToWork(new MCRJDOMContent(element));
    }

    /**
     * Transforms MCRContent to work.
     * 
     * @param content the content
     * @return transformed work
     * @throws JAXBException {@inheritDoc}
     * @throws IOException {@inheritDoc}
     */
    public T transformToWork(MCRContent content) throws JAXBException, IOException {
        return transform(content);
    }

    /**
     * Transforms BibTeX string to mods Element.
     *
     * @param bibTeX bibTeX string
     * @return bibTeX as mods Element
     * @throws IOException {@inheritDoc}
     * @throws JDOMException {@inheritDoc}
     * @throws SAXException {@inheritDoc}
     */
    public static Element convertBibTeXToMODS(String bibTeX) throws IOException, JDOMException, SAXException {
        final MCRContent result = T_BIBTEX2MODS.transform(new MCRStringContent(bibTeX));
        final Element modsCollection = result.asXML().getRootElement();
        final Element modsFromBibTeX = modsCollection.getChild("mods", MCRConstants.MODS_NAMESPACE);
        // Remove mods:extension containing the original BibTeX:
        modsFromBibTeX.removeChildren("extension", MCRConstants.MODS_NAMESPACE);
        return modsFromBibTeX;
    }

    /**
     * Tranforms genric work to MCRContent.
     *
     * @param work the work
     * @return transformed work
     * @throws JAXBException {@inheritDoc}
     * @throws JDOMException {@inheritDoc}
     * @throws SAXException {@inheritDoc}
     * @throws IOException {@inheritDoc}
     */
    abstract protected MCRContent transform(T work) throws JAXBException, JDOMException, SAXException, IOException;

    /**
     * Tranforms MCRContent to generic work.
     *
     * @param content the content
     * @return genric work
     * @throws JAXBException {@inheritDoc}
     * @throws IOException {@inheritDoc}
     */
    abstract protected T transform(MCRContent content) throws JAXBException, IOException;

    /**
     * Extracts BibTex String from generic work.
     *
     * @param work genric work
     * @return BibTex String or null
     */
    abstract protected String getBibTeX(T work);
}
