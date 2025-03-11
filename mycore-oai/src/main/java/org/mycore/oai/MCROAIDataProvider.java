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
package org.mycore.oai;

import java.io.IOException;
import java.io.Serial;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.ProcessingInstruction;
import org.jdom2.filter.ElementFilter;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.jdom2.output.support.AbstractXMLOutputProcessor;
import org.jdom2.output.support.FormatStack;
import org.jdom2.util.NamespaceStack;
import org.mycore.common.MCRClassTools;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.frontend.MCRFrontendUtil;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;
import org.mycore.oai.pmh.OAIConstants;
import org.mycore.oai.pmh.dataprovider.OAIAdapter;
import org.mycore.oai.pmh.dataprovider.OAIProvider;
import org.mycore.oai.pmh.dataprovider.OAIRequest;
import org.mycore.oai.pmh.dataprovider.OAIResponse;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Implements an OAI-PMH 2.0 Data Provider as a servlet.
 *
 * @author Matthias Eichner
 */
public class MCROAIDataProvider extends MCRServlet {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * Map of all MyCoRe oai adapter.
     */
    private static final Map<String, MCROAIAdapter> ADAPTER_MAP;

    private static final OAIXMLOutputProcessor OAI_XML_OUTPUT_PROCESSOR = new OAIXMLOutputProcessor();

    static {
        ADAPTER_MAP = new ConcurrentHashMap<>();
    }

    private String myBaseURL;

    private transient ServiceLoader<OAIProvider> oaiAdapterServiceLoader;

    @Override
    public void init() throws ServletException {
        super.init();
        oaiAdapterServiceLoader = ServiceLoader.load(OAIProvider.class, MCRClassTools.getClassLoader());
    }

    @Override
    protected void doGetPost(MCRServletJob job) throws Exception {
        HttpServletRequest request = job.getRequest();
        // get base url
        if (this.myBaseURL == null) {
            this.myBaseURL = MCRFrontendUtil.getBaseURL() + request.getServletPath().substring(1);
        }
        logRequest(request);
        // create new oai request
        OAIRequest oaiRequest = new OAIRequest(fixParameterMap(request.getParameterMap()));
        // create new oai provider
        OAIProvider oaiProvider = oaiAdapterServiceLoader
            .findFirst()
            .orElseThrow(() -> new ServletException("No implementation of " + OAIProvider.class + " found."));
        final OAIAdapter adapter = getOAIAdapter();
        oaiProvider.setAdapter(adapter);
        // handle request
        OAIResponse oaiResponse = oaiProvider.handleRequest(oaiRequest);
        // build response
        Element xmlResponse = oaiResponse.toXML();

        if (!(adapter instanceof MCROAIAdapter mcrAdapter) || mcrAdapter.moveNamespaceDeclarationsToRoot()) {
            moveNamespacesUp(xmlResponse);
        }

        // fire
        job.getResponse().setContentType("text/xml; charset=UTF-8");
        XMLOutputter xout = new XMLOutputter(Format.getPrettyFormat(), OAI_XML_OUTPUT_PROCESSOR);
        xout.output(addXSLStyle(new Document(xmlResponse)), job.getResponse().getOutputStream());
    }

    /**
     * Converts the servlet parameter map to deal with oaipmh api.
     *
     * @param pMap servlet parameter map
     * @return parameter map with generics and list
     */
    @SuppressWarnings("rawtypes")
    private Map<String, List<String>> fixParameterMap(Map pMap) {
        Map<String, List<String>> rMap = new HashMap<>();
        for (Object o : pMap.entrySet()) {
            Map.Entry entry = (Map.Entry) o;
            List<String> valueList = new ArrayList<>();
            Collections.addAll(valueList, (String[]) entry.getValue());
            rMap.put((String) entry.getKey(), valueList);
        }
        return rMap;
    }

    protected void logRequest(HttpServletRequest req) {
        LOGGER.info(() -> {
            StringBuilder log = new StringBuilder(this.getServletName());
            for (Object o : req.getParameterMap().keySet()) {
                String name = (String) o;
                for (String value : req.getParameterValues(name)) {
                    log.append(' ').append(name).append('=').append(value);
                }
            }
            return log.toString();
        });
    }

    /**
     * Add link to XSL stylesheet for displaying OAI response in web browser.
     */
    private Document addXSLStyle(Document doc) {
        String styleSheet = MCROAIAdapter.PREFIX + getServletName() + ".ResponseStylesheet";
        String xsl = MCRConfiguration2.getString(styleSheet).orElse("oai/oai2.xsl");
        if (!xsl.isEmpty()) {
            Map<String, String> pairs = new HashMap<>();
            pairs.put("type", "text/xsl");
            pairs.put("href", MCRFrontendUtil.getBaseURL() + xsl);
            doc.addContent(0, new ProcessingInstruction("xml-stylesheet", pairs));
        }
        return doc;
    }

    private OAIAdapter getOAIAdapter() {
        String oaiAdapterKey = getServletName();
        return ADAPTER_MAP.computeIfAbsent(oaiAdapterKey, k -> createOAIAdapter(oaiAdapterKey));
    }

    private MCROAIAdapter createOAIAdapter(String oaiAdapterKey) {
        MCROAIAdapter oaiAdapter;
        String adapter = MCROAIAdapter.PREFIX + oaiAdapterKey + ".Adapter";
        oaiAdapter = MCRConfiguration2.getInstanceOf(MCROAIAdapter.class, adapter)
            .orElseGet(() -> MCRConfiguration2.getInstanceOfOrThrow(
                MCROAIAdapter.class, MCROAIAdapter.PREFIX + "DefaultAdapter"));
        oaiAdapter.init(this.myBaseURL, oaiAdapterKey);
        return oaiAdapter;
    }

    /**
     * Moves all namespace declarations in the children of target to the target.
     *
     * @param target the namespace is bundled here
     */
    private void moveNamespacesUp(Element target) {
        Map<String, Namespace> existingNamespaces = getNamespaceMap(target);
        Map<String, Namespace> newNamespaces = new HashMap<>();
        target.getDescendants(new ElementFilter()).forEach(child -> {
            Map<String, Namespace> childNamespaces = getNamespaceMap(child);
            childNamespaces.forEach((prefix, ns) -> {
                if (existingNamespaces.containsKey(prefix) || newNamespaces.containsKey(prefix)) {
                    return;
                }
                newNamespaces.put(prefix, ns);
            });
        });
        newNamespaces.forEach((prefix, ns) -> target.addNamespaceDeclaration(ns));
    }

    private Map<String, Namespace> getNamespaceMap(Element element) {
        Map<String, Namespace> map = new HashMap<>();
        map.put(element.getNamespace().getPrefix(), element.getNamespace());
        element.getAdditionalNamespaces().forEach(ns -> map.put(ns.getPrefix(), ns));
        return map;
    }

    private static final class OAIXMLOutputProcessor extends AbstractXMLOutputProcessor {
        @Override
        protected void printElement(Writer out, FormatStack fstack, NamespaceStack nstack, Element element)
            throws IOException {
            //MCR-1866 use raw format if element is not in OAI namespace
            if (!element.getNamespace().equals(OAIConstants.NS_OAI)) {
                fstack.setTextMode(Format.TextMode.PRESERVE);
            }
            super.printElement(out, fstack, nstack, element);
        }
    }

}
