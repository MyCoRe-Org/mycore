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
import org.mycore.common.xml.MCRXMLFunctions;
import org.mycore.common.xsl.MCRXSLResourceHelper;
import org.mycore.datamodel.metadata.MCRXMLConstants;
import org.mycore.resource.uriresolver.MCRResourceURIResolver;

/**
 * {@link URIResolver} that resolves XSL import chains configured via properties.
 * <p>Each stylesheet in the chain must import this resolver to continue the chain:
 * <pre>
 *   &lt;xsl:import href="xslImport:{configId}:{filename.xsl}"/&gt;
 * </pre>
 * The import order is defined by:
 * <pre>
 *   MCR.URIResolver.xslImports.{configId}=first.xsl,second.xsl
 * </pre>
 */
public class MCRXSLImportURIResolver implements URIResolver {

    private static final Logger LOGGER = LogManager.getLogger();

    private final URIResolver fallback;

    public MCRXSLImportURIResolver() {
        fallback = new MCRResourceURIResolver();
    }

    /**
     * Resolves the next XSL stylesheet in the import chain for the given config ID and file.
     * <p>If no further import step exists, an empty {@code <xsl:stylesheet>} element is returned
     * to terminate the chain cleanly.
     * <p>URI Syntax:
     * <pre>
     *   &lt;scheme&gt;:{configId}:{filename.xsl}
     * </pre>
     * <p>Example request:
     * <pre>
     *   xslImport:components:second.xsl
     * </pre>
     * <p>Example response when the chain continues: the next XSL stylesheet as a source.
     * <p>Example response at end of chain:
     * <pre>{@code
     *   <xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"/>
     * }</pre>
     *
     * @param href the URI in the syntax above to resolve
     * @param base the base URI of the calling stylesheet, used to determine the XSL folder
     * @return a {@link Source} wrapping the next stylesheet in the chain, or an empty
     *         {@code <xsl:stylesheet>} element if the end of the chain is reached
     * @throws TransformerException if the next stylesheet cannot be resolved
     */
    @Override
    public Source resolve(String href, String base) throws TransformerException {
        String xslFolder = getXSLFolder(base);

        String importXSL = MCRXMLFunctions.nextImportStep(href.substring(href.indexOf(':') + 1));
        if (importXSL.isEmpty()) {
            LOGGER.debug("End of import queue: {}", href);
            Namespace xslNamespace = Namespace.getNamespace("xsl", "http://www.w3.org/1999/XSL/Transform");
            Element root = new Element("stylesheet", xslNamespace);
            root.setAttribute(MCRXMLConstants.VERSION, "1.0");
            return new JDOMSource(root);
        }
        LOGGER.debug("xslImport importing {}", importXSL);

        return fallback.resolve(MCRXSLResourceHelper.RESOURCE_PREFIX + xslFolder + "/" + importXSL, base);
    }

    private String getXSLFolder(String base) {
        String baseURI = MCRXSLResourceHelper.getXSLDirectory(base);
        if (Strings.CS.startsWith(baseURI, "resource:xsl/")) {
            return "xsl";
        } else if (Strings.CS.startsWith(baseURI, "resource:xslt/")) {
            return "xslt";
        } else {
            return MCRXSLResourceHelper.getXSLFolder();
        }
    }

}
