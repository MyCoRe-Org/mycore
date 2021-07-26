/*
 * $Revision$ $Date$
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
package org.mycore.oai;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.ProcessingInstruction;
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

    private static final long serialVersionUID = 1L;

    protected static final Logger LOGGER = LogManager.getLogger(MCROAIDataProvider.class);

    /**
     * Map of all MyCoRe oai adapter.
     */
    private static Map<String, MCROAIAdapter> ADAPTER_MAP;

    private static final OAIXMLOutputProcessor OAI_XML_OUTPUT_PROCESSOR = new OAIXMLOutputProcessor();

    static {
        ADAPTER_MAP = new HashMap<>();
    }

    private String myBaseURL;

    private ServiceLoader<OAIProvider> oaiAdapterServiceLoader;

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
        oaiProvider.setAdapter(getOAIAdapter());
        // handle request
        OAIResponse oaiResponse = oaiProvider.handleRequest(oaiRequest);
        // build response
        Element xmlRespone = oaiResponse.toXML();
        // fire
        job.getResponse().setContentType("text/xml; charset=UTF-8");
        XMLOutputter xout = new XMLOutputter(Format.getPrettyFormat(), OAI_XML_OUTPUT_PROCESSOR);
        xout.output(addXSLStyle(new Document(xmlRespone)), job.getResponse().getOutputStream());
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
        StringBuilder log = new StringBuilder(this.getServletName());
        for (Object o : req.getParameterMap().keySet()) {
            String name = (String) o;
            for (String value : req.getParameterValues(name)) {
                log.append(" ").append(name).append("=").append(value);
            }
        }
        LOGGER.info(log.toString());
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
        MCROAIAdapter oaiAdapter = ADAPTER_MAP.get(oaiAdapterKey);
        if (oaiAdapter == null) {
            synchronized (this) {
                // double check because of synchronize block
                oaiAdapter = ADAPTER_MAP.get(oaiAdapterKey);
                if (oaiAdapter == null) {
                    String adapter = MCROAIAdapter.PREFIX + oaiAdapterKey + ".Adapter";
                    oaiAdapter = MCRConfiguration2.<MCROAIAdapter>getInstanceOf(adapter)
                        .orElseGet(MCROAIAdapter::new);
                    oaiAdapter.init(this.myBaseURL, oaiAdapterKey);
                    ADAPTER_MAP.put(oaiAdapterKey, oaiAdapter);
                }
            }
        }
        return oaiAdapter;
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
