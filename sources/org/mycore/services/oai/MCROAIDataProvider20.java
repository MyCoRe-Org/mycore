/**
 * $RCSfile: MCROAIDataProvider20.java,v $
 * $Revision: 1.21 $ $Date: 2003/01/28 15:53:23 $
 *
 * This file is part of ** M y C o R e **
 * Visit our homepage at http://www.mycore.de/ for details.
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
 * along with this program, normally in the file license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 *
 **/

package org.mycore.services.oai;

import java.io.*;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.sax.*;
import javax.xml.transform.stream.StreamSource;

import org.mycore.common.*;
import org.mycore.datamodel.classifications.MCRClassificationItem;
import org.mycore.datamodel.classifications.MCRCategoryItem;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRMetaClassification;
import org.mycore.datamodel.metadata.MCRMetaElement;
import org.mycore.services.query.MCRQueryResult;
import org.mycore.common.xml.MCRXMLContainer;
import org.mycore.common.xml.MCRXMLHelper;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import org.jdom.*;
import org.jdom.input.*;
import org.jdom.output.*;
import org.jdom.transform.*;

import org.xml.sax.*;

/**
 * This class implements an OAI Data Provider for MyCoRe
 *
 * @author Werner Gresshoff
 *
 * @version $Revision: 1.21 $ $Date: 2003/01/28 15:53:23 $
 **/
public class MCROAIDataProvider20 extends HttpServlet {
    static Logger logger = Logger.getLogger(MCROAIDataProvider20.class);
    
    /**
     * Some configuration constants
     */
    private static String STR_OAI_ADMIN_EMAIL = "MCR.oai.adminemail"; //EMail address of oai admin
    private static String STR_OAI_REPOSITORY_NAME = "MCR.oai.repositoryname"; //Name of the repository
    private static String STR_OAI_REPOSITORY_IDENTIFIER = "MCR.oai.repositoryidentifier"; //Identifier of the repository
    private static String STR_OAI_CLASSIFICATION_ID = "MCR.oai.classificationid"; //Identifier of the repository
    private static String STR_OAI_RESUMPTIONTOKEN_DIR = "MCR.oai.resumptiontoken.dir"; //temporary Directory
    private static String STR_OAI_MAXRETURNS = "MCR.oai.maxreturns"; //maximum number of returned list sets
    private static String STR_OAI_METADATA_TRANSFORMER = "MCR.oai.metadata.transformer"; 
    private static String STR_STANDARD_TRANSFORMER = "oai_dc";
    // Name of the metadata transformer
    // Format in properties file is MCR.oai.metadata.transformer.oai_dc
    // If there are other metadata formats available, all need a transformer entry
    // of it's own, e.g. MCR.oai.metadata.transformer.oai_marc=mycore2marc.xsl
    private static String STR_OAI_METADATA_NAMESPACE = "MCR.oai.metadata.namespace"; 
    private static String STR_OAI_METADATA_SCHEMA = "MCR.oai.metadata.schema"; 
    // If there are other metadata formats available, all need a namespace and schema entry
    // of it's own, e.g. 
    // MCR.oai.metadata.namespace.olac=http://www.language-archives.org/OLAC/0.2/
    // MCR.oai.metadata.schema.olac=http://www.language-archives.org/OLAC/olac-0.2.xsd
    
    /**
     * Some constants referring to metadata formats (must be known for dc)
     */
    private static String STR_DC_NAMESPACE = "http://www.openarchives.org/OAI/2.0/oai_dc/";
    private static String STR_DC_SCHEMA = "http://www.openarchives.org/OAI/2.0/oai_dc.xsd";

    private static String STR_OAI_NAMESPACE = "http://www.openarchives.org/OAI/2.0/";
    private static String STR_OAI_VERSION = "2.0";
    private static String STR_SCHEMA_INSTANCE = "http://www.w3.org/2001/XMLSchema-instance";

    private static String STR_RESUMPTIONTOKEN_SUFFIX = ".ser";
    private static int I_MAXRETURNS;
    
    private static String ERR_FAULTY_VERB = "No verb or too much verbs";
    private static String ERR_ILLEGAL_VERB = "Illegal verb";
    private static String ERR_ILLEGAL_ARGUMENT = "Illegal argument";
    private static String ERR_UNKNOWN_FORMAT = "Unknown metadata format";
    private static String ERR_UNKNOWN_ID = "Unknown identifier";
    private static String ERR_BAD_RESUMPTION_TOKEN = "Bad resumption token";
    private static String ERR_NO_RECORDS_MATCH = "No results where found with your search criteria";

    private static String[] STR_VERBS = {"GetRecord", "Identify",
        "ListIdentifiers", "ListMetadataFormats", "ListRecords",
        "ListSets"};

    /** Initializes the servlet.
     */
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        
    }
    
    /** Destroys the servlet.
     */
    public void destroy() {
        
    }
    
    /** Processes requests for both HTTP <code>GET</code> and <code>POST</code> methods.
     * @param request servlet request
     * @param response servlet response
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
    	MCRConfiguration.instance().reload(true);
    	PropertyConfigurator.configure(MCRConfiguration.instance().getLoggingProperties());
        response.setContentType("text/xml; charset=UTF-8");
        
        // Exceptions must be caught...
        try {
            PrintWriter out = response.getWriter();
            XMLOutputter outputter = new XMLOutputter("   ");
            outputter.setNewlines(true);
	        outputter.setEncoding("UTF-8");

            Namespace ns = Namespace.getNamespace(STR_OAI_NAMESPACE);
            Element eRoot  = new Element("OAI-PMH", ns);
            org.jdom.Document header = new org.jdom.Document(eRoot);
            Namespace xsi = Namespace.getNamespace("xsi", STR_SCHEMA_INSTANCE);

            eRoot.addNamespaceDeclaration(xsi);
            eRoot.setAttribute("schemaLocation", STR_OAI_NAMESPACE + " "  + STR_OAI_NAMESPACE + "OAI-PMH.xsd", xsi);
            
            // add "responseDate"...
            Date today = new Date();
    	    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
            SimpleTimeZone tz = (SimpleTimeZone) timeFormat.getTimeZone();
            
            String sDate = dateFormat.format(today) + "T" + timeFormat.format(today) + "Z";
            Element eDate = new Element("responseDate", ns);
            eDate.setText(sDate);
            eRoot.addContent(eDate);
            
            // add "request"...
            String url = HttpUtils.getRequestURL(request).toString();
            Element eRequest = new Element("request", ns);
            eRequest.addContent(response.encodeURL(url));
            eRoot.addContent(eRequest);
            
            org.jdom.Document document = null;
            
            //get parameter "verb"
            String verb[] = getParameter("verb", request); 
            
            if ((verb == null) || (verb.length != 1)) {
            	logger.info("Request without a verb.");
            	document = setError(header, ns, "badVerb", ERR_FAULTY_VERB);
            } else {
                //Check if a correct verb was given
                if (verb[0].equalsIgnoreCase(STR_VERBS[0])) {
                    document = getRecord(request, header, ns);
                } else if (verb[0].equalsIgnoreCase(STR_VERBS[1])) {
                    document = identify(request, header, ns);
                } else if (verb[0].equalsIgnoreCase(STR_VERBS[2])) {
                    document = listIdentifiers(request, header, ns);
                } else if (verb[0].equalsIgnoreCase(STR_VERBS[3])) {
                    document = listMetadataFormats(request, header, ns);
                } else if (verb[0].equalsIgnoreCase(STR_VERBS[4])) {
                    document = listRecords(request, header, ns);
                } else if (verb[0].equalsIgnoreCase(STR_VERBS[5])) {
                    document = listSets(request, header, ns);
                } else {
	            	logger.info("Request without a verb.");
                	document = setError(header, ns, "badVerb", ERR_ILLEGAL_VERB);
                }
            }

            outputter.output(document, out); 

	    return;
        } catch (MCRException mcrx) {
            logger.warn(mcrx.getMessage());
        } catch (IOException ioex) {
            logger.warn(ioex.getMessage());
        }
    }
    
    /** Handles the HTTP <code>GET</code> method.
     * @param request servlet request
     * @param response servlet response
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        processRequest(request, response);
    }
    
    /** Handles the HTTP <code>POST</code> method.
     * @param request servlet request
     * @param response servlet response
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        processRequest(request, response);
    }
    
    /** Returns a short description of the servlet.
     */
    public String getServletInfo() {
        return "This class implements an OAI Data Provider for MyCoRe";
    }
    
    /**
     * Extracts the value of <i>p</i> out of <i>request</i> and returns it.
     * If the parameter occurs more than one time, the first occurence is returned.
     */
    private String[] getParameter(String p, HttpServletRequest request) {      
        Enumeration parameters = request.getParameterNames();
	    String parameter = null;
        String[] paramValues = null;

        while (parameters.hasMoreElements()) {
            parameter = (String) parameters.nextElement();
            if (parameter.equalsIgnoreCase(p)) {
                paramValues = request.getParameterValues(parameter);
                logger.debug("Parameter mit Wert " + p + " gefunden.");
                return paramValues;
            }
        }
	    return null;
    }
    
    /**
     * check for bad arguments
     */
    boolean badArgumentsFound(HttpServletRequest request, int maxargs) {
        Enumeration parameters = request.getParameterNames();
        int nop = 0;
        while (parameters.hasMoreElements()) {
            parameters.nextElement();
            nop++;
            if (nop > maxargs) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * insert <error> in document
     */
    private org.jdom.Document setError(org.jdom.Document document, Namespace ns, String errorCode, String errorDescription) {
        Element eRoot = document.getRootElement();
        Element eError = new Element("error", ns);
        eError.setAttribute("code", errorCode);
        eError.addContent(errorDescription);
        eRoot.addContent(eError);
                
        return document;
    }
    
    /**
     * Implementation of the OAI Verb Identify
     */
    private org.jdom.Document identify(HttpServletRequest request, org.jdom.Document header, Namespace ns) {
    	logger.info("Harvester hat 'Identify' angefordert");
        org.jdom.Document document = header;
        
        if (badArgumentsFound(request, 1)) {
            return setError(document, ns, "badArgument", ERR_ILLEGAL_ARGUMENT);
        }

        Element eRoot = document.getRootElement();
        Element eRequest = eRoot.getChild("request", ns);
        eRequest.setAttribute("verb", "Identify");
        Element eIdentify = new Element("Identify", ns);
        String repositoryName = MCRConfiguration.instance().getString(STR_OAI_REPOSITORY_NAME + "." + getServletName());
        eIdentify.addContent(MCRXMLHelper.newJDOMElementWithContent("repositoryName", ns, 
            repositoryName));
        eIdentify.addContent(MCRXMLHelper.newJDOMElementWithContent("baseURL", ns, 
            URLEncoder.encode(HttpUtils.getRequestURL(request).toString())));
        eIdentify.addContent(MCRXMLHelper.newJDOMElementWithContent("protocolVersion", ns, 
            STR_OAI_VERSION));
        String adminEmail = MCRConfiguration.instance().getString(STR_OAI_ADMIN_EMAIL);
        eIdentify.addContent(MCRXMLHelper.newJDOMElementWithContent("adminEmail", ns, 
            adminEmail));
        eIdentify.addContent(MCRXMLHelper.newJDOMElementWithContent("earliestDatestamp", ns, 
            "1900-01-01"));
        eIdentify.addContent(MCRXMLHelper.newJDOMElementWithContent("deletedRecord", ns, 
            "no"));
        eIdentify.addContent(MCRXMLHelper.newJDOMElementWithContent("granularity", ns, 
            "YYYY-MM-DD"));
        eIdentify.addContent(MCRXMLHelper.newJDOMElementWithContent("compression", ns, 
            "identity"));
        
//        Properties properties = MCRConfiguration.instance().getProperties("mcr.oai");
        
        Element eDescription = new Element("description", ns);
        Namespace idns = Namespace.getNamespace(STR_OAI_NAMESPACE + "oai-identifier");
        Element eOAIIdentifier = new Element("oai-identifier", idns);
        Namespace xsi = Namespace.getNamespace("xsi", STR_SCHEMA_INSTANCE);
        eOAIIdentifier.addNamespaceDeclaration(xsi);
        eOAIIdentifier.setAttribute("schemaLocation", STR_OAI_NAMESPACE + "oai-identifier "  + STR_OAI_NAMESPACE + "oai-identifier.xsd", xsi);

        String repositoryIdentifier = MCRConfiguration.instance()
            .getString(STR_OAI_REPOSITORY_IDENTIFIER + "." + getServletName());
        eOAIIdentifier.addContent(MCRXMLHelper.newJDOMElementWithContent("scheme", idns, "oai"));
        eOAIIdentifier.addContent(MCRXMLHelper.newJDOMElementWithContent("repositoryIdentifier", idns, repositoryIdentifier));  
        eOAIIdentifier.addContent(MCRXMLHelper.newJDOMElementWithContent("delimiter", idns, ":"));
        eOAIIdentifier.addContent(MCRXMLHelper.newJDOMElementWithContent("sampleIdentifier", idns, "oai:" + repositoryIdentifier + ":MyCoReDemoDC_Document_1"));

        eDescription.addContent(eOAIIdentifier);
        eIdentify.addContent(eDescription);
        eRoot.addContent(eIdentify);
        
        return document;
    }

    /**
     * Implementation of the OAI Verb GetRecord
     */
    private org.jdom.Document getRecord(HttpServletRequest request, org.jdom.Document header, Namespace ns) {
        org.jdom.Document document = header;

        // First: check if there was an identifier in the request (required)
        String identifier[] = getParameter("identifier", request); 
        // Then: check if there was an metadataPrefix in the request (required)
        String metadataPrefix[] = getParameter("metadataPrefix", request); 
        if ((identifier == null) || (metadataPrefix == null) || 
                (identifier.length != 1) || (metadataPrefix.length != 1) || 
                badArgumentsFound(request, 3)) {
            logger.info("Anfrage 'getRecord' wurde wegen fehlendem Parameter abgebrochen.");
            return setError(document, ns, "badArgument", ERR_ILLEGAL_ARGUMENT);
        }
        
        Element eRoot = document.getRootElement();
        Element eRequest = eRoot.getChild("request", ns);
        eRequest.setAttribute("verb", "GetRecord");
        eRequest.setAttribute("identifier", identifier[0]);
        eRequest.setAttribute("metadataPrefix", metadataPrefix[0]);
        
        //Search for the last delimiter (a colon)...
		int lastDelimiter = identifier[0].lastIndexOf(":");
		//...and use the characters behind as id
        String id = identifier[0].substring(lastDelimiter + 1);
        //We don't look at the begginning of the identifier!!
        
        //Check, if the requested metadata format is supported
        String format = MCRConfiguration.instance().getString(STR_OAI_METADATA_TRANSFORMER + "." + metadataPrefix[0], "empty");
        if (format.equals("empty")) {
        	logger.info("Anfrage 'getRecord' wurde wegen fehlendem Metadatenformat " + metadataPrefix[0] + " abgebrochen.");
            return setError(document, ns, "cannotDisseminateFormat", ERR_UNKNOWN_FORMAT);
        }
        
        //Then look, if the id exists
        if (MCRObject.existInDatastore(id)) {
            Element eGetRecord = new Element("GetRecord", ns);
            eGetRecord.addContent(getRecordElement(id, ns));
            eRoot.addContent(eGetRecord);
            
            org.jdom.Document newDocument = transform(document, format, metadataPrefix[0]);
            if (newDocument != null) {
                document = newDocument;
            }
		} else {
			logger.info("Anfrage 'getRecord wurde fegen fehlender ID " + id + "abgebrochen.");
            return setError(document, ns, "idDoesNotExist", ERR_UNKNOWN_ID);
        }
        
        return document;
    }
    
    /** 
     * Helper function to build header element from object id
     */
    private Element getHeaderElement(String id, Namespace ns) {
        MCRObject object = new MCRObject();
        try {
            object.receiveFromDatastore(id);
        } catch(MCRException e) {
            logger.warn(e.getMessage());
            return null;
        }
        
        Element eHeader = formatHeader(object, id, ns);
        return eHeader;
    }

    /** 
     * Helper function to build record element from object id
     */
    private Element getRecordElement(String id, Namespace ns) {
        Element eRecord = new Element("record", ns);
        MCRObject object = new MCRObject();
        try {
            object.receiveFromDatastore(id);
        } catch(MCRException e) {
            logger.warn(e.getMessage());
            return eRecord;
        }
        
        Element eHeader = formatHeader(object, id, ns);
        eRecord.addContent(eHeader);
            
        try {
            Element eMetadata = object.getMetadata().createXML();
            eRecord.addContent(eMetadata);
        } catch(MCRException mcrx) {
            logger.warn(mcrx.getMessage());
        } finally {
            return eRecord;
        }
    }

    /**
     * Helper function to build a header element
     */
    private Element formatHeader(MCRObject object, String id, Namespace ns) {
        Calendar calendar = object.getService().getDate("modifydate");
        // Format the date.
        SimpleDateFormat formatter = new SimpleDateFormat ("yyyy-MM-dd");
        formatter.setCalendar(calendar);
        String datestamp = formatter.format(calendar.getTime());
            
        Element eHeader = new Element("header", ns);
        String repositoryIdentifier = MCRConfiguration.instance()
            .getString(STR_OAI_REPOSITORY_IDENTIFIER + "." + getServletName());
        eHeader.addContent(MCRXMLHelper.newJDOMElementWithContent("identifier", ns, "oai:" + repositoryIdentifier + ":" + id));        
        eHeader.addContent(MCRXMLHelper.newJDOMElementWithContent("datestamp", ns, datestamp));        
        
        String classificationIdentifier = MCRConfiguration.instance().
            getString(STR_OAI_CLASSIFICATION_ID + "." + getServletName());
        
        for (int i = 0; i < object.getMetadata().size(); i++) {
            if (object.getMetadata().getMetadataElement(i).getClassName().equals("MCRMetaClassification")) {
                MCRMetaElement element = object.getMetadata().getMetadataElement(i);
                if (element.getTag().equals("subjects")) {
                    for (int j = 0; i < element.size(); j++) {
                        MCRMetaClassification classification = (MCRMetaClassification) element.getElement(j);
                        String categoryIdentifier = classification.getCategId();
                        MCRCategoryItem category = MCRCategoryItem.
                            getCategoryItem(classificationIdentifier, categoryIdentifier);
                        MCRCategoryItem parent;
                        while ((parent = category.getParent()) != null) {
                            categoryIdentifier = parent.getID() + ":" + categoryIdentifier;
                            category = parent;
                        }
                        eHeader.addContent(MCRXMLHelper.newJDOMElementWithContent("setSpec", ns, categoryIdentifier));
                    }
                }
            }
        }
        
        return eHeader;
    }
    
    /**
     * Helper function for record transformation
     */
    private org.jdom.Document transform(org.jdom.Document in, String stylesheet, String requestedFormat) {
        try {
            JDOMResult out = new JDOMResult();
            if (requestedFormat.equals(STR_STANDARD_TRANSFORMER)) {
                Transformer transformer = TransformerFactory.newInstance().newTransformer(
                    new StreamSource(new File(getServletContext().getRealPath("/WEB-INF/stylesheets/" + stylesheet))));
                transformer.transform(new JDOMSource(in), out);
            } else {
                String standard = MCRConfiguration.instance().
                    getString(STR_OAI_METADATA_TRANSFORMER + "." + STR_STANDARD_TRANSFORMER, "empty");
                if (standard.equals("empty")) {
                    logger.fatal("Transformation error: standard stylesheet entry is missing in properties file.");
                    return null;
                }

                SAXParserFactory spf = SAXParserFactory.newInstance();
                SAXParser parser = spf.newSAXParser();
                XMLReader reader = parser.getXMLReader();
            
                SAXTransformerFactory stf = (SAXTransformerFactory) TransformerFactory.newInstance();
                XMLFilter filter1 = stf.
	                newXMLFilter(new StreamSource(new File(getServletContext().getRealPath("/WEB-INF/stylesheets/" + standard))));
                XMLFilter filter2 = stf.
    	            newXMLFilter(new StreamSource(new File(getServletContext().getRealPath("/WEB-INF/stylesheets/" + stylesheet))));
                filter1.setParent(reader);
                filter2.setParent(filter1);
            
                Transformer transformer = stf.newTransformer();
                JDOMSource transformSource = new JDOMSource(in);
                transformSource.setXMLReader(filter2);
                transformer.transform(transformSource, out);
            }
            return out.getDocument();
        }
        catch (ParserConfigurationException e) {
            logger.error(e.getMessage());
            return null;
        }
        catch (SAXException e) {
            logger.error(e.getMessage());
            return null;
        }
        catch (TransformerException e) {
            logger.fatal(e.getMessage());
            return null;
        }
    }

    /** 
     * Implementation of the OAI Verb ListMetadataFormats
     */
    private org.jdom.Document listMetadataFormats(HttpServletRequest request, org.jdom.Document header, Namespace ns) {
        org.jdom.Document document = header;
        Element eRoot = document.getRootElement();
        
        String repositoryIdentifier = MCRConfiguration.instance().
            getString(STR_OAI_REPOSITORY_IDENTIFIER + "." + getServletName());
        logger.debug("Repository aus properties: " + repositoryIdentifier);
        
        // First; check if there was an identifier in the request
        String identifier[] = getParameter("identifier", request); 
        if (identifier == null) {
            if (badArgumentsFound(request, 1)) {
            	logger.info("Anfrage 'listMetadataFormats' wurde wegen fehlendem Parameter abgebrochen.");
                return setError(document, ns, "badArgument", ERR_ILLEGAL_ARGUMENT);
            }
            Element eRequest = eRoot.getChild("request", ns);
            eRequest.setAttribute("verb", "ListMetadataFormats");
        } else if (identifier.length > 1) {
            //Es ist nur ein Identifier erlaubt!
          	logger.info("Anfrage 'listMetadataFormats' wurde wegen zu vieler Parameter abgebrochen.");
            return setError(document, ns, "badArgument", ERR_ILLEGAL_ARGUMENT);
        } else {
            Element eRequest = eRoot.getChild("request", ns);
            eRequest.setAttribute("verb", "ListMetadataFormats");
            eRequest.setAttribute("identifier", identifier[0]);
            if (!checkIdentifier(identifier[0], repositoryIdentifier)) {
            	logger.info("Anfrage 'listMetadataFormats' wurde wegen falscher ID abgebrochen.");
                return setError(document, ns, "idDoesNotExist", ERR_UNKNOWN_ID);
            }
        }
        
        // The supported metadata formats are only returned if no identifier was given, or
        // the identifier was found in the repository
        Element eListMetadataFormats = new Element("ListMetadataFormats", ns);
        Element dcMetadataFormat = new Element("metadataFormat", ns);
        dcMetadataFormat.addContent(MCRXMLHelper.
            newJDOMElementWithContent("metadataPrefix", ns, "oai_dc"));
        dcMetadataFormat.addContent(MCRXMLHelper.
            newJDOMElementWithContent("schema", ns, STR_DC_SCHEMA));
        dcMetadataFormat.addContent(MCRXMLHelper.
            newJDOMElementWithContent("metadataNamespace", ns, STR_DC_NAMESPACE));
        eListMetadataFormats.addContent(dcMetadataFormat);
        
        Properties properties = MCRConfiguration.instance().
            getProperties(STR_OAI_METADATA_NAMESPACE);
        Enumeration propertiesNames = properties.propertyNames();
        while (propertiesNames.hasMoreElements()) {
            String name = (String) propertiesNames.nextElement();
            String metadataPrefix = name.substring(name.lastIndexOf(".") + 1);
            Element eMetadataFormat = new Element("metadataFormat", ns);
            eMetadataFormat.addContent(MCRXMLHelper.
                newJDOMElementWithContent("metadataPrefix", ns, metadataPrefix));
            eMetadataFormat.addContent(MCRXMLHelper.
                newJDOMElementWithContent("schema", ns, MCRConfiguration.instance().
                getString(STR_OAI_METADATA_SCHEMA + "." + metadataPrefix)));
            eMetadataFormat.addContent(MCRXMLHelper.
                newJDOMElementWithContent("metadataNamespace", ns, MCRConfiguration.instance().
                getString(STR_OAI_METADATA_NAMESPACE + "." + metadataPrefix)));
            eListMetadataFormats.addContent(eMetadataFormat);
        }
        
        eRoot.addContent(eListMetadataFormats);

        return document;
    }
    
    /**
     * Check for conformance with the rules and the existence of the given identifier.
     */
    private boolean checkIdentifier(String identifier, String repositoryIdentifier) {
	// First, check if the identifier starts with "oai"
	if (!identifier.toUpperCase().startsWith("OAI:")) {
	    return false;
	} else {
	    int lastColon = identifier.lastIndexOf(":");
	    
	    if ((lastColon == 3) || !identifier.substring(4, lastColon).equals(repositoryIdentifier) ||
                    !MCRObject.existInDatastore(identifier.substring(lastColon + 1))) {
		return false;
	    }
	} 
        
	return true;
    }

    /** 
     ** create a JDOM from an array of categories
     */
    private org.jdom.Document getSets(org.jdom.Document header, Namespace ns, 
            MCRCategoryItem[] children, String parentSpec) {
        org.jdom.Document document = header;
        Element eRoot = document.getRootElement();
		Element eListSets = eRoot.getChild("ListSets", ns);
    
        for (int i = 0; i < children.length; i++) { 
            String categoryID = children[i].getID();
            String categoryLabel = children[i].getText("en");
            String categoryDescription = children[i].getDescription("en");
	    	logger.debug("Suche nach Kategorie: " + categoryID);    
            
            Element eSet = new Element("set", ns);
            eSet.addContent(MCRXMLHelper.
                newJDOMElementWithContent("setSpec", ns, parentSpec + categoryID));
            eSet.addContent(MCRXMLHelper.
                newJDOMElementWithContent("setName", ns, categoryLabel));
            if ((categoryDescription != null) && (categoryDescription.length() > 0)) {
                Namespace oaidc = Namespace.getNamespace("oai_dc", STR_DC_NAMESPACE);
                Element eDC = new Element("dc", oaidc);
                Namespace dc = Namespace.getNamespace("dc", "http://purl.org/dc/elements/1.1/");
                Namespace xsi = Namespace.getNamespace("xsi", STR_SCHEMA_INSTANCE);
                eDC.addNamespaceDeclaration(dc);
                eDC.addNamespaceDeclaration(xsi);
                eDC.setAttribute("schemaLocation", STR_OAI_NAMESPACE + "oai_dc/ "  + STR_OAI_NAMESPACE + "oai_dc.xsd", xsi);
                Element eDescription = new Element("description", dc);
                eDescription.addContent(categoryDescription);
                eDC.addContent(eDescription);
                eSet.addContent(eDC);
            }
            eListSets.addContent(eSet);
		    
            if (children[i].hasChildren()) {
                document = getSets(document, ns, children[i].getChildren(), 
                    parentSpec + categoryID + ":");
            }
	}
	
        return document;
    }
    
    /** 
     ** Implementation of the OAI Verb ListSets
     */
    private org.jdom.Document listSets(HttpServletRequest request, org.jdom.Document header, Namespace ns) {
        org.jdom.Document document = header;
        Element eRoot = document.getRootElement();
        
        if (badArgumentsFound(request, 1)) {
        	logger.info("Anfrage 'listSets' enthält zuviele Parameter.");
            return setError(document, ns, "badArgument", ERR_ILLEGAL_ARGUMENT);
        }
        Element eRequest = eRoot.getChild("request", ns);
        eRequest.setAttribute("verb", "ListSets");
		Element eListSets = new Element("ListSets", ns);
        eRoot.addContent(eListSets);
        
        String classificationIdentifier = MCRConfiguration.instance().
            getString(STR_OAI_CLASSIFICATION_ID + "." + getServletName());
        logger.debug("Suche in Klassifikation: " + classificationIdentifier);
        MCRClassificationItem repository = MCRClassificationItem.
            getClassificationItem(classificationIdentifier);
        if ((repository != null) && repository.hasChildren()) {
            document = getSets(document, ns,  repository.getChildren(), "");
        }
        
        return document;
    }

    /** 
     * Implementation of the OAI Verb ListRecords
     */
    private org.jdom.Document listRecords(HttpServletRequest request, org.jdom.Document header, Namespace ns) {
        org.jdom.Document document = header;
        
        // First: get some configuration data
        String resumptionTokenDir = MCRConfiguration.instance().
            getString(STR_OAI_RESUMPTIONTOKEN_DIR);
        I_MAXRETURNS = MCRConfiguration.instance().getInt(STR_OAI_MAXRETURNS, 100);
        String repositoryIdentifier = MCRConfiguration.instance().
            getString(STR_OAI_REPOSITORY_IDENTIFIER + "." + getServletName());
        
        checkTokenFile(resumptionTokenDir);
        
        int maxArguments = 2; //verb and metadataPrefix are required!
        
        // Second: get the arguments from the request
        String from[] = getParameter("from", request);
        if (from != null) {
            maxArguments++;
        }
        String until[] = getParameter("until", request); 
        if (until != null) {
            maxArguments++;
        }
        String set[] = getParameter("set", request); 
        if (set != null) {
            maxArguments++;
        }
        String resumptionToken[] = getParameter("resumptionToken", request); 
        if (resumptionToken != null) {
            maxArguments++;
        }
        String metadataPrefix[] = getParameter("metadataPrefix", request); 
        if (metadataPrefix == null) {
        	logger.info("Anfrage 'listRecords' enthält fehlerhafte Parameter.");
            return setError(document, ns, "badArgument", ERR_ILLEGAL_ARGUMENT);
        }
        //The number of arguments must not exceed maxArguments
        if (badArgumentsFound(request, maxArguments)) {
        	logger.info("Anfrage 'listRecords' enthält fehlerhafte Parameter.");
            return setError(document, ns, "badArgument", ERR_ILLEGAL_ARGUMENT);
        }
        
        Element eRoot = document.getRootElement();
        Element eRequest = eRoot.getChild("request", ns);
        eRequest.setAttribute("verb", "ListRecords");
        if (from != null) {
            eRequest.setAttribute("from", from[0]);
        }
        if (until != null) {
            eRequest.setAttribute("until", until[0]);
        }
        if (set != null) {
            eRequest.setAttribute("set", set[0]);
        }
        if (resumptionToken != null) {
            eRequest.setAttribute("resumptionToken", resumptionToken[0]);
        }
        eRequest.setAttribute("metadataPrefix", metadataPrefix[0]);
        
        //Check, if the requested metadata format is supported
        String format = MCRConfiguration.instance().
            getString(STR_OAI_METADATA_TRANSFORMER + "." + metadataPrefix[0], "empty");
        if (format.equals("empty")) {
        	logger.info("Anfrage 'listRecords' wurde wegen fehlendem Metadatenformat " + metadataPrefix[0] + " abgebrochen.");
            return setError(document, ns, "cannotDisseminateFormat", ERR_UNKNOWN_FORMAT);
        }
        //If I reach this point in code, I know the requested metadata format is supported,
        //so I may start constructing the records
        Element eListRecords = new Element("ListRecords", ns);
        
        if (resumptionToken != null) {
		    try {
		    	boolean deleteFile = false;
				File objectFile = new File(resumptionTokenDir + 
                    resumptionToken + STR_RESUMPTIONTOKEN_SUFFIX);
				FileInputStream fis = new FileInputStream(objectFile);
				ObjectInputStream ois = new ObjectInputStream(fis);
				int tokenNo = Integer.valueOf(resumptionToken[0].
                    substring(resumptionToken[0].indexOf('x') + 1, 
                    resumptionToken[0].lastIndexOf('x'))).intValue();
				int objectInFile = Integer.valueOf(resumptionToken[0].
                    substring(resumptionToken[0].lastIndexOf('x')+1)).intValue();
		
				int startObject = tokenNo * I_MAXRETURNS;
				int endObject = startObject + I_MAXRETURNS;

				if (endObject >= objectInFile) {
				    deleteFile = true;
				    endObject = objectInFile;
				}

				for (int a = 0; a < (startObject * 2); a++) {
		            ois.readObject();
        	    }

				for (int i = startObject; i < endObject; i++) {
				    Element record = (Element) ois.readObject();
				    eListRecords.addContent(record);
				}
                
				ois.close();
	 			fis.close();	

				if (deleteFile) {
                    objectFile.delete();
                }

				if (endObject < objectInFile) {
				    String newResumptionToken = resumptionToken[0].
                        substring(0, resumptionToken[0].indexOf('x')) + 
                        "x" + (tokenNo + 1) + "x" + objectInFile;
				    File newObjectFile = new File(resumptionTokenDir + 
                        newResumptionToken + STR_RESUMPTIONTOKEN_SUFFIX);
				    objectFile.renameTo(newObjectFile);
	            	eListRecords.addContent(MCRXMLHelper.
                        newJDOMElementWithContent("resumptionToken", ns, newResumptionToken));
				}
                eRoot.addContent(eListRecords);
		
	    	} catch (ClassNotFoundException e) {
	            logger.error(e.getMessage());
				return document;
		    } catch (IOException e) { 	
	            logger.error(e.getMessage());
                return setError(document, ns, "badResumptionToken", ERR_BAD_RESUMPTION_TOKEN);
            }
            
            org.jdom.Document newDocument = transform(document, format, metadataPrefix[0]);
            if (newDocument != null) {
                document = newDocument;
            }
            return document;
        }
        
        StringBuffer query = new StringBuffer("");
        String classificationIdentifier = MCRConfiguration.instance().
            getString(STR_OAI_CLASSIFICATION_ID + "." + getServletName());

        if (set == null) {
            query.append("/mycoreobject[metadata/*/*/@classid=\"").append(classificationIdentifier).append("\"]");
        } else {
            String categoryIdentifier = set[0].substring(set[0].lastIndexOf(':') + 1);
            query.append("/mycoreobject[@classid=\"").append(classificationIdentifier).
                append("\" and @categid=\"").append(categoryIdentifier).append("\"]");
        }
        if (from != null) {
            String date = from[0].substring(8) + "." + from[0].substring(5, 7) +
                "." + from[0].substring(0, 4);
            query.append(" and ").append("/mycoreobject[service.dates.date>=\"").append(date).
                append("\" and service.dates.date/@type=\"modifydate\"]");
        }
        if (until != null) {
            String date = until[0].substring(8) + "." + until[0].substring(5, 7) +
                "." + until[0].substring(0, 4);
            query.append(" and ").append("/mycoreobject[service.dates.date<=\"").append(date).
                append("\" and service.dates.date/@type=\"modifydate\"]");
        }

		logger.debug("Erzeugte Query: " + query.toString());
        MCRQueryResult qr = new MCRQueryResult();
        MCRXMLContainer qra = qr.setFromQuery("local", "document", query.toString());

		int fetchTo = 0;
		boolean toBig = false;
	
		if (qra.size() > I_MAXRETURNS) {
	    	fetchTo = I_MAXRETURNS;
		    toBig = true;
		} else {
            fetchTo = qra.size();
        }
	
        if (fetchTo == 0) {
            return setError(document, ns, "noRecordsMatch", ERR_NO_RECORDS_MATCH);
        }
        
		for (int i = 0; i < fetchTo; i++) {
            eListRecords.addContent(getRecordElement(qra.getId(i), ns));
		}
        
		if (toBig) {
		    Date tmpDate = new Date();
	    	long fileId = tmpDate.getTime();
		    int docs = qra.size()- I_MAXRETURNS;

		    try {
				String fileName = fileId+"x0x"+docs;
		
	 			FileOutputStream fos = new FileOutputStream(resumptionTokenDir + 
                    fileName + STR_RESUMPTIONTOKEN_SUFFIX);
			 	ObjectOutputStream oos = new ObjectOutputStream(fos);
		
				eListRecords.addContent(MCRXMLHelper.
                    newJDOMElementWithContent("resumptionToken", ns, fileName));
		
				for (int i = I_MAXRETURNS; i < qra.size(); i++) { 
				    oos.writeObject(getRecordElement(qra.getId(i), ns));
				} 
				oos.close();
	    	} catch (IOException e) { 
	            logger.error(e.getMessage());
            }
	    
		}
        eRoot.addContent(eListRecords);
        
        org.jdom.Document newDocument = transform(document, format, metadataPrefix[0]);
        if (newDocument != null) {
            document = newDocument;
        }
        return document;
    }

    /** 
    ** Helper function to check, if the resumption tokens are outdated
    */
    private void checkTokenFile(String dir) {
		File directory = new File(dir);
		File[] tokenList = directory.listFiles(new TokenFileFilter(directory, 
            STR_RESUMPTIONTOKEN_SUFFIX));
		if (tokenList == null) {
            return;
		}

		Date now = new Date();
		long fileAge = 0;
	
		for (int i = 0; i < tokenList.length; i++) {
            File tmpFile = tokenList[i];
            fileAge = now.getTime() - tmpFile.lastModified();
            long test = fileAge / (3600000 * 24);
            if ((fileAge / (3600000 * 24)) > 2) {
                tmpFile.delete();
	        	logger.debug("Token File wurde gelöscht.");
            }
        }
    }
    
    /** 
     * Implementation of the OAI Verb ListIdentifiers
     */
    private org.jdom.Document listIdentifiers(HttpServletRequest request, org.jdom.Document header, Namespace ns) {
        org.jdom.Document document = header;
        
        // First: get some configuration data
        String resumptionTokenDir = MCRConfiguration.instance().
            getString(STR_OAI_RESUMPTIONTOKEN_DIR);
        I_MAXRETURNS = MCRConfiguration.instance().getInt(STR_OAI_MAXRETURNS, 100);
        String repositoryIdentifier = MCRConfiguration.instance().
            getString(STR_OAI_REPOSITORY_IDENTIFIER + "." + getServletName());
        
        checkTokenFile(resumptionTokenDir);
        
        int maxArguments = 2; //verb and metadataPrefix are required!
        
        // Second: get the arguments from the request
        String from[] = getParameter("from", request);
        if (from != null) {
            maxArguments++;
        }
        String until[] = getParameter("until", request); 
        if (until != null) {
            maxArguments++;
        }
        String set[] = getParameter("set", request); 
        if (set != null) {
            maxArguments++;
        }
        String resumptionToken[] = getParameter("resumptionToken", request); 
        if (resumptionToken != null) {
            maxArguments++;
        }
        String metadataPrefix[] = getParameter("metadataPrefix", request); 
        if (metadataPrefix == null) {
        	logger.info("Anfrage 'listIdentifiers' wegen falschen Parametern abgebrochen.");
            return setError(document, ns, "badArgument", ERR_ILLEGAL_ARGUMENT);
        }
        //The number of arguments must not exceed maxArguments
        if (badArgumentsFound(request, maxArguments)) {
        	logger.info("Anfrage 'listIdentifiers' wegen falschen Parametern abgebrochen.");
            return setError(document, ns, "badArgument", ERR_ILLEGAL_ARGUMENT);
        }
        
        Element eRoot = document.getRootElement();
        Element eRequest = eRoot.getChild("request", ns);
        eRequest.setAttribute("verb", "ListIdentifiers");
        if (from != null) {
            eRequest.setAttribute("from", from[0]);
        }
        if (until != null) {
            eRequest.setAttribute("until", until[0]);
        }
        if (set != null) {
            eRequest.setAttribute("set", set[0]);
        }
        if (resumptionToken != null) {
            eRequest.setAttribute("resumptionToken", resumptionToken[0]);
        }
        eRequest.setAttribute("metadataPrefix", metadataPrefix[0]);
        
        //Check, if the requested metadata format is supported
        String format = MCRConfiguration.instance().
            getString(STR_OAI_METADATA_TRANSFORMER + "." + metadataPrefix[0], "empty");
        if (format.equals("empty")) {
        	logger.info("Anfrage 'listIdentifiers' wegen unbekanntem Metadatenformat " + metadataPrefix[0] + " abgebrochen.");
            return setError(document, ns, "cannotDisseminateFormat", ERR_UNKNOWN_FORMAT);
        }
        //If I reach this point in code, I know the requested metadata format is supported,
        //so I may start constructing the records
        Element eListIdentifiers = new Element("ListIdentifiers", ns);
        
        if (resumptionToken != null) {
		    try {
                boolean deleteFile = false;
				File objectFile = new File(resumptionTokenDir + 
                    resumptionToken + STR_RESUMPTIONTOKEN_SUFFIX);
				FileInputStream fis = new FileInputStream(objectFile);
				ObjectInputStream ois = new ObjectInputStream(fis);
				int tokenNo = Integer.valueOf(resumptionToken[0].
                    substring(resumptionToken[0].indexOf('x') + 1, 
                    resumptionToken[0].lastIndexOf('x'))).intValue();
				int objectInFile = Integer.valueOf(resumptionToken[0].
                    substring(resumptionToken[0].lastIndexOf('x')+1)).intValue();
		
				int startObject = tokenNo * I_MAXRETURNS;
				int endObject = startObject + I_MAXRETURNS;

				if (endObject >= objectInFile) {
				    deleteFile = true;
				    endObject = objectInFile;
				}

				for (int a = 0; a < (startObject * 2); a++) {
                    ois.readObject();
                }

				for (int i = startObject; i < endObject; i++) {
				    Element eHeader = (Element) ois.readObject();
				    eListIdentifiers.addContent(eHeader);
				}
                
				ois.close();
	 			fis.close();	

				if (deleteFile) {
                    objectFile.delete();
                }

				if (endObject < objectInFile) {
				    String newResumptionToken = resumptionToken[0].
                        substring(0, resumptionToken[0].indexOf('x')) + 
                        "x" + (tokenNo + 1) + "x" + objectInFile;
				    File newObjectFile = new File(resumptionTokenDir + 
                        newResumptionToken + STR_RESUMPTIONTOKEN_SUFFIX);
				    objectFile.renameTo(newObjectFile);
	            	eListIdentifiers.addContent(MCRXMLHelper.
                        newJDOMElementWithContent("resumptionToken", ns, newResumptionToken));
				}
                eRoot.addContent(eListIdentifiers);

		    } catch (ClassNotFoundException e) {
	            logger.error(e.getMessage());
				return document;
		    } catch (IOException e) {
	            logger.error(e.getMessage());
		    	logger.info("Anfrage konnte wegen falschem RESUMPTION TOKEN nicht bearbeitet werden.");
                return setError(document, ns, "badResumptionToken", ERR_BAD_RESUMPTION_TOKEN);
            }

            return document;
        }

        StringBuffer query = new StringBuffer("");
        String classificationIdentifier = MCRConfiguration.instance().
            getString(STR_OAI_CLASSIFICATION_ID + "." + getServletName());

        if (set == null) {
            query.append("/mycoreobject[metadata/*/*/@classid=\"").append(classificationIdentifier).append("\"]");
        } else {
            String categoryIdentifier = set[0].substring(set[0].lastIndexOf(':') + 1);
            query.append("/mycoreobject[@classid=\"").append(classificationIdentifier).
                append("\" and @categid=\"").append(categoryIdentifier).append("\"]");
        }
        if (from != null) {
            String date = from[0].substring(8) + "." + from[0].substring(5, 7) +
                "." + from[0].substring(0, 4);
            query.append(" and ").append("/mycoreobject[service.dates.date>=\"").append(date).
                append("\" and service.dates.date/@type=\"modifydate\"]");
        }
        if (until != null) {
            String date = until[0].substring(8) + "." + until[0].substring(5, 7) +
                "." + until[0].substring(0, 4);
            query.append(" and ").append("/mycoreobject[service.dates.date<=\"").append(date).
                append("\" and service.dates.date/@type=\"modifydate\"]");
        }
        
		logger.debug("Erzeugte Query: " + query.toString());
        MCRQueryResult qr = new MCRQueryResult();
        MCRXMLContainer qra = qr.setFromQuery("local", "document", query.toString());

		int fetchTo = 0;
		boolean toBig = false;
	
		if (qra.size() > I_MAXRETURNS) {
	    	fetchTo = I_MAXRETURNS;
		    toBig = true;
		} else {
            fetchTo = qra.size();
        }
	
        if (fetchTo == 0) {
            return setError(document, ns, "noRecordsMatch", ERR_NO_RECORDS_MATCH);
        }
        
		for (int i = 0; i < fetchTo; i++) {
            eListIdentifiers.addContent(getHeaderElement(qra.getId(i), ns));
		}
        
		if (toBig) {
		    Date tmpDate = new Date();
	    	long fileId = tmpDate.getTime();
		    int docs = qra.size()- I_MAXRETURNS;

		    try {
				String fileName = fileId+"x0x"+docs;
		
	 			FileOutputStream fos = new FileOutputStream(resumptionTokenDir + 
                    fileName + STR_RESUMPTIONTOKEN_SUFFIX);
			 	ObjectOutputStream oos = new ObjectOutputStream(fos);
		
				eListIdentifiers.addContent(MCRXMLHelper.
                    newJDOMElementWithContent("resumptionToken", ns, fileName));
		
				for (int i = I_MAXRETURNS; i < qra.size(); i++) { 
				    oos.writeObject(getHeaderElement(qra.getId(i), ns));
				} 
				oos.close();
	    	} catch (IOException e) { 
	            logger.error(e.getMessage());
            }
	    
	}
        eRoot.addContent(eListIdentifiers);
        
        return document;
    }

}

class TokenFileFilter implements FilenameFilter {
    String filter = null;
    
    public TokenFileFilter(File directory, String filterName) {
        filter = filterName;
    }
    
    public boolean accept(File dir, String name) {
        if (dir.isDirectory()) {
            if (name.endsWith(this.filter)) {
                return true;
            }
        }
	return false;
    }
}
