/**
 * $RCSfile: MCROAIDataProvider.java,v $
 * $Revision: 1.3 $ $Date: 2003/01/23 10:07:25 $
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

package mycore.services.oai;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.SimpleTimeZone;
import java.util.StringTokenizer;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpUtils;

import mycore.common.MCRConfiguration;
import mycore.common.MCRConfigurationException;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.output.XMLOutputter;

/**
 * This class implements an OAI Data Provider for MyCoRe and Miless
 *
 * @author Werner Gresshoff
 *
 * @version $Revision: 1.3 $ $Date: 2003/01/23 10:07:25 $
 **/
public class MCROAIDataProvider extends HttpServlet {
    static Logger logger = Logger.getLogger(MCROAIDataProvider.class);

	// repository independent settings which are used for all repositories
    private static final String STR_OAI_ADMIN_EMAIL = "MCR.oai.adminemail"; //EMail address of oai admin
    
    // repository specific settings. Should be followed by .Servletname so the OAI client
    // may decide which settings belong to itself.
    private static final String STR_OAI_REPOSITORY_NAME = "MCR.oai.repositoryname"; // Name of the repository
    private static final String STR_OAI_REPOSITORY_IDENTIFIER = "MCR.oai.repositoryidentifier"; // Identifier of the repository
	private static final String STR_OAI_FRIENDS = "MCR.oai.friends"; // a colon (:) separated list of other OAI repsoitories (friends)
	private static final String STR_OAI_QUERYSERVICE = "MCR.oai.queryservice"; // implementing class of MCROAIQuery
	private static final String STR_OAI_SETSCHEME = "MCR.oai.setscheme"; // the classification id which serves as scheme for the OAI set structure
    private static final String STR_OAI_RESUMPTIONTOKEN_DIR = "MCR.oai.resumptiontoken.dir"; // temporary Directory
    private static final String STR_OAI_RESUMPTIONTOKEN_TIMEOUT="MCR.oai.resumptiontoken.timeout"; // timeout, after which a resumption token will be deleted	
    private static final String STR_OAI_MAXRETURNS = "MCR.oai.maxreturns"; //maximum number of returned list sets
    
    // If there are other metadata formats available, all need a namespace and schema entry
    // of it's own, e.g. 
    // MCR.oai.metadata.namespace.olac=http://www.language-archives.org/OLAC/0.2/
    // MCR.oai.metadata.schema.olac=http://www.language-archives.org/OLAC/olac-0.2.xsd
    private static final String STR_OAI_METADATA_NAMESPACE = "MCR.oai.metadata.namespace"; 
    private static final String STR_OAI_METADATA_SCHEMA = "MCR.oai.metadata.schema"; 
    
    // Some constants referring to metadata formats (must be known for dc)
    private static final String STR_DC_NAMESPACE = "http://www.openarchives.org/OAI/2.0/oai_dc/";
    private static final String STR_DC_SCHEMA = "http://www.openarchives.org/OAI/2.0/oai_dc.xsd";

    private static final String STR_OAI_NAMESPACE = "http://www.openarchives.org/OAI/";
    private static final String STR_OAI_VERSION = "2.0";
    private static final String STR_SCHEMA_INSTANCE = "http://www.w3.org/2001/XMLSchema-instance";

    private static final String STR_RESUMPTIONTOKEN_SUFFIX = ".token";
    
    private static final String ERR_FAULTY_VERB = "No verb or too much verbs";
    private static final String ERR_ILLEGAL_VERB = "Illegal verb";
    private static final String ERR_ILLEGAL_ARGUMENT = "Illegal argument";
    private static final String ERR_UNKNOWN_ID = "Unknown identifier";

    private static final String[] STR_VERBS = {"GetRecord", "Identify",
        "ListIdentifiers", "ListMetadataFormats", "ListRecords",
        "ListSets"};

	/**
	 * Method init. Initializes the Servlet when first loaded
	 * @param config Configuration data
	 * @throws ServletException
	 */
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
    }

	/**
	 * Method destroy. Automatically destroys the Servlet.
	 */
    public void destroy() {
    }

	/**
	 * Method doGet. Handles the HTTP <code>GET</code> method.
	 * @param request servlet request
	 * @param response servlet response
	 * @throws ServletException
	 * @throws IOException
	 */
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        processRequest(request, response);
    }
    
	/**
	 * Method doPost. Handles the HTTP <code>POST</code> method.
	 * @param request servlet request
	 * @param response servlet response
	 * @throws ServletException
	 * @throws IOException
	 */
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        processRequest(request, response);
    }
    
	/**
	 * Method processRequest. Processes requests for both HTTP <code>GET</code> and <code>POST</code> methods.
	 * @param request servlet request
	 * @param response servlet response
	 * @throws ServletException
	 * @throws IOException
	 */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
    	MCRConfiguration.instance().reload(true);
    	PropertyConfigurator.configure(MCRConfiguration.instance().getLoggingProperties());
        response.setContentType("text/xml; charset=UTF-8");
        
        // Exceptions must be caught...
        try {
            ServletOutputStream out = response.getOutputStream();
            XMLOutputter outputter = new XMLOutputter("   ");
            outputter.setNewlines(true);
	        outputter.setEncoding("UTF-8");

            Namespace ns = Namespace.getNamespace(STR_OAI_NAMESPACE + "2.0/");
            Element eRoot  = new Element("OAI-PMH", ns);
            org.jdom.Document header = new org.jdom.Document(eRoot);
            Namespace xsi = Namespace.getNamespace("xsi", STR_SCHEMA_INSTANCE);

            eRoot.addNamespaceDeclaration(xsi);
            eRoot.setAttribute("schemaLocation", STR_OAI_NAMESPACE + "2.0/ "  + STR_OAI_NAMESPACE + "2.0/OAI-PMH.xsd", xsi);
            
            // add "responseDate"...
            Date today = new Date();
    	    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
            
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
            	document = addError(header, "badVerb", ERR_FAULTY_VERB);
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
	            	logger.info("Request with a bad verb:" + verb[0]);
                	document = addError(header, "badVerb", ERR_ILLEGAL_VERB);
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

	/**
	 * Method getServletInfo. Returns a short description of the servlet.
	 * @return String
	 */
    public String getServletInfo() {
        return "This class implements an OAI Data Provider for MyCoRe";
    }
    
	/**
	 * Method getParameter. Extracts the value of <i>p</i> out of <i>request</i> and returns it.
	 * @param p The name of the parameter to extract.
	 * @param request The HttpServletRequest to extract the parameter from.
	 * @return String[] The values of the parameter.
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
	 * Method addError. insert <error>-Tag in <i>document</i>.
	 * @param document
	 * @param errorCode 
	 * @param errorDescription
	 * @return Document
	 */
    private org.jdom.Document addError(org.jdom.Document document, String errorCode, String errorDescription) {
        Element eRoot = document.getRootElement();
        Namespace ns = eRoot.getNamespace();
        Element eError = new Element("error", ns);
        eError.setAttribute("code", errorCode);
        eError.addContent(errorDescription);
        eRoot.addContent(eError);
                
        return document;
    }
    
	/**
	 * Method badArguments. Check the <i>request</i> for too much parameters
	 * @param request
	 * @param maxargs
	 * @return boolean True, if too much parameters were found
	 */
    private boolean badArguments(HttpServletRequest request, int maxargs) {
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
     * Method newElementWithContent. Create a new JDOM-Element with some content.
     * @param elementName the element name to be created.
     * @param ns the namespace the element should be created in.
     * @param content
     * @return Element
     */
    private Element newElementWithContent(String elementName, Namespace ns, String content) {
        Element element = new Element(elementName, ns);
        element.addContent(content);
        return element;
    }

	/**
	 * Method checkIdentifier. Check for conformance with the rules and the existence of the given <i>identifier</i>.
	 * @param identifier Identifier to be checked.
	 * @param repositoryIdentifier The repository identifier (from .properties file)
	 * @return boolean
	 */
    private boolean checkIdentifier(String identifier, String repositoryIdentifier) {
		// First, check if the identifier starts with "oai"
		if (!identifier.toUpperCase().startsWith("OAI:")) {
		    return false;
		} else {
	    	int lastColon = identifier.lastIndexOf(":");
	    
	    	if ((lastColon == 3) || !identifier.substring(4, lastColon).equals(repositoryIdentifier)) {
				return false;
	    	}
		    MCRConfiguration config = MCRConfiguration.instance();
		    String queryImplementation = config.getString(STR_OAI_QUERYSERVICE);
		    try {
			    MCROAIQuery query = (MCROAIQuery) config.getInstanceOf(queryImplementation);
			    return query.exists(identifier.substring(lastColon + 1));
		    } catch (MCRConfigurationException mcrx) {
		    	logger.fatal("Die OAIQuery-Klasse ist nicht konfiguriert.");
		    	return false;
		    }
		} 
        
		return true;
    }

	/**
	 * Method deleteOutdatedTokenFiles. Helper function to delete outdated resumption token files
	 */
    private void deleteOutdatedTokenFiles() {
	    MCRConfiguration config = MCRConfiguration.instance();
	    String dir;
	    int timeout;
	    
	    try {
		    dir = config.getString(STR_RESUMPTIONTOKEN_DIR);
		    timeout = config.getInt(STR_RESUMPTIONTOKEN_TIMEOUT, 72);
	    } catch (MCRConfigurationException mcrx) {
	    	logger.error("Die Property '" + STR_RESUMPTIONTOKEN_DIR + "' ist nicht konfiguriert. Resumption Tokens werden nicht unterstützt.");
	    	return;
	    } catch (NumberFormatException nfx) {
	    	timeout = 72;
	    }
	    
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
            fileAge = now.getTime() - tmpFile.lastModified(); // in milliseconds!
            if (fileAge > timeout * 3600000) {
	        	logger.debug("Token File " + tmpFile.getName() + " wird gelöscht.");
                tmpFile.delete();
            }
        }
    }
    
	/**
	 * Method listFromResumptionToken. Get the element list from the Resumption Token File and add them to the <i>list</i>.
	 * @param list The list to which the new Elements are added.
	 * @param resumptionToken The Resumption Token to process.
	 * @return Element The new List
	 * @throws IOException
	 */
    private Element listFromResumptionToken(Element list, String resumptionToken) throws IOException {
		try {
		   	String resumptionTokenDir = config.getString(STR_OAI_RESUMPTIONTOKEN_DIR);
		   	int maxreturns = config.getInt(STR_OAI_MAXRETURNS);
			File objectFile = new File(resumptionTokenDir + 
                resumptionToken + STR_RESUMPTIONTOKEN_SUFFIX);
			FileInputStream fis = new FileInputStream(objectFile);
			ObjectInputStream ois = new ObjectInputStream(fis);
			int tokenNo = Integer.valueOf(resumptionToken
                .substring(resumptionToken.indexOf('x') + 1, 
                .resumptionToken.lastIndexOf('x'))).intValue();
			int objectsInFile = Integer.valueOf(resumptionToken
                .substring(resumptionToken.lastIndexOf('x')+1)).intValue();
		
			int cursor = tokenNo * maxreturns;
			int endObject = maxreturns;

			if (endObject > objectsInFile) {
			    endObject = objectsInFile;
			}

			for (int i = 0; i < endObject; i++) {
			    Element record = (Element) ois.readObject();
			    list.addContent(record);
			}
			
            Namespace ns = list.getNamespace();  
			Element eResumptionToken = new Element("resumptionToken", ns);
			eResumptionToken.setAttribute("completeListSize", Integer.toString(tokenNo * maxreturns + objectsInFile));
			eResumptionToken.setAttribute("cursor", Integer.toString(tokenNo * maxreturns));
				
			if (endObject < objectsInFile) {
			    String newResumptionToken = resumptionToken.substring(0, resumptionToken.indexOf('x')) + 
                    "x" + (tokenNo + 1) + "x" + (objectsInFile - endObject);
			    File newObjectFile = new File(resumptionTokenDir + 
                    newResumptionToken + STR_RESUMPTIONTOKEN_SUFFIX);
				FileOutputStream fos = new FileOutputStream(newObjectFile);
				ObjectOutputStream oos = new ObjectOutputStream(fos);
					
				for (int i = endObject; i < objectsInFile; i++) {
				    Element record = (Element) ois.readObject();
				    oos.writeObject(record);
				}
					
				oos.close();
				fos.close();
					
				Calendar calendar = new GregorianCalendar();
				Date now = new Date();
			    int timeout = config.getInt(STR_RESUMPTIONTOKEN_TIMEOUT, 72);
				calendar.setTime(now);
				calendar.add(Calendar.HOUR, timeout);
	            Date timeoutDate = calendar.getTime();
	    	    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
       		    SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
            
           		String sDate = dateFormat.format(timeoutDate) + "T" + timeFormat.format(timeoutDate) + "Z";
				eResumptionToken.setAttribute("expirationDate", sDate);
			}
				
           	list.addContent(eResumptionToken);
            	
			ois.close();
 			fis.close();	
	    } catch (MCRConfigurationException mcrx) {
            logger.fatal(mcrx.getMessage());
			return list;
    	} catch (ClassNotFoundException e) {
            logger.error(e.getMessage());
			return list;
	    } catch (IOException e) { 	
	    	throw new IOException(e);
        }
    }
    
	/**
	 * Method identify. Implementation of the OAI Verb Identify.
	 * @param request
	 * @param header
	 * @return Document
	 */
    private org.jdom.Document identify(HttpServletRequest request, org.jdom.Document header) {
    	logger.info("Harvester hat 'Identify' angefordert");
        org.jdom.Document document = header;
        
        if (badArguments(request, 1)) {
        	logger.info("Es wurden überflüssige Argumente an die Anfrage übergeben. Nach OAI 2.0 erfolgt hier ein Abbruch.");
            return addError(document, "badArgument", ERR_ILLEGAL_ARGUMENT);
        }

		try {
	        String repositoryIdentifier = MCRConfiguration.instance()
    	        .getString(STR_OAI_REPOSITORY_IDENTIFIER + "." + getServletName());
	        String repositoryName = MCRConfiguration.instance()
	        	.getString(STR_OAI_REPOSITORY_NAME + "." + getServletName());
		} catch (MCRConfigurationException mcrx) {
			logger.fatal("Missing configuration item: either " + STR_OAI_REPOSITORY_IDENTIFIER + "." + getServletName()
				+ " or " + STR_OAI_REPOSITORY_NAME + "." + getServletName() + " is missing.");
			return null;
		}
		
        Element eRoot = document.getRootElement();
        Namespace ns = eRoot.getNamespace();
        Element eRequest = eRoot.getChild("request", ns);
        eRequest.setAttribute("verb", "Identify");
        Element eIdentify = new Element("Identify", ns);
        eIdentify.addContent(newElementWithContent("repositoryName", ns, repositoryName));
        eIdentify.addContent(newElementWithContent("baseURL", ns, 
            URLEncoder.encode(HttpUtils.getRequestURL(request).toString())));
        eIdentify.addContent(newElementWithContent("protocolVersion", ns, STR_OAI_VERSION));
        String adminEmail = MCRConfiguration.instance().getString(STR_OAI_ADMIN_EMAIL);
        eIdentify.addContent(newElementWithContent("adminEmail", ns, adminEmail));
        eIdentify.addContent(newElementWithContent("earliestDatestamp", ns, "2000-01-01"));
        eIdentify.addContent(newElementWithContent("deletedRecord", ns, "no"));
        eIdentify.addContent(newElementWithContent("granularity", ns, "YYYY-MM-DD"));
        // If we don't support compression, this SHOULD NOT be mentioned, so it is outmarked
		// eIdentify.addContent(newElementWithContent("compression", ns, "identity"));
        
        Element eOAIDescription = new Element("description", ns);
        Namespace idns = Namespace.getNamespace(STR_OAI_NAMESPACE + "2.0/oai-identifier");
        Element eOAIIdentifier = new Element("oai-identifier", idns);
        Namespace xsi = Namespace.getNamespace("xsi", STR_SCHEMA_INSTANCE);
        eOAIIdentifier.addNamespaceDeclaration(xsi);
        eOAIIdentifier
        	.setAttribute("schemaLocation", STR_OAI_NAMESPACE + "2.0/oai-identifier "  + STR_OAI_NAMESPACE + "2.0/oai-identifier.xsd", xsi);

        eOAIIdentifier.addContent(newElementWithContent("scheme", idns, "oai"));
        eOAIIdentifier.addContent(newElementWithContent("repositoryIdentifier", idns, repositoryIdentifier));  
        eOAIIdentifier.addContent(newElementWithContent("delimiter", idns, ":"));
        eOAIIdentifier.addContent(newElementWithContent("sampleIdentifier", idns, "oai:" + repositoryIdentifier + ":MyCoReDemoDC_Document_1"));

        eOAIDescription.addContent(eOAIIdentifier);
        eIdentify.addContent(eOAIDescription);

		try {
	        String friends = MCRConfiguration.instance()
    	        .getString(STR_OAI_FRIENDS + "." + getServletName());
    	    StringTokenizer tokenizer = new StringTokenizer(friends, ":");
    	    if (tokenizer.countTokens() > 0) {
		        Element eFriendsDescription = new Element("description", ns);
    	    	Namespace frns = Namespace.getNamespace(STR_OAI_NAMESPACE + "2.0/friends/");
    	    	Element eFriends = new Element("friends", frns);
    	    	eFriends.addNamespaceDeclaration(xsi);
    	    	eFriends
    	    		.setAttribute("schemaLocation", STR_OAI_NAMESPACE + "2.0/friends/ "  + STR_OAI_NAMESPACE + "2.0/friends.xsd", xsi);
    	    	
    	    	while (tokenizer.hasMoreTokens()) {
    	    		eFriends.addContent(newElementWithContent("baseURL", frns, tokenizer.nextToken()));	
    	    	}
    	    	
    	    	eIdentify.addContent(eFriends);
    	    }
		} catch (MCRConfigurationException mcrx) {
			// Nothing to be done here (realy, not kidding!)
		}

        eRoot.addContent(eIdentify);
        
        return document;
    }

	/**
	 * Method listMetadataFormats. Implementation of the OAI Verb ListMetadataFormats.
	 * @param request
	 * @param header
	 * @return Document
	 */
    private org.jdom.Document listMetadataFormats(HttpServletRequest request, org.jdom.Document header) {
    	logger.info("Harvester hat 'listMetadatFormats' angefordert");
        org.jdom.Document document = header;
        
        Element eRoot = document.getRootElement();
        Namespace ns = eRoot.getNamespace();
        
		try {
	        String repositoryIdentifier = MCRConfiguration.instance()
    	        .getString(STR_OAI_REPOSITORY_IDENTIFIER + "." + getServletName());
		} catch (MCRConfigurationException mcrx) {
			logger.fatal("Missing configuration item: " + STR_OAI_REPOSITORY_IDENTIFIER + "." + getServletName()
				+ " is missing.");
			return null;
		}
        
        // First; check if there was an identifier in the request
        String identifier[] = getParameter("identifier", request); 
        if (identifier == null) {
            if (badArguments(request, 1)) {
            	logger.info("Anfrage 'listMetadataFormats' wurde wegen fehlendem Parameter abgebrochen.");
                return addError(document, "badArgument", ERR_ILLEGAL_ARGUMENT);
            }
            Element eRequest = eRoot.getChild("request", ns);
            eRequest.setAttribute("verb", "ListMetadataFormats");
        } else if (identifier.length > 1) {
            //Es ist nur ein Identifier erlaubt!
          	logger.info("Anfrage 'listMetadataFormats' wurde wegen zu vieler Parameter abgebrochen.");
            return addError(document, "badArgument", ERR_ILLEGAL_ARGUMENT);
        } else {
            Element eRequest = eRoot.getChild("request", ns);
            eRequest.setAttribute("verb", "ListMetadataFormats");
            eRequest.setAttribute("identifier", identifier[0]);
            if (!checkIdentifier(identifier[0], repositoryIdentifier)) {
            	logger.info("Anfrage 'listMetadataFormats' wurde wegen falscher ID abgebrochen.");
                return addError(document, "idDoesNotExist", ERR_UNKNOWN_ID);
            }
        }
        
        // The supported metadata formats are only returned if no identifier was given, or
        // the identifier was found in the repository
        Element eListMetadataFormats = new Element("ListMetadataFormats", ns);
        Element dcMetadataFormat = new Element("metadataFormat", ns);
        dcMetadataFormat.addContent(newElementWithContent("metadataPrefix", ns, "oai_dc"));
        dcMetadataFormat.addContent(newElementWithContent("schema", ns, STR_DC_SCHEMA));
        dcMetadataFormat.addContent(newJDOMElementWithContent("metadataNamespace", ns, STR_DC_NAMESPACE));
        eListMetadataFormats.addContent(dcMetadataFormat);
        
        Properties properties = MCRConfiguration.instance().
            getProperties(STR_OAI_METADATA_NAMESPACE);
        Enumeration propertiesNames = properties.propertyNames();
        while (propertiesNames.hasMoreElements()) {
            String name = (String) propertiesNames.nextElement();
            String metadataPrefix = name.substring(name.lastIndexOf(".") + 1);
            Element eMetadataFormat = new Element("metadataFormat", ns);
            eMetadataFormat.addContent(newElementWithContent("metadataPrefix", ns, metadataPrefix));
            eMetadataFormat.addContent(newElementWithContent("schema", ns, MCRConfiguration.instance()
                .getString(STR_OAI_METADATA_SCHEMA + "." + metadataPrefix)));
            eMetadataFormat.addContent(newElementWithContent("metadataNamespace", ns, MCRConfiguration.instance()
                .getString(STR_OAI_METADATA_NAMESPACE + "." + metadataPrefix)));
            eListMetadataFormats.addContent(eMetadataFormat);
        }
        
        eRoot.addContent(eListMetadataFormats);

        return document;
    }
    
	/**
	 * Method listSets. Implementation of the OAI Verb ListSets.
	 * @param request The servlet request.
	 * @param header The document so far
	 * @return Document
	 */
    private org.jdom.Document listSets(HttpServletRequest request, org.jdom.Document header) {
        org.jdom.Document document = header;
        Element eRoot = document.getRootElement();
        Namespace ns = eRoot.getNamespace();
        
        deleteOutdatedTokenFiles();
        
        int maxArguments = 0;
        String resumptionToken[] = getParameter("resumptionToken", request); 
        if (resumptionToken != null) {
            maxArguments++;
        }
        //The number of arguments must not exceed maxArguments
        if (badArguments(request, maxArguments)) {
        	logger.info("Anfrage 'listSets' enthält fehlerhafte Parameter.");
            return addError(document, "badArgument", ERR_ILLEGAL_ARGUMENT);
        }
        
        Element eRequest = eRoot.getChild("request", ns);
        eRequest.setAttribute("verb", "ListSets");
        if (resumptionToken != null) {
            eRequest.setAttribute("resumptionToken", resumptionToken[0]);
        }
		Element eListSets = new Element("ListSets", ns);
        
	    MCRConfiguration config = MCRConfiguration.instance();
	    
        if (resumptionToken != null) {
		    try {
				eListSets = listFromResumptionToken(eListSets, resumptionToken[0]);
		        eRoot.addContent(eListSets);
		    } catch (IOException e) { 	
	            logger.error(e.getMessage());
                return addError(document, "badResumptionToken", ERR_BAD_RESUMPTION_TOKEN);
            }
            
            return document;
        }
        
	    List sets;
	    
	    try {
        	String classificationIdentifier = config.getString(STR_OAI_SETSCHEME + "." + getServletName());
    	    logger.debug("Suche in Klassifikation: " + classificationIdentifier);
        
		    String queryImplementation = config.getString(STR_OAI_QUERYSERVICE);
		    MCROAIQuery query = (MCROAIQuery) config.getInstanceOf(queryImplementation);
		    sets = new ArrayList(query.listSets(classificationIdentifier));
	    } catch (MCRConfigurationException mcrx) {
	    	logger.fatal(mcrx.getMessage());
	    	return document;
	    }
	    
	    //Hier erfolgt noch die Verarbeitung der Datensätze!
        
        return document;
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
}
