/*
 * $RCSfile$
 * $Revision$ $Date$
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */

package org.mycore.services.fieldquery;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.xml.namespace.QName;

import org.apache.axis.client.Call;
import org.apache.axis.client.Service;
import org.apache.axis.constants.Style;
import org.apache.axis.constants.Use;
import org.apache.axis.description.OperationDesc;
import org.apache.axis.description.ParameterDesc;
import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.DOMBuilder;
import org.jdom.output.DOMOutputter;
import org.mycore.common.MCRConfigurationException;
import org.mycore.common.xml.MCRURIResolver;

/**
 * Executes a query on a remote host using a webservice
 * 
 * @author Frank Lützenkirchen
 */
public class MCRQueryClient {
    /** The logger */
    private final static Logger LOGGER = Logger.getLogger(MCRQueryClient.class);

    /** A list containing the aliases of all hosts */
    public final static List ALL_HOSTS;

    /** A map from host alias to endpoint URL */
    private static Properties endpoints = new Properties();

    /** The AXIS service object */
    private static Service service = new Service();

    /** The description of the doQuery service operation */
    private static OperationDesc operation;

    static {
        // Read hosts.xml configuration file
        Element hosts = MCRURIResolver.instance().resolve("resource:hosts.xml");

        ALL_HOSTS = new ArrayList();
        List children = hosts.getChildren();
        for (int i = 0; i < children.size(); i++) {
            Element host = (Element) (children.get(i));
            String alias = host.getAttributeValue("alias");
            String url = host.getAttributeValue("url");
            LOGGER.debug("Host " + alias + " uses query endpoint at " + url);
            endpoints.put(alias, url);
            ALL_HOSTS.add(alias);
        }

        String xmlsoap = "http://xml.apache.org/xml-soap";

        // Build doQuery operation description
        operation = new OperationDesc();
        operation.setName("MCRDoQuery");
        operation.addParameter(new ParameterDesc(new QName("", "in0"), ParameterDesc.IN, new QName("http://xml.apache.org/xml-soap", "Document"), org.w3c.dom.Document.class, false, false));
        operation.setReturnType(new QName(xmlsoap, "Document"));
        operation.setReturnClass(org.w3c.dom.Document.class);
        operation.setReturnQName(new QName("", "MCRDoQueryReturn"));
        operation.setStyle(Style.RPC);
        operation.setUse(Use.ENCODED);
    }

    public static MCRResults search(String hostAlias, MCRQuery query) {
        if (!endpoints.containsKey(hostAlias)) {
            String msg = "No configuration for host " + hostAlias;
            throw new MCRConfigurationException(msg);
        }

        LOGGER.info("Starting remote query at host " + hostAlias);

        String endpoint = endpoints.getProperty(hostAlias);
        Document xml = query.buildXML();

        try {
            // Build webservice call
            Call call = (Call) (service.createCall());
            call.setTargetEndpointAddress(new URL(endpoint));
            call.setOperation(operation);
            call.setOperationName("MCRDoQuery");

            // Call webservice
            org.w3c.dom.Document inDoc = new DOMOutputter().output(xml);
            org.w3c.dom.Document outDoc = (org.w3c.dom.Document) (call.invoke(new Object[] { inDoc }));
            LOGGER.info("Received remote query results, processing XML now");

            // Process xml response
            Document response = new DOMBuilder().build(outDoc);
            MCRResults results = MCRResults.parseXML(response);
            LOGGER.debug("Received " + results.getNumHits() + " hits from host " + hostAlias);
            return results;
        } catch (Exception ex) {
            String msg = "Exception while querying remote host " + hostAlias;
            LOGGER.error(msg, ex);
            return new MCRResults();
        }
    }
}
