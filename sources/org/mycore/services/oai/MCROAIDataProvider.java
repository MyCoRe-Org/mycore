/**
 * $RCSfile: MCROAIDataProvider.java,v $
 * $Revision: 1.23 $ $Date: 2003/01/31 12:48:25 $
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TimeZone;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRConfigurationException;
import org.mycore.common.MCRException;
import org.mycore.common.xml.MCRXSLTransformation;

/**
 * This class implements an OAI Data Provider for MyCoRe and Miless
 *
 * @author Werner Gresshoff
 *
 * @version $Revision: 1.23 $ $Date: 2003/01/31 12:48:25 $
 **/
public class MCROAIDataProvider extends HttpServlet {
    /**
	 * <code>serialVersionUID</code> introduced for compatibility
	 * with JDK 1.4 (a should have)
	 */
	private static final long serialVersionUID = 4121136939476267829L;

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
    private static final String STR_OAI_RESTRICTION_CLASSIFICATION = "MCR.oai.restriction.classification"; //Classification and...
    private static final String STR_OAI_RESTRICTION_CATEGORY = "MCR.oai.restriction.category"; //...Category to restrict the access to
    private static final String STR_OAI_METADATA_TRANSFORMER = "MCR.oai.metadata.transformer"; 
    private static final String STR_STANDARD_TRANSFORMER = "oai_dc";
    
    // If there are other metadata formats available, all need a namespace and schema entry
    // of it's own, e.g. 
    // MCR.oai.metadata.namespace.olac=http://www.language-archives.org/OLAC/0.2/
    // MCR.oai.metadata.schema.olac=http://www.language-archives.org/OLAC/olac-0.2.xsd
    private static final String STR_OAI_METADATA_NAMESPACE = "MCR.oai.metadata.namespace"; 
    private static final String STR_OAI_METADATA_ELEMENT = "MCR.oai.metadata.element"; 
    private static final String STR_OAI_METADATA_SCHEMA = "MCR.oai.metadata.schema"; 
    
    // Following the DINI recommendation for OAI repositories
    // (http://www.dini.de/documents/OAI-Empfehlungen-Okt2003-de.pdf) there
    // there should be some classifications/categories defined which are very
    // restrictive in usage. So this client provides a possibility to map
    // from a far more detailed classification system to the simplistic
    // DINI specification.
    private static final String STR_OAI_CATEGORY_MAPPING = "MCR.oai.dini-mapping";
    
    // Some constants referring to metadata formats (must be known for dc)
    private static final String STR_DC_NAMESPACE = "http://www.openarchives.org/OAI/2.0/oai_dc/";
    private static final String STR_DC_SCHEMA = "http://www.openarchives.org/OAI/2.0/oai_dc.xsd";

    private static final String STR_OAI_NAMESPACE = "http://www.openarchives.org/OAI/";
    private static final String STR_OAI_VERSION = "2.0";
    private static final String STR_SCHEMA_INSTANCE = "http://www.w3.org/2001/XMLSchema-instance";

    private static final String STR_RESUMPTIONTOKEN_SUFFIX = ".token";
    
    private static final String STR_GRANULARITY = "yyyy-MM-dd";
    private static final String STR_FIRST_DATE = "2000-01-01";
    
    private static final String ERR_FAULTY_VERB = "No verb or too much verbs";
    private static final String ERR_ILLEGAL_VERB = "Illegal verb";
    private static final String ERR_ILLEGAL_ARGUMENT = "Illegal argument";
    private static final String ERR_UNKNOWN_ID = "Unknown identifier";
    private static final String ERR_BAD_RESUMPTION_TOKEN = "Bad resumption token";
    private static final String ERR_NO_RECORDS_MATCH = "No results where found with your search criteria";
    private static final String ERR_UNKNOWN_FORMAT = "Unknown metadata format";

    private static final String[] STR_VERBS = {"GetRecord", "Identify",
        "ListIdentifiers", "ListMetadataFormats", "ListRecords",
        "ListSets"};

    private static Map mappings = null;
    
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
    	PropertyConfigurator.configure(MCRConfiguration.instance().getLoggingProperties());
        response.setContentType("text/xml; charset=UTF-8");
        
        // Exceptions must be caught...
        try {
            ServletOutputStream out = response.getOutputStream();
            XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());

            Namespace ns = Namespace.getNamespace(STR_OAI_NAMESPACE + "2.0/");
            Element eRoot  = new Element("OAI-PMH", ns);
            org.jdom.Document header = new org.jdom.Document(eRoot);
            Namespace xsi = Namespace.getNamespace("xsi", STR_SCHEMA_INSTANCE);

            eRoot.addNamespaceDeclaration(xsi);
            eRoot.setAttribute("schemaLocation", STR_OAI_NAMESPACE + "2.0/ "  + STR_OAI_NAMESPACE + "2.0/OAI-PMH.xsd", xsi);
            
            // add "responseDate"...
       		String sDate = getUTCDate(0);
            Element eDate = new Element("responseDate", ns);
            eDate.setText(sDate);
            eRoot.addContent(eDate);
            
            // add "request"...
            String url = request.getRequestURL().toString();
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
                    document = getRecord(request, header);
                } else if (verb[0].equalsIgnoreCase(STR_VERBS[1])) {
                    document = identify(request, header);
                } else if (verb[0].equalsIgnoreCase(STR_VERBS[2])) {
                    document = listIdentifiers(request, header);
                } else if (verb[0].equalsIgnoreCase(STR_VERBS[3])) {
                    document = listMetadataFormats(request, header);
                } else if (verb[0].equalsIgnoreCase(STR_VERBS[4])) {
                    document = listRecords(request, header);
                } else if (verb[0].equalsIgnoreCase(STR_VERBS[5])) {
                    document = listSets(request, header);
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
    	Map parameterMap = request.getParameterMap();
    	if (parameterMap.size() > maxargs) {
    		return true;
    	}
    	
    	//If there are no wrong parameters, it it possible some 
    	//parameters are doubled
    	Collection values = parameterMap.values();
    	Iterator iterator = values.iterator();
    	while (iterator.hasNext()) {
    		String[] value = (String[]) iterator.next();
    		if (value.length > 1) {
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

	private String getPrefix(String token) {
	    MCRConfiguration config = MCRConfiguration.instance();
	    String dir = new String();
	    
	    try {
		    dir = config.getString(STR_OAI_RESUMPTIONTOKEN_DIR);
	    } catch (MCRConfigurationException mcrx) {
	    	logger.error("Die Property '" + STR_OAI_RESUMPTIONTOKEN_DIR + "' ist nicht konfiguriert. Resumption Tokens werden nicht unterstützt.");
	    	return null;
	    }
	    
		File directory = new File(dir);
		File[] tokenList = directory.listFiles(new TokenFileFilter(directory, 
            STR_RESUMPTIONTOKEN_SUFFIX));
		if (tokenList == null) {
            return null;
		}
	
		for (int i = 0; i < tokenList.length; i++) {
            File tmpFile = tokenList[i];
            String fileName = tmpFile.getName();
            if (fileName.indexOf(token) != -1) {
            	StringTokenizer tokenizer = new StringTokenizer(fileName, ".");
            	String name = tokenizer.nextToken();
            	name = tokenizer.nextToken();
            	
            	return name;
            }
		}
		
		return null;
	}
	
	/**
	 * Method deleteOutdatedTokenFiles. Helper function to delete outdated resumption token files
	 */
    private void deleteOutdatedTokenFiles() {
	    MCRConfiguration config = MCRConfiguration.instance();
	    String dir = new String();
	    int timeout;
	    
	    try {
		    dir = config.getString(STR_OAI_RESUMPTIONTOKEN_DIR);
		    timeout = config.getInt(STR_OAI_RESUMPTIONTOKEN_TIMEOUT, 72);
	    } catch (MCRConfigurationException mcrx) {
	    	logger.error("Die Property '" + STR_OAI_RESUMPTIONTOKEN_DIR + "' ist nicht konfiguriert. Resumption Tokens werden nicht unterstützt.");
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
    private Element listFromResumptionToken(Element list, String resumptionToken, String prefix) throws IOException {
	    MCRConfiguration config = MCRConfiguration.instance();
		try {
		   	String resumptionTokenDir = config.getString(STR_OAI_RESUMPTIONTOKEN_DIR) + File.separator;
		   	int maxreturns = config.getInt(STR_OAI_MAXRETURNS);
			File objectFile = new File(resumptionTokenDir +
                resumptionToken + "." + prefix + STR_RESUMPTIONTOKEN_SUFFIX);
			if (!objectFile.exists()) {
				return null;
			}
			FileInputStream fis = new FileInputStream(objectFile);
			ObjectInputStream ois = new ObjectInputStream(fis);
			int tokenNo = Integer.valueOf(resumptionToken
                .substring(resumptionToken.indexOf('x') + 1, 
                resumptionToken.lastIndexOf('x'))).intValue();
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
			eResumptionToken.setAttribute("completeListSize", Integer.toString(cursor + objectsInFile));
			eResumptionToken.setAttribute("cursor", Integer.toString(cursor));
				
			if (endObject < objectsInFile) {
			    String newResumptionToken = resumptionToken.substring(0, resumptionToken.indexOf('x')) + 
                    "x" + (tokenNo + 1) + "x" + (objectsInFile - endObject);
			    File newObjectFile = new File(resumptionTokenDir +
                    newResumptionToken + "." + prefix + STR_RESUMPTIONTOKEN_SUFFIX);
				FileOutputStream fos = new FileOutputStream(newObjectFile);
				ObjectOutputStream oos = new ObjectOutputStream(fos);
					
				for (int i = endObject; i < objectsInFile; i++) {
				    Element record = (Element) ois.readObject();
				    oos.writeObject(record);
				}
					
				oos.close();
				fos.close();
					
			    int timeout = config.getInt(STR_OAI_RESUMPTIONTOKEN_TIMEOUT, 72);
           		String sDate = getUTCDate(timeout);
           		eResumptionToken.addContent(newResumptionToken);
				eResumptionToken.setAttribute("expirationDate", sDate);
			}
				
           	list.addContent(eResumptionToken);
            	
			ois.close();
 			fis.close();	
	    } catch (MCRConfigurationException mcrx) {
            logger.fatal(mcrx.getMessage());
    	} catch (ClassNotFoundException e) {
            logger.error(e.getMessage());
	    } catch (IOException e) { 	
	    	throw new IOException(e.getMessage());
		}
        
        return list;
    }
    
	/**
	 * Method getDate. Gets the date from an ISO8601 string or null
	 * @param date a ISO8601 conformant date string
	 * @return Date the date or null
	 */
    private Date getDate(String date) {
    	logger.debug("Given date: " + date);
   	    SimpleDateFormat dateFormat = new SimpleDateFormat(STR_GRANULARITY);
   	    if (date.length() > STR_GRANULARITY.length()) {
   	    	return null;
   	    }
		ParsePosition pos = new ParsePosition(0);
		Date currentDate = dateFormat.parse(date, pos);

    	return currentDate;
    }
    
	/**
	 * Method getUTCDate. The actual date and time (GMT).
	 * @param timeout. offset to add to get a future time.
	 * @return String the date and time as string.
	 */
    private String getUTCDate(int timeout) {
		Calendar calendar = new GregorianCalendar();
		Date now = new Date();
		calendar.setTime(now);
   	    SimpleDateFormat dateFormat = new SimpleDateFormat(STR_GRANULARITY);
	    SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
        TimeZone tz = timeFormat.getTimeZone();
        // compute milliseconds to hours...
        int offset = Math.abs(tz.getRawOffset() / 3600000) * -1;
		calendar.add(Calendar.HOUR, offset);
		calendar.add(Calendar.HOUR, timeout);
        Date timeoutDate = calendar.getTime();
            
   		String sDate = dateFormat.format(timeoutDate) + "T" + timeFormat.format(timeoutDate) + "Z";
   		
   		return sDate;
    }
    
	/**
	 * Method listToResumptionToken. Add the elements in the list to a resumption token file.
	 * @param list a list of Element's(!)
	 * @return String The resumption token
	 */
    private String listToResumptionToken(List list, String prefix) {
	    MCRConfiguration config = MCRConfiguration.instance();
	    List tokenElements = new ArrayList(list);
	    String fileName = null;
	    
	    if (tokenElements.size() > 0) {
	    	// a resumption token file has to be written
		    Date tmpDate = new Date();
		   	long fileId = tmpDate.getTime();
		    int docs = tokenElements.size();

			try {
			   	String resumptionTokenDir = config.getString(STR_OAI_RESUMPTIONTOKEN_DIR) + File.separator;
				fileName = fileId + "x1x" + docs;
		
 				FileOutputStream fos = new FileOutputStream(resumptionTokenDir +
   	                fileName + "." + prefix + STR_RESUMPTIONTOKEN_SUFFIX);
			 	ObjectOutputStream oos = new ObjectOutputStream(fos);
				 	
			 	ListIterator tokenElementsIterator = tokenElements.listIterator();
				 	
			 	while (tokenElementsIterator.hasNext()) {
			 		Element element = (Element) tokenElementsIterator.next();
				 		
				    oos.writeObject(element);
			 	}
				
				oos.close();
				fos.close(); 	
			} catch (MCRConfigurationException mcrcx) {
				logger.error("Resumption Token Directory not configured.");
				logger.error("The result list was only partially returned.");
	    	} catch (IOException e) { 
	            logger.error(e.getMessage());
				logger.error("The result list was only partially returned.");
			}					
    	}
    	
    	return fileName;
    }
    
	/**
	 * Method identify. Implementation of the OAI Verb Identify.
	 * @param request The servlet request.
	 * @param header The document so far
	 * @return Document The document with all new elements added.
	 */
    private org.jdom.Document identify(HttpServletRequest request, org.jdom.Document header) {
    	logger.info("Harvester hat 'Identify' angefordert");
        org.jdom.Document document = header;
        
        if (badArguments(request, 1)) {
        	logger.info("Es wurden überflüssige Argumente an die Anfrage übergeben. Nach OAI 2.0 erfolgt hier ein Abbruch.");
            return addError(document, "badArgument", ERR_ILLEGAL_ARGUMENT);
        }

	    MCRConfiguration config = MCRConfiguration.instance();
	    String repositoryIdentifier = new String();
	    String repositoryName = new String();
		try {
	        repositoryIdentifier = config.getString(STR_OAI_REPOSITORY_IDENTIFIER + "." + getServletName());
	        repositoryName = config.getString(STR_OAI_REPOSITORY_NAME + "." + getServletName());
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
        eIdentify.addContent(newElementWithContent("baseURL", ns, request.getRequestURL().toString()));
        eIdentify.addContent(newElementWithContent("protocolVersion", ns, STR_OAI_VERSION));
        String adminEmail = MCRConfiguration.instance().getString(STR_OAI_ADMIN_EMAIL);
        eIdentify.addContent(newElementWithContent("adminEmail", ns, adminEmail));
        eIdentify.addContent(newElementWithContent("earliestDatestamp", ns, STR_FIRST_DATE));
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
    	    StringTokenizer tokenizer = new StringTokenizer(friends, " ");
    	    if (tokenizer.countTokens() > 0) {
		        Element eFriendsDescription = new Element("description", ns);
    	    	Namespace frns = Namespace.getNamespace(STR_OAI_NAMESPACE + "2.0/friends/");
    	    	Element eFriends = new Element("friends", frns);
    	    	eFriends.addNamespaceDeclaration(xsi);
    	    	eFriends.setAttribute("schemaLocation", STR_OAI_NAMESPACE + "2.0/friends/ "  
    	    		+ STR_OAI_NAMESPACE + "2.0/friends.xsd", xsi);
    	    	
    	    	while (tokenizer.hasMoreTokens()) {
    	    		eFriends.addContent(newElementWithContent("baseURL", frns, "http://" + tokenizer.nextToken()));	
    	    	}
    	    	
    	    	eFriendsDescription.addContent(eFriends);
    	    	eIdentify.addContent(eFriendsDescription);
    	    }
		} catch (MCRConfigurationException mcrx) {
			// Nothing to be done here (really, not kidding!)
		}

        eRoot.addContent(eIdentify);
        
        return document;
    }

	/**
	 * Method listMetadataFormats. Implementation of the OAI Verb ListMetadataFormats.
	 * @param request The servlet request.
	 * @param header The document so far
	 * @return Document The document with all new elements added.
	 */
    private org.jdom.Document listMetadataFormats(HttpServletRequest request, org.jdom.Document header) {
    	logger.info("Harvester hat 'listMetadatFormats' angefordert");
        org.jdom.Document document = header;
        
        Element eRoot = document.getRootElement();
        Namespace ns = eRoot.getNamespace();
	    MCRConfiguration config = MCRConfiguration.instance();
		MCROAIQuery query = null;
		        
		try {
			//check if property is set, else Exception is thrown 
	        config.getString(STR_OAI_REPOSITORY_IDENTIFIER + "." + getServletName());
			query = (MCROAIQuery) config.getInstanceOf(STR_OAI_QUERYSERVICE);
		} catch (MCRConfigurationException mcrx) {
			logger.fatal("Missing configuration item: " + STR_OAI_REPOSITORY_IDENTIFIER + "." + getServletName()
				+ " or OAIQuery-Class is missing.");
			return null;
		}
        
        // First; check if there was an identifier in the request
        String identifier[] = getParameter("identifier", request);
        // If an identifier was given, this will be the record to
        // check, if the metadata is supported
        List record = null;
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
	        String id;
    	    try {
        		id = legalOAIIdentifier(identifier[0]);
	        } catch (MCRException mcrx) {
    	        logger.info("Anfrage 'listMetadataFormats' wurde wegen fehlerhaftem Identifier abgebrochen.");
        	    return addError(document, "badArgument", ERR_ILLEGAL_ARGUMENT);
	        }
	        
            Element eRequest = eRoot.getChild("request", ns);
            eRequest.setAttribute("verb", "ListMetadataFormats");
            eRequest.setAttribute("identifier", identifier[0]);
            if (!query.exists(id)) {
            	logger.info("Anfrage 'listMetadataFormats' wurde wegen falscher ID abgebrochen.");
                return addError(document, "idDoesNotExist", ERR_UNKNOWN_ID);
            } else {
            	record = query.getRecord(id, getServletName());
            }
        }
        
        // The supported metadata formats are only returned if no identifier was given, or
        // the identifier was found in the repository
        Element eListMetadataFormats = new Element("ListMetadataFormats", ns);
        Element dcMetadataFormat = new Element("metadataFormat", ns);
        dcMetadataFormat.addContent(newElementWithContent("metadataPrefix", ns, "oai_dc"));
        dcMetadataFormat.addContent(newElementWithContent("schema", ns, STR_DC_SCHEMA));
        dcMetadataFormat.addContent(newElementWithContent("metadataNamespace", ns, STR_DC_NAMESPACE));
        eListMetadataFormats.addContent(dcMetadataFormat);
        
        Properties properties = config.getProperties(STR_OAI_METADATA_NAMESPACE);
        Enumeration propertiesNames = properties.propertyNames();
        while (propertiesNames.hasMoreElements()) {
            String name = (String) propertiesNames.nextElement();
            String metadataPrefix = name.substring(name.lastIndexOf(".") + 1);
            if (record != null) {
            	// Identifier submitted
            	Element metadata = (Element) record.get(1);
            	try {
        			String namespace = config.getString(STR_OAI_METADATA_NAMESPACE + "." + metadataPrefix);
        			String elementName = config.getString(STR_OAI_METADATA_ELEMENT + "." + metadataPrefix);
        			Namespace mns = Namespace.getNamespace(metadataPrefix, namespace);
        			if (metadata.getChild(elementName, mns) == null) {
        				continue;
        			}
            	} catch(MCRConfigurationException e) {
            		continue;
            	}
            }
            Element eMetadataFormat = new Element("metadataFormat", ns);
            eMetadataFormat.addContent(newElementWithContent("metadataPrefix", ns, metadataPrefix));
            eMetadataFormat.addContent(newElementWithContent("schema", ns, config
            		.getString(STR_OAI_METADATA_SCHEMA + "." + metadataPrefix)));
            eMetadataFormat.addContent(newElementWithContent("metadataNamespace", ns, config
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
	 * @return Document The document with all new elements added.
	 */
    private org.jdom.Document listSets(HttpServletRequest request, org.jdom.Document header) {
        org.jdom.Document document = header;
        Element eRoot = document.getRootElement();
        Namespace ns = eRoot.getNamespace();
        
        deleteOutdatedTokenFiles();
        
        int maxArguments = 1; // the parameter verb is allways there
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
        
	    MCRConfiguration config = MCRConfiguration.instance();
	    
		Element eListSets = new Element("ListSets", ns);
        
        if (resumptionToken != null) {
		    try {
				eListSets = listFromResumptionToken(eListSets, resumptionToken[0], "null");
				if (eListSets == null) {
        			logger.info("Anfrage 'listSets' enthält fehlerhaften Resumption Token " + resumptionToken[0] + ".");
		            return addError(document, "badArgument", ERR_ILLEGAL_ARGUMENT);
				}
		        eRoot.addContent(eListSets);
		    } catch (IOException e) { 	
	            logger.error(e.getMessage());
                return addError(document, "badResumptionToken", ERR_BAD_RESUMPTION_TOKEN);
            }
            
            return document;
        }
        
	    List sets = null;
	    
	    try {
		    MCROAIQuery query = (MCROAIQuery) config.getInstanceOf(STR_OAI_QUERYSERVICE);
		    sets = new ArrayList(query.listSets(getServletName()));
	    } catch (MCRConfigurationException mcrx) {
	    	logger.fatal(mcrx.getMessage());
            return addError(document, "badResumptionToken", mcrx.getMessage());
	    }
	    
	    if (sets != null) {
	    	int maxreturns = 0;
	    	ListIterator iterator = sets.listIterator();
	    	
			try {
		        maxreturns = config.getInt(STR_OAI_MAXRETURNS);
			} catch (NumberFormatException nfx) {
				//do nothing, just let maxreturns be 0
			}
	    	
	    	int elementCounter = 0;
	    	
	    	List tokenElements = new ArrayList();
	    	List specs = new ArrayList();
	    	
	    	while (iterator.hasNext()) {
	    		String[] set = (String[]) iterator.next();
	    		
	            Element eSet = new Element("set", ns);
	            eSet = setSpec(eSet, set[0], ns);
	            String content = eSet.getChildText("setSpec", ns);
	            if (specs.contains(content)) {
	            	continue;
	            }
	            specs.add(content);
	    		elementCounter++;
    	        if (set[1].length() > 0) {
    	        	eSet.addContent(newElementWithContent("setName", ns, set[1]));
    	        } else {
    	        	eSet.addContent(newElementWithContent("setName", ns, set[0]));
    	        }
	            if ((set[2] != null) && (set[2].length() > 0)) {
	                Namespace oaidc = Namespace.getNamespace("oai_dc", STR_DC_NAMESPACE);
    	            Element eDC = new Element("dc", oaidc);
        	        Namespace dc = Namespace.getNamespace("dc", "http://purl.org/dc/elements/1.1/");
            	    Namespace xsi = Namespace.getNamespace("xsi", STR_SCHEMA_INSTANCE);
    	            eDC.addNamespaceDeclaration(dc);
	                eDC.addNamespaceDeclaration(xsi);
        	        eDC.setAttribute("schemaLocation", STR_OAI_NAMESPACE 
        	        		+ STR_OAI_VERSION + "/oai_dc/ "  
        	        		+ STR_OAI_NAMESPACE + STR_OAI_VERSION 
        	        		+ "/oai_dc.xsd", xsi);
	                Element eDescription = new Element("description", dc);
                	eDescription.addContent(set[2]);
            	    eDC.addContent(eDescription);
            	    Element eSetDescription = new Element("setDescription", ns);
            	    eSetDescription.addContent(eDC);
        	        eSet.addContent(eSetDescription);
    	        }
            
            	if ((maxreturns == 0) || (elementCounter <= maxreturns)) {
		            eListSets.addContent(eSet);
            	} else {
            		tokenElements.add(eSet);
            	}
	    	}
	    	
	    	String sResumptionToken = listToResumptionToken(tokenElements, "null");
            if (sResumptionToken != null) {
				Element eResumptionToken = new Element("resumptionToken", ns);
				eResumptionToken.setAttribute("completeListSize", Integer.toString(maxreturns + tokenElements.size()));
				eResumptionToken.setAttribute("cursor", "0");
				
			    int timeout = config.getInt(STR_OAI_RESUMPTIONTOKEN_TIMEOUT, 72);
           		String sDate = getUTCDate(timeout);
           		eResumptionToken.addContent(sResumptionToken);
				eResumptionToken.setAttribute("expirationDate", sDate);
	           	eListSets.addContent(eResumptionToken);
			}
	    	
	    	eRoot.addContent(eListSets);
	    }

        return document;
    }

	/**
	 * Method listIdentifiers. Implementation of the OAI Verb ListIdentifiers.
	 * @param request The servlet request.
	 * @param header The document so far
	 * @return Document The document with all new elements added.
	 */
    private org.jdom.Document listIdentifiers(HttpServletRequest request, org.jdom.Document header) {
        org.jdom.Document document = header;
        Element eRoot = document.getRootElement();
        Namespace ns = eRoot.getNamespace();
        
        deleteOutdatedTokenFiles();
        
        int maxArguments = 2; //verb and metadataPrefix are required!
        
        // get the arguments from the request
        String from[] = getParameter("from", request);
        String until[] = getParameter("until", request); 
        String set[] = getParameter("set", request); 
        String metadataPrefix[] = getParameter("metadataPrefix", request); 
        String resumptionToken[] = getParameter("resumptionToken", request); 
        if (resumptionToken != null) {
            maxArguments++;
            if ((from != null) || (until != null) || (set != null) || (metadataPrefix != null)) {
    	    	logger.info("Anfrage 'listRecords' enthält fehlerhafte Parameter.");
	            return addError(document, "badArgument", ERR_ILLEGAL_ARGUMENT);
            }
        } else {
	        Date fromDate = null;
    	    if (from != null) {
        		fromDate = getDate(from[0]);
        		if (fromDate == null) {
    	    		logger.info("Anfrage 'listIdentifiers' enthält fehlerhafte Parameter.");
		            return addError(document, "badArgument", ERR_ILLEGAL_ARGUMENT);
    	    	}
        	    maxArguments++;
	        } else {
    	    	fromDate = getDate(STR_FIRST_DATE);
        	}
	        Date untilDate = null;
    	    if (until != null) {
        		untilDate = getDate(until[0]);
        		if (untilDate == null) {
    	    		logger.info("Anfrage 'listIdentifiers' enthält fehlerhafte Parameter.");
		            return addError(document, "badArgument", ERR_ILLEGAL_ARGUMENT);
    	    	}
        		if (fromDate != null) {
        			if (fromDate.after(untilDate)) {
    		    		logger.info("Anfrage 'listIdentifiers' enthält fehlerhafte Parameter.");
		           		return addError(document, "noRecordsMatch", ERR_NO_RECORDS_MATCH);
	        		}
    	    	}
        	    maxArguments++;
	        }
    	    if (set != null) {
        	    maxArguments++;
	        }
        	if (metadataPrefix == null) {
    	    	logger.info("Anfrage 'listRecords' enthält fehlerhafte Parameter.");
	            return addError(document, "badArgument", ERR_ILLEGAL_ARGUMENT);
        	}
        }
        //The number of arguments must not exceed maxArguments
        if (badArguments(request, maxArguments)) {
        	logger.info("Anfrage 'listIdentifiers' wegen falschen Parametern abgebrochen.");
            return addError(document, "badArgument", ERR_ILLEGAL_ARGUMENT);
        }
        
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
        String prefix = null;
        if (metadataPrefix != null) {
        	prefix = metadataPrefix[0];
	        eRequest.setAttribute("metadataPrefix", prefix);
        } else {
        	prefix = getPrefix(resumptionToken[0]);
        	if (prefix == null) {
        		logger.info("Error in resumption token.");
	            return addError(document, "badResumptionToken", ERR_BAD_RESUMPTION_TOKEN);
        	}
        }
        
        MCRConfiguration config = MCRConfiguration.instance();
        //Check, if the requested metadata format is supported
	    try {
			//check if property is set, else Exception is thrown 
	        config.getString(STR_OAI_METADATA_TRANSFORMER + "." + prefix);
	    } catch (MCRConfigurationException mcrx) {
        	logger.info("Anfrage 'listIdentifiers' wegen unbekanntem Metadatenformat " + prefix + " abgebrochen.");
            return addError(document, "cannotDisseminateFormat", ERR_UNKNOWN_FORMAT);
	    }
        
        Element eListIdentifiers = new Element("ListIdentifiers", ns);
        
        if (resumptionToken != null) {
		    try {
				eListIdentifiers = listFromResumptionToken(eListIdentifiers, resumptionToken[0], prefix);
				if (eListIdentifiers == null) {
        			logger.info("Anfrage 'listIdentifiers' enthält fehlerhaften Resumption Token " + resumptionToken[0] + ".");
		            return addError(document, "badArgument", ERR_ILLEGAL_ARGUMENT);
				}
		        eRoot.addContent(eListIdentifiers);
		    } catch (IOException e) { 	
	            logger.error(e.getMessage());
                return addError(document, "badResumptionToken", ERR_BAD_RESUMPTION_TOKEN);
            }
            
            return document;
        }
        
        List mappedSets = new ArrayList();
	    if (set != null) {
	    	buildMappings();
	    	logger.info("Set: " + set[0]);
	    	if (mappings.containsValue(set[0])) {
	    		Set keys = mappings.keySet();
	    		Iterator keyIterator = keys.iterator();
	    		while (keyIterator.hasNext()) {
	    			String key = (String) keyIterator.next();
	    			String value = (String) mappings.get(key);
	    			if (value.equals(set[0])) {
	    				String[] mappedSet = new String[1];
	    				mappedSet[0] = key;
	    				mappedSets.add(mappedSet);
	    			}
	    		}
	    	}
	    } else {
	    	mappedSets.add(set);
	    }
	    
	    List sets = new ArrayList();
	    try {
		    MCROAIQuery query = (MCROAIQuery) config.getInstanceOf(STR_OAI_QUERYSERVICE);
		    Iterator mappedSetIterator = mappedSets.iterator();
		    while (mappedSetIterator.hasNext()) {
		    	String[] mappedSet = (String[]) mappedSetIterator.next();
		    	List result = query.listIdentifiers(mappedSet, from, until, getServletName());
		    	if (result != null) {
		    		sets.addAll(result);
		    	}
		    }
	    } catch (MCRConfigurationException mcrx) {
	    	logger.fatal(mcrx.getMessage());
           	return addError(document, "noRecordsMatch", ERR_NO_RECORDS_MATCH);
	    }

	    if (sets.size() > 0) {
	    	int maxreturns = 0;
	    	ListIterator iterator = sets.listIterator();
	    	
			try {
		        maxreturns = config.getInt(STR_OAI_MAXRETURNS);
			} catch (NumberFormatException nfx) {
				//do nothing, just let maxreturns be 0
			}
	    	
	    	int elementCounter = 0;
	    	
	    	List tokenElements = new ArrayList();
	    	
	    	while (iterator.hasNext()) {
	    		elementCounter++;
	    		String[] array = (String[]) iterator.next();
	    		
		        Element eHeader = new Element("header", ns);
    	        eHeader.addContent(newElementWithContent("identifier", ns, array[0]));
            	eHeader.addContent(newElementWithContent("datestamp", ns, array[1]));
            	eHeader = setSpec(eHeader, array[2], ns);
            
            	if ((maxreturns == 0) || (elementCounter <= maxreturns)) {
		            eListIdentifiers.addContent(eHeader);
            	} else {
            		tokenElements.add(eHeader);
            	}
	    	}
	    	
	    	String sResumptionToken = listToResumptionToken(tokenElements, prefix);
            if (sResumptionToken != null) {
				Element eResumptionToken = new Element("resumptionToken", ns);
				eResumptionToken.setAttribute("completeListSize", Integer.toString(maxreturns + tokenElements.size()));
				eResumptionToken.setAttribute("cursor", "0");
				
			    int timeout = config.getInt(STR_OAI_RESUMPTIONTOKEN_TIMEOUT, 72);
           		String sDate = getUTCDate(timeout);
           		eResumptionToken.addContent(sResumptionToken);
				eResumptionToken.setAttribute("expirationDate", sDate);
	           	eListIdentifiers.addContent(eResumptionToken);
			}
				
	    	eRoot.addContent(eListIdentifiers);
	    } else {
           	return addError(document, "noRecordsMatch", ERR_NO_RECORDS_MATCH);
	    }
	    
        return document;
    }
    
	/**
	 * Method getRecord. Implementation of the OAI Verb GetRecord.
	 * @param request The servlet request.
	 * @param header The document so far
	 * @return Document The document with all new elements added.
	 */
    private org.jdom.Document getRecord(HttpServletRequest request, org.jdom.Document header) {
        org.jdom.Document document = header;
        Element eRoot = document.getRootElement();
        Namespace ns = eRoot.getNamespace();

        // First: check if there was an identifier in the request (required)
        String identifier[] = getParameter("identifier", request); 
        // Then: check if there was an metadataPrefix in the request (required)
        String metadataPrefix[] = getParameter("metadataPrefix", request); 
        if ((identifier == null) || (metadataPrefix == null) || 
                (identifier.length != 1) || (metadataPrefix.length != 1) || 
                badArguments(request, 3)) {
            logger.info("Anfrage 'getRecord' wurde wegen fehlendem Parameter abgebrochen.");
            return addError(document, "badArgument", ERR_ILLEGAL_ARGUMENT);
        }
        
        String id;
        try {
        	id = legalOAIIdentifier(identifier[0]);
        } catch (MCRException mcrx) {
            logger.info("Anfrage 'getRecord' wurde wegen fehlerhaftem Identifier abgebrochen.");
            return addError(document, "badArgument", ERR_ILLEGAL_ARGUMENT);
        }
        
        Element eRequest = eRoot.getChild("request", ns);
        eRequest.setAttribute("verb", "GetRecord");
        eRequest.setAttribute("identifier", identifier[0]);
        eRequest.setAttribute("metadataPrefix", metadataPrefix[0]);
        
        MCRConfiguration config = MCRConfiguration.instance();
        //Check, if the requested metadata format is supported
        String format = null;
        //Check, if the requested metadata format is supported
	    try {
	        format = config.getString(STR_OAI_METADATA_TRANSFORMER + "." + metadataPrefix[0]);
	        logger.info("Transformer: " + format);
	    } catch (MCRConfigurationException mcrx) {
        	logger.info("Anfrage 'getRecord' wurde wegen fehlendem Metadatenformat " + metadataPrefix[0] + " abgebrochen.");
            return addError(document, "cannotDisseminateFormat", ERR_UNKNOWN_FORMAT);
	    }
        
        //Then look, if the id exists
        MCROAIQuery query = null;
	    try {
		    query = (MCROAIQuery) config.getInstanceOf(STR_OAI_QUERYSERVICE);
	    } catch (MCRConfigurationException mcrx) {
	    	logger.fatal(mcrx.getMessage());
            return addError(document, "idDoesNotExist", ERR_UNKNOWN_ID);
	    }

        if (query.exists(id)) {
        	List record = new ArrayList(query.getRecord(id, getServletName()));
            Element eGetRecord = new Element("GetRecord", ns);
            
		    if (record != null) {
		    	ListIterator iterator = record.listIterator();
	    	
	    		String[] array = (String[]) iterator.next();
	    		
		        Element eHeader = new Element("header", ns);
    	        eHeader.addContent(newElementWithContent("identifier", ns, array[0]));
            	eHeader.addContent(newElementWithContent("datestamp", ns, array[1]));
            	eHeader = setSpec(eHeader, array[2], ns);
            
		        Element eRecord = new Element("record", ns);
		        eRecord.addContent(eHeader);
	            
	            Element eMetadata = (Element) iterator.next();
	            eRecord.addContent(eMetadata);
	            
	            eGetRecord.addContent(eRecord);
		    	eRoot.addContent(eGetRecord);

            	org.jdom.Document newDocument = MCRXSLTransformation.transform(document, 
            		getServletContext().getRealPath("/WEB-INF/stylesheets/" + format));
        	    if (newDocument != null) {
    	            document = newDocument;
	            } else {
	            	logger.error("Die Transformation in 'getRecord' hat nicht funktioniert.");
        	    }
	    	}
	    	
		} else {
			logger.info("Anfrage 'getRecord wurde fegen fehlender ID " + id + "abgebrochen.");
            return addError(document, "idDoesNotExist", ERR_UNKNOWN_ID);
        }
        
        return document;
    }
    
	/**
	 * Method listRecords. Implementation of the OAI Verb ListRecords.
	 * @param request The servlet request.
	 * @param header The document so far
	 * @return Document The document with all new elements added.
	 */
    private org.jdom.Document listRecords(HttpServletRequest request, org.jdom.Document header) {
        org.jdom.Document document = header;
        Element eRoot = document.getRootElement();
        Namespace ns = eRoot.getNamespace();
        
        deleteOutdatedTokenFiles();
        
        int maxArguments = 2; //verb and metadataPrefix are required!
        
        // Second: get the arguments from the request
        String from[] = getParameter("from", request);
        String until[] = getParameter("until", request); 
        String set[] = getParameter("set", request); 
        String metadataPrefix[] = getParameter("metadataPrefix", request); 
        String resumptionToken[] = getParameter("resumptionToken", request); 
        if (resumptionToken != null) {
            maxArguments++;
            if ((from != null) || (until != null) || (set != null) || (metadataPrefix != null)) {
    	    	logger.info("Anfrage 'listRecords' enthält fehlerhafte Parameter.");
	            return addError(document, "badArgument", ERR_ILLEGAL_ARGUMENT);
            }
        } else {
	        Date fromDate = null;
    	    if (from != null) {
        		fromDate = getDate(from[0]);
        		if (fromDate == null) {
    	    		logger.info("Anfrage 'listRecords' enthält fehlerhafte Parameter.");
		            return addError(document, "badArgument", ERR_ILLEGAL_ARGUMENT);
    	    	}
        	    maxArguments++;
	        } else {
    	    	fromDate = getDate(STR_FIRST_DATE);
        	}
	        Date untilDate = null;
    	    if (until != null) {
        		untilDate = getDate(until[0]);
        		if (untilDate == null) {
    	    		logger.info("Anfrage 'listRecords' enthält fehlerhafte Parameter.");
		            return addError(document, "badArgument", ERR_ILLEGAL_ARGUMENT);
    	    	}
        		if (fromDate != null) {
        			if (fromDate.after(untilDate)) {
    		    		logger.info("Anfrage 'listRecords' enthält fehlerhafte Parameter.");
		           		return addError(document, "noRecordsMatch", ERR_NO_RECORDS_MATCH);
	        		}
    	    	}
        	    maxArguments++;
	        }
    	    if (set != null) {
        	    maxArguments++;
        	}
        	if (metadataPrefix == null) {
    	    	logger.info("Anfrage 'listRecords' enthält fehlerhafte Parameter.");
	            return addError(document, "badArgument", ERR_ILLEGAL_ARGUMENT);
        	}
        }
        //The number of arguments must not exceed maxArguments
        if (badArguments(request, maxArguments)) {
        	logger.info("Anfrage 'listRecords' enthält fehlerhafte Parameter.");
            return addError(document, "badArgument", ERR_ILLEGAL_ARGUMENT);
        }
        
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
        String prefix = null;
        if (metadataPrefix != null) {
        	prefix = metadataPrefix[0];
	        eRequest.setAttribute("metadataPrefix", prefix);
        } else {
        	prefix = getPrefix(resumptionToken[0]);
        	if (prefix == null) {
        		logger.info("Error in resumption token.");
	            return addError(document, "badResumptionToken", ERR_BAD_RESUMPTION_TOKEN);
        	}
        }
        
        MCRConfiguration config = MCRConfiguration.instance();
        String format = null;
        //Check, if the requested metadata format is supported
	    try {
	        format = config.getString(STR_OAI_METADATA_TRANSFORMER + "." + prefix);
	    } catch (MCRConfigurationException mcrx) {
        	logger.info("Anfrage 'listRecords' wurde wegen fehlendem Metadatenformat " + prefix + " abgebrochen.");
            return addError(document, "cannotDisseminateFormat", ERR_UNKNOWN_FORMAT);
	    }
	    
        Element eListRecords = new Element("ListRecords", ns);
        
        if (resumptionToken != null) {
		    try {
				eListRecords = listFromResumptionToken(eListRecords, resumptionToken[0], prefix);
				if (eListRecords == null) {
        			logger.info("Anfrage 'listRecords' enthält fehlerhaften Resumption Token " + resumptionToken[0] + ".");
		            return addError(document, "badArgument", ERR_ILLEGAL_ARGUMENT);
				}
		        eRoot.addContent(eListRecords);
		    } catch (IOException e) { 	
	            logger.error(e.getMessage());
                return addError(document, "badResumptionToken", ERR_BAD_RESUMPTION_TOKEN);
            }
            
            org.jdom.Document newDocument = MCRXSLTransformation.transform(document, 
            	getServletContext().getRealPath("/WEB-INF/stylesheets/" + format));
        	if (newDocument != null) {
    	        document = newDocument;
	        } else {
	           	logger.error("Die Transformation in 'listRecords' hat nicht funktioniert.");
        	}
        	
            return document;
        }

        List mappedSets = new ArrayList();
	    if (set != null) {
	    	buildMappings();
	    	if (mappings.containsValue(set[0])) {
	    		Set keys = mappings.keySet();
	    		Iterator keyIterator = keys.iterator();
	    		while (keyIterator.hasNext()) {
	    			String key = (String) keyIterator.next();
	    			String value = (String) mappings.get(key);
	    			if (value.equals(set[0])) {
	    				String[] mappedSet = new String[1];
	    				mappedSet[0] = key;
	    				mappedSets.add(mappedSet);
	    			}
	    		}
	    	}
	    } else {
	    	mappedSets.add(set);
	    }
	    
	    List sets = new ArrayList();
	    try {
		    MCROAIQuery query = (MCROAIQuery) config.getInstanceOf(STR_OAI_QUERYSERVICE);
		    Iterator mappedSetIterator = mappedSets.iterator();
		    while (mappedSetIterator.hasNext()) {
		    	String[] mappedSet = (String[]) mappedSetIterator.next();
		    	List result = query.listRecords(mappedSet, from, until, getServletName());
		    	if (result != null) {
		    		sets.addAll(result);
		    	}
		    }
	    } catch (MCRConfigurationException mcrx) {
	    	logger.fatal(mcrx.getMessage());
           	return addError(document, "noRecordsMatch", ERR_NO_RECORDS_MATCH);
	    }

	    if (sets.size() > 0) {
	    	int maxreturns = 0;
	    	ListIterator iterator = sets.listIterator();
	    	
			try {
		        maxreturns = config.getInt(STR_OAI_MAXRETURNS);
			} catch (NumberFormatException nfx) {
				//do nothing, just let maxreturns be 0
			}
	    	
	    	int elementCounter = 0;
	    	
	    	List tokenElements = new ArrayList();
	    	
	    	while (iterator.hasNext()) {
	    		elementCounter++;
	    		String[] array = (String[]) iterator.next();
	    		
		        Element eHeader = new Element("header", ns);
    	        eHeader.addContent(newElementWithContent("identifier", ns, array[0]));
            	eHeader.addContent(newElementWithContent("datestamp", ns, array[1]));
            	eHeader = setSpec(eHeader, array[2], ns);

            	Element eRecord = new Element("record", ns);
		        eRecord.addContent(eHeader);
	            
	            Element eMetadata = (Element) iterator.next();
	            eRecord.addContent(eMetadata);
	            
            	if ((maxreturns == 0) || (elementCounter <= maxreturns)) {
		            eListRecords.addContent(eRecord);
            	} else {
            		tokenElements.add(eRecord);
            	}
            	
	    	}
	    	
	    	String sResumptionToken = listToResumptionToken(tokenElements, prefix);
            if (sResumptionToken != null) {
				Element eResumptionToken = new Element("resumptionToken", ns);
				eResumptionToken.setAttribute("completeListSize", Integer.toString(maxreturns + tokenElements.size()));
				eResumptionToken.setAttribute("cursor", "0");
				
			    int timeout = config.getInt(STR_OAI_RESUMPTIONTOKEN_TIMEOUT, 72);
           		String sDate = getUTCDate(timeout);
           		eResumptionToken.addContent(sResumptionToken);
				eResumptionToken.setAttribute("expirationDate", sDate);
	           	eListRecords.addContent(eResumptionToken);
			}
				
	    	eRoot.addContent(eListRecords);
		    	
            org.jdom.Document newDocument = MCRXSLTransformation.transform(document, 
            	getServletContext().getRealPath("/WEB-INF/stylesheets/" + format));
        	if (newDocument != null) {
    	        document = newDocument;
	        } else {
	           	logger.error("Die Transformation in 'listRecords' hat nicht funktioniert.");
        	}
	    } else {
           	return addError(document, "noRecordsMatch", ERR_NO_RECORDS_MATCH);
	    }
	    
        return document;
    }

	/**
	 * Method legalOAIIdentifier. Extract the identifier from a legal OAI identifier or throw a MCRException.
	 * @param identifier a legal MCRException
	 * @return String the MyCoRe-/Miless-Identifier
	 * @throws MCRException
	 */
	private String legalOAIIdentifier(String identifier) throws MCRException {
		StringTokenizer tokenizer = new StringTokenizer(identifier, ":");
		
		if (tokenizer.countTokens() < 3) {
			throw new MCRException("Error in legalOAIIdentifier");
		}
		String scheme = tokenizer.nextToken();
		String repositoryIdentifierFromIdentifier = tokenizer.nextToken();
	    MCRConfiguration config = MCRConfiguration.instance();
	    String repositoryIdentifier = new String();
		try {
	        repositoryIdentifier = config.getString(STR_OAI_REPOSITORY_IDENTIFIER + "." + getServletName());
		} catch (MCRConfigurationException mcrx) {
			logger.fatal("Missing configuration item: either " + STR_OAI_REPOSITORY_IDENTIFIER + "." + getServletName()
				+ " is missing.");
			throw new MCRException("Error in legalOAIIdentifier");
		}
		if (!"oai".equals(scheme) || !repositoryIdentifierFromIdentifier.equals(repositoryIdentifier)) {
			throw new MCRException("Error in legalOAIIdentifier");
		}
		
		String allowed = new String("1234567890abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ;/?:@&=+$,-_.!~*´()");
		StringBuffer buffer = new StringBuffer(tokenizer.nextToken());
		while (tokenizer.hasMoreTokens()) {
			buffer.append(":")
				  .append(tokenizer.nextToken());
		}
		
		for (int i = 0; i < buffer.length(); i++) {
			char c = buffer.charAt(i);
			if (allowed.indexOf(c) == -1) {
				throw new MCRException("Error in legalOAIIdentifier");
			}
		}
		
		String id = buffer.toString();
		return id;
	}
	
	/**
	 * @param element the Header element
	 * @param spec the set specs
	 * @param ns the Namespace of <code>element</code>
	 * @return the new header element
	 */
	private Element setSpec(Element element, String setSpec, Namespace ns) {
		buildMappings();
        if ((setSpec != null) && (setSpec.length() > 0)) {
    	    StringTokenizer tokenizer = new StringTokenizer(setSpec, " ");
    	
	    	while (tokenizer.hasMoreTokens()) {
	    		String spec = tokenizer.nextToken();
	    		if ((mappings != null) && mappings.containsKey(spec)) {
	    			spec = (String) mappings.get(spec);
	    		}
	    		element.addContent(newElementWithContent("setSpec", ns, spec));
	    	}
    	
        }
		return element;
	}
	
	private void buildMappings() {
		if (mappings == null) {
	        MCRConfiguration config = 
	        	MCRConfiguration.instance();
			Properties mappingProperties = 
				config.getProperties(STR_OAI_CATEGORY_MAPPING);
			if (mappingProperties.size() > 0) {
				mappings = new HashMap();
				Enumeration enumeration = mappingProperties.keys();
				while (enumeration.hasMoreElements()) {
					String propertyKey = (String) enumeration.nextElement();
					String value = mappingProperties.getProperty(propertyKey);
					StringTokenizer tokenizer = new StringTokenizer(value, " ");
					String shortKey = ((String) propertyKey).
							substring(
									STR_OAI_CATEGORY_MAPPING.
									length() + 1).replace('.', ':');
			    	while (tokenizer.hasMoreTokens()) {
			    		String token = tokenizer.nextToken();
			    		mappings.put(token, shortKey);
			    	}
				}
			}
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
}
