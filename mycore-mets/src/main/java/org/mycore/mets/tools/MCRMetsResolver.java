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

package org.mycore.mets.tools;

import java.nio.file.Files;
import java.util.Collection;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.transform.JDOMSource;
import org.mycore.access.MCRAccessManager;
import org.mycore.common.content.MCRPathContent;
import org.mycore.datamodel.common.MCRLinkTableManager;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.niofs.MCRPath;
import org.mycore.mets.model.MCRMETSGeneratorFactory;

/**
 * returns a structured METS document for any valid MyCoRe ID (object or
 * derivate). May return empty &lt;mets:mets/&gt; if no derivate is present. No
 * metadata is attached.
 * 
 * @author Thomas Scheffler (yagee)
 */
public class MCRMetsResolver implements URIResolver {
    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public Source resolve(String href, String base) throws TransformerException {
        String id = href.substring(href.indexOf(':') + 1);
        LOGGER.debug("Reading METS for ID {}", id);
        MCRObjectID objId = MCRObjectID.getInstance(id);
        if (!objId.getTypeId().equals(MCRDerivate.OBJECT_TYPE)) {
            String derivateID = getDerivateFromObject(id);
            if (derivateID == null) {
                return new JDOMSource(new Element("mets", Namespace.getNamespace("mets", "http://www.loc.gov/METS/")));
            }
            id = derivateID;
        }
        MCRPath metsPath = MCRPath.getPath(id, "/mets.xml");
        try {
            if (Files.exists(metsPath)) {
                //TODO: generate new METS Output
                //ignoreNodes.add(metsFile);
                return new MCRPathContent(metsPath).getSource();
            }
            Document mets = MCRMETSGeneratorFactory.create(MCRPath.getPath(id, "/")).generate().asDocument();
            return new JDOMSource(mets);
        } catch (Exception e) {
            throw new TransformerException(e);
        }
    }

    private String getDerivateFromObject(String id) {
        Collection<String> derivates = MCRLinkTableManager.getInstance().getDestinationOf(id, "derivate");
        for (String derID : derivates) {
            if (MCRAccessManager.checkDerivateDisplayPermission(derID)) {
                return derID;
            }
        }
        return null;
    }

}
