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

import javax.xml.namespace.QName;

import org.apache.axis.client.Call;
import org.apache.axis.client.Service;
import org.apache.axis.constants.Style;
import org.apache.axis.constants.Use;
import org.apache.axis.description.OperationDesc;
import org.apache.axis.description.ParameterDesc;

import org.jdom.Document;
import org.jdom.input.DOMBuilder;

/*
 * This class is the implementation for remote access via Webservices
 * 
 * @author Jens Kupferschmidt @author Frank Lützenkirchen
 */

public class MCRQueryClientWebService extends MCRQueryClientBase {

    /** The AXIS service object */
    private static Service service = new Service();

    /** The description of the doQuery service operation */
    private static OperationDesc operation;

    /** The complete WebService URL * */
    private String endpoint = "";

    static {
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

    /*
     * The constructor.
     */
    public MCRQueryClientWebService() {
        super();
    }

    /*
     * The initialization.
     * 
     * @param xmlhost an entry of a remote host from hosts.xml
     */
    public void init(org.jdom.Element xmlhost) {
        alias = xmlhost.getAttributeValue("alias");
        if ((alias == null) || ((alias = alias.trim()).length() == 0)) {
            alias = "remote";
            LOGGER.warn("The alias attribute for the host is null or empty, remote was set.");
        }
        url = xmlhost.getAttributeValue("url");
        if ((url == null) || ((url = url.trim()).length() == 0)) {
            url = "http://localhost:8291/";
            LOGGER.warn("The url attribute for the host is null or empty, http://localhost:8291/ was set.");
        }
        access = xmlhost.getAttributeValue("access");
        if ((access == null) || ((access = access.trim()).length() == 0)) {
            alias = "webservice";
            LOGGER.warn("The access attribute for the host is null or empty, webservice was set.");
        }
        servicepath = xmlhost.getAttributeValue("servicepath");
        if ((servicepath == null) || ((servicepath = servicepath.trim()).length() == 0)) {
            alias = "services/MCRWebService";
            LOGGER.warn("The servicepath attribute for the host is null or empty, services/MCRWebService was set.");
        }
        StringBuffer sb = new StringBuffer(256);
        sb.append("Host ").append(alias).append(" with access mode ").append(access).append(" uses host url ").append(url).append(servicepath);
        LOGGER.debug(sb.toString());
        if (!url.endsWith("/"))
            url = url + "/";
        endpoint = url + servicepath;
    }

    /**
     * Executes a query on a single remote host using the defined service.
     * 
     * @param inDoc
     *            the query as W3C DOM document
     * @param results
     *            the result list to add the hits to
     */
    public void search(org.w3c.dom.Document inDoc, MCRResults results) {
        try {
            // Build webservice call
            Call call = (Call) (service.createCall());
            call.setTargetEndpointAddress(new URL(endpoint));
            call.setOperation(operation);
            call.setOperationName("MCRDoQuery");

            // Call webservice
            org.w3c.dom.Document outDoc = (org.w3c.dom.Document) (call.invoke(new Object[] { inDoc }));
            LOGGER.info("Received remote query results, processing XML now");

            // Process xml response
            Document response = new DOMBuilder().build(outDoc);
            int numHits = results.merge(response, alias);
            LOGGER.debug("Received " + numHits + " hits from host " + alias);
        } catch (Exception ex) {
            String msg = "Exception while querying remote host " + alias;
            LOGGER.error(msg, ex);
        }
    }

    /**
     * Retrieves an Object from remote host using the defined service.
     * 
     * @param hostAlias
     *            the alias of the remote host as defined in hosts.xml
     * @param ID
     *            the ID of the object to retrieve
     * @return the object document
     */
    public org.w3c.dom.Document doRetrieveObject(String ID) {
        try {
            // Build webservice call
            Call call = (Call) (service.createCall());
            call.setTargetEndpointAddress(new URL(endpoint));
            call.setOperation(operation);
            call.setOperationName("MCRDoRetrieveObject");
            // Call webservice
            org.w3c.dom.Document outDoc = (org.w3c.dom.Document) (call.invoke(new Object[] { ID }));
            LOGGER.info("Received remote Object: " + ID);

            return outDoc;
        } catch (Exception ex) {
            String msg = "Exception while retrieving Object '" + ID + "' from remote host " + alias;
            LOGGER.error(msg, ex);
        }
        return null;
    }

    /**
     * Retrieves an classification part from remote host using the WebService.
     * 
     * @param level
     *            the level of the classification to retrieve
     * @param type
     *            the type of the classification to retrieve
     * @param classID
     *            the class ID of the classification to retrieve
     * @param categID
     *            the category ID of the classification to retrieve
     * @return the classification document
     */
    public org.w3c.dom.Document doRetrieveClassification(String level, String type, String classID, String categID) {
        StringBuffer ID = new StringBuffer(256);
        ID.append("level=").append(level).append(":type=").append(type).append(":classId=").append(classID).append(":categId=").append(categID);
        try {
            // Build webservice call
            Call call = (Call) (service.createCall());
            call.setTargetEndpointAddress(new URL(endpoint));
            call.setOperation(operation);
            call.setOperationName("MCRDoRetrieveClassification");
            call.removeAllParameters();
            call.addParameter("level", org.apache.axis.Constants.XSD_STRING, javax.xml.rpc.ParameterMode.IN);
            call.addParameter("type", org.apache.axis.Constants.XSD_STRING, javax.xml.rpc.ParameterMode.IN);
            call.addParameter("classID", org.apache.axis.Constants.XSD_STRING, javax.xml.rpc.ParameterMode.IN);
            call.addParameter("categID", org.apache.axis.Constants.XSD_STRING, javax.xml.rpc.ParameterMode.IN);
            call.setReturnType(new QName("http://xml.apache.org/xml-soap", "Document"));
            // Call webservice
            org.w3c.dom.Document outDoc = (org.w3c.dom.Document) (call.invoke(new Object[] { level, type, classID, categID }));
            LOGGER.info("Received remote Object: " + ID.toString());

            return outDoc;
        } catch (Exception ex) {
            String msg = "Exception while retrieving Object '" + ID.toString() + "' from remote host " + alias;
            LOGGER.error(msg, ex);
        }
        return null;
    }
}
