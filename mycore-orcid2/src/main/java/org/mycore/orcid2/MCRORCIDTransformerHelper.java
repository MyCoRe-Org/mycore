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

package org.mycore.orcid2;

import java.io.IOException;

import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.mycore.common.MCRConstants;
import org.mycore.common.content.MCRContent;
import org.mycore.common.content.MCRStringContent;
import org.mycore.common.content.transformer.MCRContentTransformer;
import org.mycore.common.content.transformer.MCRContentTransformerFactory;
import org.xml.sax.SAXException;

/**
 * Provides transformer methods.
 */ 
public class MCRORCIDTransformerHelper {

    private static final MCRContentTransformer T_BIBTEX2MODS
        = MCRContentTransformerFactory.getTransformer("BibTeX2MODS");

    /**
     * Converts BibTeX String to MODS.
     *
     * @param bibTeX BibTex String
     * @return MODS Element
     * @throws IOException
     * @throws JDOMException
     * @throws SAXException
     */
    public static Element transformBibTeXToMODS(String bibTeX) throws IOException, JDOMException, SAXException {
        final MCRContent result = T_BIBTEX2MODS.transform(new MCRStringContent(bibTeX));
        final Element modsCollection = result.asXML().getRootElement();
        final Element mods = modsCollection.getChild("mods", MCRConstants.MODS_NAMESPACE);
        // Remove mods:extension containing the original BibTeX:
        mods.removeChildren("extension", MCRConstants.MODS_NAMESPACE);
        return mods;
    }
}
