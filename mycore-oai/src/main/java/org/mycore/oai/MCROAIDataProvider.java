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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.ProcessingInstruction;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.frontend.MCRFrontendUtil;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;
import org.mycore.oai.pmh.dataprovider.OAIAdapter;
import org.mycore.oai.pmh.dataprovider.OAIRequest;
import org.mycore.oai.pmh.dataprovider.OAIResponse;
import org.mycore.oai.pmh.dataprovider.OAIXMLProvider;
import org.mycore.oai.pmh.dataprovider.jaxb.JAXBOAIProvider;

/**
 * Implements an OAI-PMH 2.0 Data Provider as a servlet.
 * 
 * @author Matthias Eichner
 */
public class MCROAIDataProvider extends MCRServlet {
    private static final long serialVersionUID = 1L;

    protected final static Logger LOGGER = LogManager.getLogger(MCROAIDataProvider.class);

    /**
     * Map of all MyCoRe oai adapter.
     */
    private static Map<String, MCROAIAdapter> mcrOAIAdapterMap;

    private String myBaseURL;

    static {
        mcrOAIAdapterMap = new HashMap<String, MCROAIAdapter>();
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
        OAIXMLProvider oaiProvider = new JAXBOAIProvider(getOAIAdapter());
        // handle request
        OAIResponse oaiResponse = oaiProvider.handleRequest(oaiRequest);
        // build response
        Element xmlRespone = oaiResponse.toXML();
        // fire
        job.getResponse().setContentType("text/xml; charset=UTF-8");
        XMLOutputter xout = new XMLOutputter();
        xout.setFormat(Format.getPrettyFormat().setEncoding("UTF-8"));
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
        Map<String, List<String>> rMap = new HashMap<String, List<String>>();
        for (Object o : pMap.entrySet()) {
            Map.Entry entry = (Map.Entry) o;
            List<String> valueList = new ArrayList<String>();
            Collections.addAll(valueList, (String[]) entry.getValue());
            rMap.put((String) entry.getKey(), valueList);
        }
        return rMap;
    }

    protected void logRequest(HttpServletRequest req) {
        StringBuilder log = new StringBuilder(this.getServletName());
        for (Object o : req.getParameterMap().keySet()) {
            String name = (String) o;
            for (String value : req.getParameterValues(name))
                log.append(" ").append(name).append("=").append(value);
        }
        LOGGER.info(log.toString());
    }

    /**
     * Add link to XSL stylesheet for displaying OAI response in web browser.
     */
    private Document addXSLStyle(Document doc) {
        String styleSheet = MCROAIAdapter.PREFIX + getServletName() + ".ResponseStylesheet";
        String xsl = MCRConfiguration.instance().getString(styleSheet, "oai/oai2.xsl");
        if (!xsl.isEmpty()) {
            Map<String, String> pairs = new HashMap<String, String>();
            pairs.put("type", "text/xsl");
            pairs.put("href", MCRFrontendUtil.getBaseURL() + xsl);
            doc.addContent(0, new ProcessingInstruction("xml-stylesheet", pairs));
        }
        return doc;
    }

    private OAIAdapter getOAIAdapter() {
        String oaiAdapterKey = getServletName();
        MCROAIAdapter oaiAdapter = mcrOAIAdapterMap.get(oaiAdapterKey);
        if (oaiAdapter == null) {
            synchronized (this) {
                // double check because of synchronize block
                oaiAdapter = mcrOAIAdapterMap.get(oaiAdapterKey);
                if (oaiAdapter == null) {
                    MCRConfiguration config = MCRConfiguration.instance();
                    String adapter = MCROAIAdapter.PREFIX + oaiAdapterKey + ".Adapter";
                    oaiAdapter = config.getInstanceOf(adapter, MCROAIAdapter.class.getName());
                    oaiAdapter.init(this.myBaseURL, oaiAdapterKey);
                    mcrOAIAdapterMap.put(oaiAdapterKey, oaiAdapter);
                }
            }
        }
        return oaiAdapter;
    }

}
