/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
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

package org.mycore.wfc.actionmapping;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;

import org.mycore.common.xml.MCRURIResolver;
import org.mycore.wfc.MCRConstants;

import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRActionMappingsManager {

    public static MCRActionMappings getActionMappings() throws TransformerException, JAXBException {
        Source source = MCRURIResolver.obtainInstance().resolve("resource:actionmappings.xml", null);
        Unmarshaller unmarshaller = MCRConstants.JAXB_CONTEXT.createUnmarshaller();
        JAXBElement<MCRActionMappings> jaxbElement = unmarshaller.unmarshal(source, MCRActionMappings.class);
        return jaxbElement.getValue();
    }
}
