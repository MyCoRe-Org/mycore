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

package org.mycore.common.xsl.uriresolver;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;

import org.apache.commons.lang3.Strings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.transform.JDOMSource;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.xml.MCRXMLFunctions;
import org.mycore.datamodel.metadata.MCRXMLConstants;

/**
 * Imports xsl files which are set in the mycore.properties file. Example:
 * MCR.URIResolver.xslImports.components=first.xsl,second.xsl Every file must import this URIResolver to form a
 * import chain:
 *
 * <pre>
 *  &lt;xsl:import href="xslImport:components:first.xsl"&gt;
 * </pre>
 * <p>
 * Returns a xsl file with the import as href.
 */
public class MCRXslImportResolver implements URIResolver {

    private static final Logger LOGGER = LogManager.getLogger();

    URIResolver fallback = new MCRResourceResolver();

    @Override
    public Source resolve(String href, String base) throws TransformerException {
        final String baseURI = MCRURIResolver.getParentDirectoryResourceURI(base);
        // set xslt folder
        final String xslFolder;
        if (Strings.CS.startsWith(baseURI, "resource:xsl/")) {
            xslFolder = "xsl";
        } else if (Strings.CS.startsWith(baseURI, "resource:xslt/")) {
            xslFolder = "xslt";
        } else {
            xslFolder = MCRConfiguration2.getStringOrThrow(MCRURIResolver.PROPERTY_XSL_FOLDER);
        }

        String importXSL = MCRXMLFunctions.nextImportStep(href.substring(href.indexOf(':') + 1));
        if (importXSL.isEmpty()) {
            LOGGER.debug("End of import queue: {}", href);
            Namespace xslNamespace = Namespace.getNamespace("xsl", "http://www.w3.org/1999/XSL/Transform");
            Element root = new Element("stylesheet", xslNamespace);
            root.setAttribute(MCRXMLConstants.VERSION, "1.0");
            return new JDOMSource(root);
        }
        LOGGER.debug("xslImport importing {}", importXSL);

        return fallback.resolve(MCRURIResolver.RESOURCE_PREFIX + xslFolder + "/" + importXSL, base);
    }

}
