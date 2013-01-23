/*
 * 
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

package org.mycore.services.wsclient;

import java.io.File;
import java.util.Properties;
import java.net.URL;
import java.rmi.Remote;

import javax.xml.namespace.QName;

import org.hibernate.Transaction;
import org.mycore.backend.hibernate.MCRHIBConnection;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRException;
import org.mycore.common.xml.MCRURIResolver;
import org.mycore.parsers.bool.MCRCondition;
import org.mycore.services.fieldquery.MCRQuery;
import org.mycore.services.fieldquery.MCRQueryParser;

import org.apache.log4j.Logger;

import org.apache.axis.AxisFault;
import org.apache.axis.client.Call;
import org.apache.axis.client.Service;
import org.apache.axis.client.Stub;

import org.apache.ws.security.handler.WSHandlerConstants;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.message.token.UsernameToken;

import org.apache.axis.EngineConfiguration;
import org.apache.axis.configuration.FileProvider;
import org.apache.axis.constants.Style;
import org.apache.axis.constants.Use;
import org.apache.axis.description.OperationDesc;
import org.apache.axis.description.ParameterDesc;

/**
 * Test client for MyCoRe MCRWebService
 * 
 * @author Harald Richter
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 */
public class MCRWebServiceClient {
    static Logger logger = Logger.getLogger(MCRWebServiceClient.class);

    private static void usage() {
        System.out.println("usage: parameter of MCRWebServicClient\n");
        System.out.println("-endpoint     url of webservice");
        System.out.println("-operation    valid opartions are:");
        System.out.println("                 retrieve (mycore object), parameter -mcrid required");
        System.out.println("                 retrievecl (mycore classification), parameters -classID, -level and -catID rquired");
        System.out.println("                 retrievelink (mycore link), parameter -form or -to requiered, -type optional");
        System.out.println("                 query, parameter -file or -query required");
        System.out.println("-mcrid        id of MyCoRe-Object");
        System.out.println("-file         xml file with query");
        System.out.println("-query        query in text format");
    }

    private static void handleParams(String args[], Properties params) {
        for (int i = 0; i < args.length; i = i + 2) {
            String op = args[i];
            if (op.charAt(0)!='-')
                throw new MCRException("Unsupported operation: "+op);
            String value = args[i + 1];
            params.setProperty(op.substring(1), value);
        }
    }

    public static void main(String args[]) {
        Transaction tx = MCRHIBConnection.instance().getSession().beginTransaction();

        Properties params = new Properties();
        String baseURL=MCRConfiguration.instance().getString("MCR.baseurl","http://localhost:8291/");
        params.setProperty("endpoint", baseURL+"services/MCRWebService");
        /*
         * params.setProperty("mcrid", "DocPortal_document_00410901");
         * params.setProperty("operation", "retrieve");
         */
        handleParams(args, params);

        String endpoint = params.getProperty("endpoint");
        System.out.println("Endpoint: " + endpoint);
        String operation = params.getProperty("operation");
        System.out.println("Operation: " + operation);

        EngineConfiguration config = new FileProvider("client_deploy.wsdd");
        MCRWSServiceLocator l = new MCRWSServiceLocator(config);

        try {
            l.setMCRWebServiceEndpointAddress(endpoint);

            Remote remote = l.getPort(MCRWS.class);
            Stub axisPort = (Stub) remote;
            axisPort._setProperty(WSHandlerConstants.ACTION, WSHandlerConstants.USERNAME_TOKEN);
            axisPort._setProperty(UsernameToken.PASSWORD_TYPE, WSConstants.PASSWORD_DIGEST);
            axisPort._setProperty(WSHandlerConstants.USER, "wss4j");
            axisPort._setProperty(WSHandlerConstants.PW_CALLBACK_CLASS, "org.mycore.services.wsclient.MCRPWCallback");

            // Need to cast
            MCRWS stub = (MCRWebServiceSoapBindingStub) axisPort;

            if ("retrieve".equals(operation)) {
                String mcrid = params.getProperty("mcrid");
                if (null != mcrid) {
                    org.w3c.dom.Document result = stub.MCRDoRetrieveObject(mcrid);

                    org.jdom2.input.DOMBuilder d = new org.jdom2.input.DOMBuilder();
                    org.jdom2.Document doc = d.build(result);
                    org.jdom2.output.XMLOutputter outputter = new org.jdom2.output.XMLOutputter();
                    logger.info(outputter.outputString(doc));
                } else
                    System.out.println("parameter -mcrid missing");
            } else if ("query".equals(operation)) {
                String fileName = params.getProperty("file");
                String query = params.getProperty("query");
                if (null != fileName) {
                    File file=new File(fileName);
                    System.out.println(file.toURI());
                    org.jdom2.Element queryXML = MCRURIResolver.instance().resolve(file.toURI().toString());
                    org.jdom2.Document root = new org.jdom2.Document(queryXML);
                    org.jdom2.output.DOMOutputter doo = new org.jdom2.output.DOMOutputter();

                    org.w3c.dom.Document result = stub.MCRDoQuery(doo.output(root));

                    org.jdom2.input.DOMBuilder d = new org.jdom2.input.DOMBuilder();
                    org.jdom2.Document doc = d.build(result);
                    org.jdom2.output.XMLOutputter outputter = new org.jdom2.output.XMLOutputter();
                    logger.info(outputter.outputString(doc));
                } else if (query!=null){
                    System.out.println("query:"+query);
                    MCRCondition queryCond=new MCRQueryParser().parse(query);
                    org.jdom2.Document root = new MCRQuery(queryCond).buildXML();
                    org.jdom2.output.DOMOutputter doo = new org.jdom2.output.DOMOutputter();

                    org.w3c.dom.Document result = stub.MCRDoQuery(doo.output(root));

                    org.jdom2.input.DOMBuilder d = new org.jdom2.input.DOMBuilder();
                    org.jdom2.Document doc = d.build(result);
                    org.jdom2.output.XMLOutputter outputter = new org.jdom2.output.XMLOutputter();
                    logger.info(outputter.outputString(doc));
                } else
                    System.out.println("xml file with query missing");
            } else if ("retrievecl".equals(operation)) {
                String classID = params.getProperty("classID");
                String level = params.getProperty("level");
                String catID = params.getProperty("catID", "");
                if (null != classID && null != level) {
                    org.w3c.dom.Document result = stub.MCRDoRetrieveClassification(level, "children", classID, catID, null);

                    org.jdom2.input.DOMBuilder d = new org.jdom2.input.DOMBuilder();
                    org.jdom2.Document doc = d.build(result);
                    org.jdom2.output.XMLOutputter outputter = new org.jdom2.output.XMLOutputter();
                    logger.info(outputter.outputString(doc));
                } else
                    System.out.println("parameter(s) for retrieve classification missing");
            } else if ("retrievecl2".equals(operation)) {
                String classID = params.getProperty("classID");
                String level = params.getProperty("level");
                String categID = params.getProperty("catID", "");
                String type = "children";
                String format = "editor[TextCounter]";
                if (null != classID && null != level) {
                    // org.w3c.dom.Document result =
                    // stub.MCRDoRetrieveClassification(level, "children",
                    // classID, catID);

                    /** The AXIS service object */
                    Service service = new Service();

                    /** The description of the doQuery service operation */
                    OperationDesc operation2;

                    String xmlsoap = "http://xml.apache.org/xml-soap";

                    // Build doQuery operation description
                    operation2 = new OperationDesc();
                    operation2.setName("MCRDoQuery");
                    operation2.addParameter(new ParameterDesc(new QName("", "in0"), ParameterDesc.IN, new QName("http://xml.apache.org/xml-soap", "Document"), org.w3c.dom.Document.class, false, false));
                    operation2.setReturnType(new QName(xmlsoap, "Document"));
                    operation2.setReturnClass(org.w3c.dom.Document.class);
                    operation2.setReturnQName(new QName("", "MCRDoQueryReturn"));
                    operation2.setStyle(Style.RPC);
                    operation2.setUse(Use.ENCODED);

                    // Build webservice call
                    Call call = (Call) (service.createCall());
                    call.setTargetEndpointAddress(new URL(endpoint));
                    call.setOperation(operation2);
                    call.setOperationName("MCRDoRetrieveClassification");
                    call.removeAllParameters();
                    call.addParameter("level", org.apache.axis.Constants.XSD_STRING, javax.xml.rpc.ParameterMode.IN);
                    call.addParameter("type", org.apache.axis.Constants.XSD_STRING, javax.xml.rpc.ParameterMode.IN);
                    call.addParameter("classID", org.apache.axis.Constants.XSD_STRING, javax.xml.rpc.ParameterMode.IN);
                    call.addParameter("categID", org.apache.axis.Constants.XSD_STRING, javax.xml.rpc.ParameterMode.IN);
                    call.addParameter("format", org.apache.axis.Constants.XSD_STRING, javax.xml.rpc.ParameterMode.IN);
                    call.setReturnType(new QName("http://xml.apache.org/xml-soap", "Document"));
                    // Call webservice
                    org.w3c.dom.Document outDoc = (org.w3c.dom.Document) (call.invoke(new Object[] { level, type, classID, categID, format }));

                    org.jdom2.input.DOMBuilder d = new org.jdom2.input.DOMBuilder();
                    org.jdom2.Document doc = d.build(outDoc);
                    org.jdom2.output.XMLOutputter outputter = new org.jdom2.output.XMLOutputter();
                    logger.info(outputter.outputString(doc));
                } else
                    System.out.println("parameter(s) for retrieve classification missing");
            } else if ("retrievelink".equals(operation)) {
                String from = params.getProperty("from", "");
                String to = params.getProperty("to", "");
                String type = params.getProperty("type", "");
                if ((from.length() != 0) || (to.length() != 0)) {
                    org.w3c.dom.Document result = stub.MCRDoRetrieveLinks(from, to, type);
                    org.jdom2.input.DOMBuilder d = new org.jdom2.input.DOMBuilder();
                    org.jdom2.Document doc = d.build(result);
                    org.jdom2.output.XMLOutputter outputter = new org.jdom2.output.XMLOutputter();
                    logger.info(outputter.outputString(doc));
                } else
                    System.out.println("parameter -from or -to missing");

            } else {
                usage();
            }
            tx.commit();
        } catch (AxisFault ax) {
            tx.rollback();
            System.out.println(ax.toString());
            ax.dump();
        } catch (Exception ex) {
            ex.printStackTrace();
            tx.rollback();
        }
    }
}