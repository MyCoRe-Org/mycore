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

package org.mycore.component.fo.common.content.xml;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.mycore.common.MCRException;
import org.mycore.common.content.transformer.MCRContentTransformer;
import org.mycore.common.content.transformer.MCRTransformerPipe;
import org.mycore.common.content.transformer.MCRXSLTransformer;
import org.mycore.common.xml.MCRLayoutTransformerFactory;
import org.mycore.component.fo.common.content.transformer.MCRFopper;
import org.xml.sax.SAXException;

/**
 * This class acts as a {@link org.mycore.common.content.transformer.MCRContentTransformer} factory for 
 * {@link org.mycore.common.xml.MCRLayoutService}.
 * @author Thomas Scheffler (yagee)
 * @author Sebastian Hofmann
 *
 */
public class MCRLayoutTransformerFoFactory extends MCRLayoutTransformerFactory {

    private static final String APPLICATION_PDF = "application/pdf";

    /** Map of transformer instances by ID */
    private static Map<String, MCRContentTransformer> transformers = new ConcurrentHashMap<>();

    private static MCRFopper fopper = new MCRFopper();

    @Override
    public MCRContentTransformer getTransformer(String id) {
        return transformers.computeIfAbsent(id, (transformerId) -> {
            try {
                MCRContentTransformer transformer = super.getTransformer(transformerId);
                if (MCRLayoutTransformerFoFactory.NOOP_TRANSFORMER.equals(transformer) ||
                    getConfiguredTransformer(id).isPresent()) {
                    return transformer;
                }

                if (APPLICATION_PDF.equals(transformer.getMimeType())) {
                    return new MCRTransformerPipe(transformer, fopper);
                }

                return transformer;
            } catch (Exception e) {
                throw new MCRException("Error while transforming!", e);
            }
        });
    }

    @Override
    protected boolean isXMLOutput(String outputMethod, MCRXSLTransformer transformerTest)
        throws ParserConfigurationException, TransformerException, SAXException {
        return super.isXMLOutput(outputMethod, transformerTest) &&
            !APPLICATION_PDF.equals(transformerTest.getMimeType());
    }

}
