/*
 * $Id$ $Revision$ $Date$
 * 
 * This file is part of *** M y C o R e *** See http://www.mycore.de/ for
 * details.
 * 
 * This program is free software; you can use it, redistribute it and / or
 * modify it under the terms of the GNU General Public License (GPL) as
 * published by the Free Software Foundation; either version 2 of the License or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program, in a file called gpl.txt or license.txt. If not, write to the
 * Free Software Foundation Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307 USA
 */

package org.mycore.mets.tools;

import java.nio.file.Files;
import java.util.Collection;
import java.util.HashSet;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.transform.JDOMSource;
import org.mycore.common.content.MCRPathContent;
import org.mycore.datamodel.common.MCRLinkTableManager;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.niofs.MCRPath;
import org.mycore.mets.model.MCRMETSGenerator;

/**
 * returns a structured METS document for any valid MyCoRe ID (object or
 * derivate). May return empty &lt;mets:mets/&gt; if no derivate is present. No
 * metadata is attached.
 * 
 * @author Thomas Scheffler (yagee)
 */
public class MCRMetsResolver implements URIResolver {
    private static final Logger LOGGER = LogManager.getLogger(MCRMetsResolver.class);

    @Override
    public Source resolve(String href, String base) throws TransformerException {
        String id = href.substring(href.indexOf(":") + 1);
        LOGGER.debug("Reading METS for ID " + id);
        MCRObjectID objId = MCRObjectID.getInstance(id);
        if (!objId.getTypeId().equals("derivate")) {
            String derivateID = getDerivateFromObject(id);
            if (derivateID == null) {
                return new JDOMSource(new Element("mets", Namespace.getNamespace("mets", "http://www.loc.gov/METS/")));
            }
            id = derivateID;
        }
        MCRPath metsPath = MCRPath.getPath(id, "/mets.xml");
        HashSet<MCRPath> ignoreNodes = new HashSet<>();
        try {
            if (Files.exists(metsPath)) {
                //TODO: generate new METS Output
                //ignoreNodes.add(metsFile);
                return new MCRPathContent(metsPath).getSource();
            }
            Document mets = MCRMETSGenerator.getGenerator().getMETS(MCRPath.getPath(id, "/"), ignoreNodes).asDocument();
            return new JDOMSource(mets);
        } catch (Exception e) {
            throw new TransformerException(e);
        }
    }

    private String getDerivateFromObject(String id) {
        Collection<String> derivates = MCRLinkTableManager.instance().getDestinationOf(id, "derivate");
        for (String derID : derivates) {
            MCRDerivate der = MCRMetadataManager.retrieveMCRDerivate(MCRObjectID.getInstance(derID));
            if (der.getDerivate().isDisplayEnabled()) {
                return derID;
            }
        }
        return null;
    }

}
