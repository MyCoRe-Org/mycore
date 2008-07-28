/**
 * $RCSfile$ $Revision$ $Date$ This file is part of ** M y C o R e ** Visit our homepage at http://www.mycore.de/ for details. This program is free software;
 * you can use it, redistribute it and / or modify it under the terms of the GNU General Public License (GPL) as published by the Free Software Foundation;
 * either version 2 of the License or (at your option) any later version. This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You
 * should have received a copy of the GNU General Public License along with this program, normally in the file license.txt. If not, write to the Free Software
 * Foundation Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307 USA
 **/

package org.mycore.services.oai;

import java.io.File;
import java.io.IOException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
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
import java.util.StringTokenizer;
import java.util.TimeZone;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.ProcessingInstruction;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.jdom.transform.JDOMSource;

import org.mycore.common.MCRConfigurationException;
import org.mycore.common.MCRException;
import org.mycore.common.xml.MCRXMLResource;
import org.mycore.common.xml.MCRXSLTransformation;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.classifications2.MCRCategoryDAOFactory;
import org.mycore.datamodel.classifications2.MCRLabel;
import org.mycore.datamodel.metadata.MCRMetaClassification;
import org.mycore.datamodel.metadata.MCRMetaElement;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;

/**
 * This class implements an OAI Data Provider for MyCoRe and Miless
 * 
 * @author Werner Gre�hoff
 * @author Heiko Helmbrecht
 * @version $Revision$ $Date$
 */
public class MCROAIProvider extends MCRServlet {
    /**
     * <code>serialVersionUID</code> introduced for compatibility with JDK 1.4 (a should have)
     */
    private static final long serialVersionUID = 4121136939476267829L;

    private static long lastTokenID = 0;

    private static Logger logger = Logger.getLogger(MCROAIProvider.class);

    private static HashMap oaiConfig;

    // e-mail of oai-administrator
    private static String oaiAdminEmail;

    // a colon (:) separated list of other oai repositories (friends)
    private static String oaiProviderFriends;

    // after this time a resumption token will be deleted
    private static int oaiResumptionTokenTimeOut;

    // maximum number of returned hits
    private static int maxReturns;

    private static MCROAIResumptionTokenStore resStore;

    static {
        Properties props = CONFIG.getProperties("MCR.OAI.Repository.Identifier.");
        oaiConfig = new HashMap();
        for (Enumeration en = props.propertyNames(); en.hasMoreElements();) {
            String propName = (String) en.nextElement();
            String instance = propName.substring("MCR.OAI.Repository.Identifier.".length());
            oaiConfig.put(instance, new MCROAIConfigBean(instance));
        }
        oaiAdminEmail = CONFIG.getString("MCR.OAI.AdmineMail");
        oaiProviderFriends = CONFIG.getString("MCR.OAI.Friends", "");
        oaiResumptionTokenTimeOut = CONFIG.getInt("MCR.OAI.Resumptiontoken.Timeout", 72);
        maxReturns = CONFIG.getInt("MCR.OAI.MaxReturns", 10);
        resStore = (MCROAIResumptionTokenStore) CONFIG
                .getInstanceOf("MCR.OAI.Resumptiontoken.Store", "org.mycore.backend.hibernate.MCRHIBResumptionTokenStore");
    }

    // property name for the implementing class of MCROAIQuery
    private static final String STR_OAI_QUERYSERVICE = "MCR.OAI.QueryService";

    private static final String STR_OAI_METADATA_TRANSFORMER = "MCR.OAI.Metadata.Transformer";

    // If there are other metadata formats than the standard format oai_dc
    // available,
    // all need a namespace and schema entry
    // of it's own, e.g.
    // MCR.OAI.Metadata.Namespace.olac=http://www.language-archives.org/OLAC/0.2/
    // MCR.OAI.Metadata.Schema.olac=http://www.language-archives.org/OLAC/olac-0.2.xsd
    private static final String STR_OAI_METADATA_NAMESPACE = "MCR.OAI.Metadata.Namespace";

    private static final String STR_OAI_METADATA_ELEMENT = "MCR.OAI.Metadata.Element";

    private static final String STR_OAI_METADATA_SCHEMA = "MCR.OAI.Metadata.Schema";

    // Following the DINI recommendation for OAI repositories
    // (http://www.dini.de/documents/OAI-Empfehlungen-Okt2003-de.pdf) there
    // there should be some classifications/categories defined which are very
    // restrictive in usage.
    // OLD: So this client provides a possibility to map
    // from a far more detailed classification system to the simplistic
    // DINI specification.
    // CHANGED: You should add new LABELS in your classification for each
    // category
    // mark them with the attribute @xml:lang="x-dini",
    // and set their @text-attribute to the DINI-spec ID,
    // which this item should be mapped to -
    // e.g. <category ID="TYPE0001.001">
    // <label xml:lang="de" text="Monographie" />
    // <label xml:lang="en" text="Monographie" />
    // <label xml:lang="x-dini" text="pub-type:monograph" />
    // </category>
    //
    // Different classification items can map to the same DINI-Spec-ID.

    // Some constants referring to metadata formats (must be known for dc)
    private static final String STR_DC_NAMESPACE = "http://www.openarchives.org/OAI/2.0/oai_dc/";

    private static final String STR_DC_SCHEMA = "http://www.openarchives.org/OAI/2.0/oai_dc.xsd";

    private static final String STR_OAI_NAMESPACE = "http://www.openarchives.org/OAI/";

    private static final String STR_OAI_VERSION = "2.0";

    private static final String STR_SCHEMA_INSTANCE = "http://www.w3.org/2001/XMLSchema-instance";

    private static final String STR_GRANULARITY = "yyyy-MM-dd'T'HH:mm:ss'Z'";

    private static final String STR_GRANULARITY_SHORT = "yyyy-MM-dd";

    private static final String STR_FIRST_DATE = "2000-01-01";

    private static final String ERR_FAULTY_VERB = "No verb or too much verbs";

    private static final String ERR_ILLEGAL_VERB = "Illegal verb";

    private static final String ERR_ILLEGAL_ARGUMENT = "Illegal argument";

    private static final String ERR_UNKNOWN_ID = "Unknown identifier";

    private static final String ERR_BAD_RESUMPTION_TOKEN = "Bad resumption token";

    private static final String ERR_NO_RECORDS_MATCH = "No results where found with your search criteria";

    private static final String ERR_UNKNOWN_FORMAT = "Unknown metadata format";

    private static final String[] STR_VERBS = { "GetRecord", "Identify", "ListIdentifiers", "ListMetadataFormats", "ListRecords", "ListSets" };

    /**
     * Method destroy. Automatically destroys the Servlet.
     */
    public void destroy() {
    }

    /**
     * The method replace the default from MCRServlet
     * 
     * @param MCRServlet
     *            job
     * @throws JDOMException 
     * @throws ServletException
     * @throws IOException
     */
    protected void doGetPost(MCRServletJob job) throws JDOMException {
        HttpServletRequest request = job.getRequest();
        HttpServletResponse response = job.getResponse();
        processRequest(request, response);
    }

    /**
     * Method processRequest. Processes requests for both HTTP <code>GET</code> and <code>POST</code> methods.
     * 
     * @param request
     *            servlet request
     * @param response
     *            servlet response
     * @throws JDOMException 
     * @throws ServletException
     * @throws IOException
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws JDOMException {
        response.setContentType("text/xml; charset=UTF-8");

        // Exceptions must be caught...
        try {
            ServletOutputStream out = response.getOutputStream();
            XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());

            Namespace ns = Namespace.getNamespace(STR_OAI_NAMESPACE + "2.0/");
            Element eRoot = new Element("OAI-PMH", ns);
            org.jdom.Document header = new org.jdom.Document(eRoot);
            Namespace xsi = Namespace.getNamespace("xsi", STR_SCHEMA_INSTANCE);

            eRoot.addNamespaceDeclaration(xsi);
            eRoot.setAttribute("schemaLocation", STR_OAI_NAMESPACE + "2.0/ " + STR_OAI_NAMESPACE + "2.0/OAI-PMH.xsd", xsi);

            // add "responseDate"...
            String sDate = getUTCDate(0);
            Element eDate = new Element("responseDate", ns);
            eDate.setText(sDate);
            eRoot.addContent(eDate);

            // add "request"...
            String url = request.getRequestURL().toString().split(";")[0];
            Element eRequest = new Element("request", ns);
            // eRequest.addContent(response.encodeURL(url));
            eRequest.addContent(url);
            eRoot.addContent(eRequest);

            org.jdom.Document document = null;

            // get parameter "verb"
            String verb[] = getParameter("verb", request);

            if ((verb == null) || (verb.length != 1)) {
                logger.info("Request without a verb.");
                document = addError(header, "badVerb", ERR_FAULTY_VERB);
            } else {
                // Check if a correct verb was given
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
            // <?xml-stylesheet type='text/xsl' href='/content/oai/oai2.xsl' ?>
            File f = new File(getServletContext().getRealPath("content/oai/oai2.xsl"));
            if (f.exists()) {
                String myURL = getBaseURL();
                if (myURL.length() == 0) {
                    String contextPath = request.getContextPath();
                    if (contextPath == null) {
                        contextPath = "";
                    }
                    contextPath += "/";
                    int pos = url.indexOf(contextPath, 9);
                    myURL = url.substring(0, pos) + contextPath;
                }

                Map<String, String> pairs = new HashMap<String, String>();
                pairs.put("type", "text/xsl");

                pairs.put("href", myURL + "content/oai/oai2.xsl");
                ProcessingInstruction pi = new ProcessingInstruction("xml-stylesheet", pairs);
                document.addContent(0, pi);
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
     * 
     * @return String
     */
    public String getServletInfo() {
        return "This class implements an OAI Data Provider for MyCoRe";
    }

    /**
     * Method getParameter. Extracts the value of <i>p </i> out of <i>request </i> and returns it.
     * 
     * @param p
     *            The name of the parameter to extract.
     * @param request
     *            The HttpServletRequest to extract the parameter from.
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
     * Method addError. insert <error>-Tag in <i>document </i>.
     * 
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
     * Method badArguments. Check the <i>request </i> for too much parameters
     * 
     * @param request
     * @param maxargs
     * @return boolean True, if too much parameters were found
     */
    private boolean badArguments(HttpServletRequest request, int maxargs) {
        Map parameterMap = request.getParameterMap();
        if (parameterMap.size() > maxargs) {
            return true;
        }

        // If there are no wrong parameters, it it possible some
        // parameters are doubled
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
     * 
     * @param elementName
     *            the element name to be created.
     * @param ns
     *            the namespace the element should be created in.
     * @param content
     * @return Element
     */
    private Element newElementWithContent(String elementName, Namespace ns, String content) {
        Element element = new Element(elementName, ns);
        element.addContent(content);
        return element;
    }

    /**
     * Method listFromResumptionToken. Get the element list from the Resumption Token File and add them to the <i>list </i>.
     * 
     * @param list
     *            The list to which the new Elements are added.
     * @param resumptionToken
     *            The Resumption Token to process.
     * @param prefix
     *            The metadataPrefix like oai_dc, epicur, xmetadiss
     * @param Namespace
     *            The namespace for oai-response
     * @param listType
     *            listtype like "identifiers" or "records"
     * @return Element The new List
     * @throws IOException
     */
    private Element listFromResumptionToken(Element list, String resumptionToken, String prefix, Namespace ns, String listType) throws IOException {

        try {
            String[] array = resumptionToken.split("x");
            String resumptionTokenID = array[0];

            int tokenNo = Integer.parseInt(array[1]);
            int cursor = tokenNo * maxReturns;
            int resumptionSize = Integer.parseInt(array[2]);
            int elementCounter = 0;

            int endObject = maxReturns;

            if (endObject > resumptionSize) {
                endObject = resumptionSize;
            }

            List result = resStore.getResumptionTokenHits(resumptionTokenID, resumptionSize, maxReturns);
            if (listType.equals("set")) {
                Iterator iterator = result.iterator();
                while (iterator.hasNext()) {
                    String[] setArray = (String[]) iterator.next();

                    Element eSet = new Element("set", ns);
                    eSet = setSpec(eSet, setArray[2], ns);
                    elementCounter++;
                    if (setArray[4].length() > 0) {
                        eSet.addContent(newElementWithContent("setName", ns, setArray[4]));
                    } else {
                        eSet.addContent(newElementWithContent("setName", ns, setArray[2]));
                    }
                    if ((setArray[5] != null) && (setArray[5].length() > 0)) {
                        Namespace oaidc = Namespace.getNamespace("oai_dc", STR_DC_NAMESPACE);
                        Element eDC = new Element("dc", oaidc);
                        Namespace dc = Namespace.getNamespace("dc", "http://purl.org/dc/elements/1.1/");
                        Namespace xsi = Namespace.getNamespace("xsi", STR_SCHEMA_INSTANCE);
                        eDC.addNamespaceDeclaration(dc);
                        eDC.addNamespaceDeclaration(xsi);
                        eDC.setAttribute("schemaLocation", STR_OAI_NAMESPACE + STR_OAI_VERSION + "/oai_dc/ " + STR_OAI_NAMESPACE + STR_OAI_VERSION
                                + "/oai_dc.xsd", xsi);
                        Element eDescription = new Element("description", dc);
                        eDescription.addContent(setArray[5]);
                        eDC.addContent(eDescription);
                        Element eSetDescription = new Element("setDescription", ns);
                        eSetDescription.addContent(eDC);
                        eSet.addContent(eSetDescription);
                    }
                    list.addContent(eSet);
                }
            } else {

                Iterator iterator = result.iterator();

                while (iterator.hasNext()) {
                    elementCounter++;

                    String[] identifier = (String[]) iterator.next();

                    if ((maxReturns == 0) || (elementCounter <= maxReturns)) {
                        Element eHeader = new Element("header", ns);
                        eHeader.addContent(newElementWithContent("identifier", ns, identifier[0]));
                        eHeader.addContent(newElementWithContent("datestamp", ns, identifier[1]));
                        eHeader = setSpec(eHeader, identifier[2], ns);

                        if (!listType.equals("records")) {
                            // listType == identifiers
                            list.addContent(eHeader);
                        } else {
                            Element eRecord = new Element("record", ns);
                            eRecord.addContent(eHeader);
                            MCRObject object = new MCRObject();
                            object.receiveFromDatastore(identifier[3]);
                            Element eMetadata = (Element) object.createXML().getRootElement().clone();
                            eRecord.addContent(eMetadata);
                            list.addContent(eRecord);
                        }
                    }
                }
            }

            Element eResumptionToken = new Element("resumptionToken", ns);
            eResumptionToken.setAttribute("completeListSize", Integer.toString(cursor + resumptionSize));
            eResumptionToken.setAttribute("cursor", Integer.toString(cursor));
            if (endObject < resumptionSize) {

                String newResumptionToken = resumptionTokenID + "x" + (tokenNo + 1) + "x" + (resumptionSize - endObject);

                String sDate = getUTCDate(oaiResumptionTokenTimeOut);
                eResumptionToken.addContent(newResumptionToken);
                eResumptionToken.setAttribute("expirationDate", sDate);
            }

            list.addContent(eResumptionToken);
            return list;
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }

    }

    /**
     * Method getDate. Gets the date from an ISO8601 string or null
     * 
     * @param date
     *            a ISO8601 conformant date string
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
        if (currentDate == null) {
            // try to match the simpler dateformat
            dateFormat = new SimpleDateFormat(STR_GRANULARITY_SHORT);
            pos = new ParsePosition(0);
            currentDate = dateFormat.parse(date, pos);
        }

        return currentDate;
    }

    /**
     * Method getUTCDate. The actual date and time (GMT).
     * 
     * @param timeout
     *            . offset (in hours) to add to get a future time.
     * @return String the date and time as string.
     */
    private String getUTCDate(int timeout) {
        // Calendar calendar = new GregorianCalendar();
        // Date now = new Date();
        // calendar.setTime(now);
        // SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        // SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
        // TimeZone tz = timeFormat.getTimeZone();
        // // compute milliseconds to hours...
        // int offset = Math.abs(tz.getRawOffset() / 3600000) * -1;
        // calendar.add(Calendar.HOUR, offset);
        // calendar.add(Calendar.HOUR, timeout);
        // Date timeoutDate = calendar.getTime();
        //
        // String sDate = dateFormat.format(timeoutDate) + "T"
        // + timeFormat.format(timeoutDate) + "Z";
        //
        // return sDate;
        Calendar calendar = new GregorianCalendar();
        calendar.setTime(new Date());
        calendar.add(Calendar.HOUR, timeout);
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        String datestamp = formatter.format(calendar.getTime());
        return datestamp;

    }

    /**
     * Method identify. Implementation of the OAI Verb Identify.
     * 
     * @param request
     *            The servlet request.
     * @param header
     *            The document so far
     * @return Document The document with all new elements added.
     */
    private org.jdom.Document identify(HttpServletRequest request, org.jdom.Document header) {
        logger.info("The harvester has called the 'Identify' value.");
        org.jdom.Document document = header;

        if (badArguments(request, 1)) {
            logger.warn("There are too many arguments in the request. OAI 2.0 define an interupt in this case.");
            return addError(document, "badArgument", ERR_ILLEGAL_ARGUMENT);
        }

        MCROAIConfigBean configBean = getConfigBean(getServletName());
        String repositoryIdentifier = configBean.getRepositoryIdentifier();
        String repositoryName = configBean.getRepositoryName();

        Element eRoot = document.getRootElement();
        Namespace ns = eRoot.getNamespace();
        Element eRequest = eRoot.getChild("request", ns);
        eRequest.setAttribute("verb", "Identify");
        Element eIdentify = new Element("Identify", ns);
        eIdentify.addContent(newElementWithContent("repositoryName", ns, repositoryName));
        if (getBaseURL().length() != 0) {
            eIdentify.addContent(newElementWithContent("baseURL", ns, getBaseURL() + "servlets/MCROAIProvider"));
        } else {
            eIdentify.addContent(newElementWithContent("baseURL", ns, request.getRequestURL().toString().split(";")[0]));
        }
        eIdentify.addContent(newElementWithContent("protocolVersion", ns, STR_OAI_VERSION));
        eIdentify.addContent(newElementWithContent("adminEmail", ns, oaiAdminEmail));
        eIdentify.addContent(newElementWithContent("earliestDatestamp", ns, STR_FIRST_DATE));
        eIdentify.addContent(newElementWithContent("deletedRecord", ns, "no"));
        eIdentify.addContent(newElementWithContent("granularity", ns, "YYYY-MM-DD"));
        // If we don't support compression, this SHOULD NOT be mentioned, so it
        // is outmarked
        // eIdentify.addContent(newElementWithContent("compression", ns,
        // "identity"));

        Element eOAIDescription = new Element("description", ns);
        Namespace idns = Namespace.getNamespace(STR_OAI_NAMESPACE + "2.0/oai-identifier");
        Element eOAIIdentifier = new Element("oai-identifier", idns);
        Namespace xsi = Namespace.getNamespace("xsi", STR_SCHEMA_INSTANCE);
        eOAIIdentifier.addNamespaceDeclaration(xsi);
        eOAIIdentifier.setAttribute("schemaLocation", STR_OAI_NAMESPACE + "2.0/oai-identifier " + STR_OAI_NAMESPACE + "2.0/oai-identifier.xsd", xsi);

        eOAIIdentifier.addContent(newElementWithContent("scheme", idns, "oai"));
        eOAIIdentifier.addContent(newElementWithContent("repositoryIdentifier", idns, repositoryIdentifier));
        eOAIIdentifier.addContent(newElementWithContent("delimiter", idns, ":"));
        eOAIIdentifier.addContent(newElementWithContent("sampleIdentifier", idns, "oai:" + repositoryIdentifier + ":MyCoReDemoDC_Document_1"));

        eOAIDescription.addContent(eOAIIdentifier);
        eIdentify.addContent(eOAIDescription);

        try {
            String[] friendsAr = oaiProviderFriends.split(",");
            if (!oaiProviderFriends.equals("") && friendsAr.length > 0) {
                Element eFriendsDescription = new Element("description", ns);
                Namespace frns = Namespace.getNamespace(STR_OAI_NAMESPACE + "2.0/friends/");
                Element eFriends = new Element("friends", frns);
                eFriends.addNamespaceDeclaration(xsi);
                eFriends.setAttribute("schemaLocation", STR_OAI_NAMESPACE + "2.0/friends/ " + STR_OAI_NAMESPACE + "2.0/friends.xsd", xsi);
                for (int i = 0; i < friendsAr.length; i++) {
                    eFriends.addContent(newElementWithContent("baseURL", frns, "http://" + friendsAr[i]));
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
     * 
     * @param request
     *            The servlet request.
     * @param header
     *            The document so far
     * @return Document The document with all new elements added.
     */
    private org.jdom.Document listMetadataFormats(HttpServletRequest request, org.jdom.Document header) {
        logger.info("Harvester hat 'listMetadatFormats' angefordert");
        org.jdom.Document document = header;

        Element eRoot = document.getRootElement();
        Namespace ns = eRoot.getNamespace();
        MCROAIQuery query = null;

        try {
            query = (MCROAIQuery) CONFIG.getInstanceOf(STR_OAI_QUERYSERVICE);
        } catch (MCRConfigurationException mcrx) {
            logger.fatal("Missing configuration item: " + STR_OAI_QUERYSERVICE + " is missing.");
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
            // Es ist nur ein Identifier erlaubt!
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
                record = query.getRecord(id, "oai_dc", getServletName());
            }
        }

        // The supported metadata formats are only returned if no identifier was
        // given, or
        // the identifier was found in the repository
        Element eListMetadataFormats = new Element("ListMetadataFormats", ns);
        Element dcMetadataFormat = new Element("metadataFormat", ns);
        dcMetadataFormat.addContent(newElementWithContent("metadataPrefix", ns, "oai_dc"));
        dcMetadataFormat.addContent(newElementWithContent("schema", ns, STR_DC_SCHEMA));
        dcMetadataFormat.addContent(newElementWithContent("metadataNamespace", ns, STR_DC_NAMESPACE));
        eListMetadataFormats.addContent(dcMetadataFormat);

        Properties properties = CONFIG.getProperties(STR_OAI_METADATA_NAMESPACE);
        Enumeration propertiesNames = properties.propertyNames();
        while (propertiesNames.hasMoreElements()) {
            String name = (String) propertiesNames.nextElement();
            String metadataPrefix = name.substring(name.lastIndexOf(".") + 1);
            if (record != null) {
                // Identifier submitted
                Element metadata = (Element) record.get(1);
                try {
                    String namespace = CONFIG.getString(STR_OAI_METADATA_NAMESPACE + "." + metadataPrefix);
                    String elementName = CONFIG.getString(STR_OAI_METADATA_ELEMENT + "." + metadataPrefix);
                    Namespace mns = Namespace.getNamespace(metadataPrefix, namespace);
                    if (metadata.getChild(elementName, mns) == null) {
                        continue;
                    }
                } catch (MCRConfigurationException e) {
                    continue;
                }
            }
            Element eMetadataFormat = new Element("metadataFormat", ns);
            eMetadataFormat.addContent(newElementWithContent("metadataPrefix", ns, metadataPrefix));
            eMetadataFormat.addContent(newElementWithContent("schema", ns, CONFIG.getString(STR_OAI_METADATA_SCHEMA + "." + metadataPrefix)));
            eMetadataFormat.addContent(newElementWithContent("metadataNamespace", ns, CONFIG.getString(STR_OAI_METADATA_NAMESPACE + "." + metadataPrefix)));
            eListMetadataFormats.addContent(eMetadataFormat);
        }

        eRoot.addContent(eListMetadataFormats);

        return document;
    }

    /**
     * Method listSets. Implementation of the OAI Verb ListSets.
     * 
     * @param request
     *            The servlet request.
     * @param header
     *            The document so far
     * @return Document The document with all new elements added.
     */
    private org.jdom.Document listSets(HttpServletRequest request, org.jdom.Document header) {
        org.jdom.Document document = header;
        Element eRoot = document.getRootElement();
        Namespace ns = eRoot.getNamespace();

        resStore.deleteOutdatedTokens();

        int maxArguments = 1; // the parameter verb is allways there
        String resumptionToken[] = getParameter("resumptionToken", request);
        if (resumptionToken != null) {
            maxArguments++;
        }
        // The number of arguments must not exceed maxArguments
        if (badArguments(request, maxArguments)) {
            logger.info("Anfrage 'listSets' enthaelt fehlerhafte Parameter.");
            return addError(document, "badArgument", ERR_ILLEGAL_ARGUMENT);
        }

        Element eRequest = eRoot.getChild("request", ns);
        eRequest.setAttribute("verb", "ListSets");
        if (resumptionToken != null) {
            eRequest.setAttribute("resumptionToken", resumptionToken[0]);
        }

        Element eListSets = new Element("ListSets", ns);

        if (resumptionToken != null) {
            try {
                eListSets = listFromResumptionToken(eListSets, resumptionToken[0], "null", ns, "set");
                if (eListSets == null) {
                    logger.info("Anfrage 'listSets' enthaelt fehlerhaften Resumption Token " + resumptionToken[0] + ".");
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
        MCROAIQuery query = null;

        try {
            query = (MCROAIQuery) CONFIG.getInstanceOf(STR_OAI_QUERYSERVICE);
        } catch (MCRConfigurationException mcrx) {
            logger.fatal(mcrx.getMessage());
            return addError(document, "badResumptionToken", mcrx.getMessage());
        }

        sets = new ArrayList(query.listSets(getServletName()));

        if (sets != null) {
            ListIterator iterator = sets.listIterator();

            int elementCounter = 0;
            String sResumptionToken = null;
            List resumptionList = new ArrayList();
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
                    eDC.setAttribute("schemaLocation", STR_OAI_NAMESPACE + STR_OAI_VERSION + "/oai_dc/ " + STR_OAI_NAMESPACE + STR_OAI_VERSION + "/oai_dc.xsd",
                            xsi);
                    Element eDescription = new Element("description", dc);
                    eDescription.addContent(set[2]);
                    eDC.addContent(eDescription);
                    Element eSetDescription = new Element("setDescription", ns);
                    eSetDescription.addContent(eDC);
                    eSet.addContent(eSetDescription);
                }

                if ((maxReturns == 0) || (elementCounter <= maxReturns)) {
                    eListSets.addContent(eSet);
                }
                resumptionList.add(set);
            }

            int docs = elementCounter - maxReturns;
            if (docs > 0) {
                sResumptionToken = newResumptionToken();
                resStore.createResumptionToken(sResumptionToken, "set", getServletName(), resumptionList);
                sResumptionToken += "x1x" + docs;
            } else {
                sResumptionToken = null;
            }
            if (sResumptionToken != null) {
                Element eResumptionToken = new Element("resumptionToken", ns);
                eResumptionToken.setAttribute("completeListSize", Integer.toString(elementCounter));
                eResumptionToken.setAttribute("cursor", "0");

                String sDate = getUTCDate(oaiResumptionTokenTimeOut);
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
     * 
     * @param request
     *            The servlet request.
     * @param header
     *            The document so far
     * @return Document The document with all new elements added.
     */
    private org.jdom.Document listIdentifiers(HttpServletRequest request, org.jdom.Document header) {
        org.jdom.Document document = header;
        Element eRoot = document.getRootElement();
        Namespace ns = eRoot.getNamespace();

        resStore.deleteOutdatedTokens();

        int maxArguments = 2; // verb and metadataPrefix are required!

        // get the arguments from the request
        String from[] = getParameter("from", request);
        String until[] = getParameter("until", request);
        String set[] = getParameter("set", request);
        String metadataPrefix[] = getParameter("metadataPrefix", request);
        String resumptionToken[] = getParameter("resumptionToken", request);
        if (resumptionToken != null) {
            maxArguments++;
            if ((from != null) || (until != null) || (set != null) || (metadataPrefix != null)) {
                logger.info("Anfrage 'listRecords' enthaelt fehlerhafte Parameter.");
                return addError(document, "badArgument", ERR_ILLEGAL_ARGUMENT);
            }
        } else {
            Date fromDate = null;
            if (from != null) {
                fromDate = getDate(from[0]);
                if (fromDate == null) {
                    logger.info("Anfrage 'listIdentifiers' enthaelt fehlerhafte Parameter.");
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
                    logger.info("Anfrage 'listIdentifiers' enthaelt fehlerhafte Parameter.");
                    return addError(document, "badArgument", ERR_ILLEGAL_ARGUMENT);
                }
                if (fromDate != null) {
                    if (fromDate.after(untilDate)) {
                        logger.info("Anfrage 'listIdentifiers' enthaelt fehlerhafte Parameter.");
                        return addError(document, "noRecordsMatch", ERR_NO_RECORDS_MATCH);
                    }
                }
                maxArguments++;
            }
            if (set != null) {
                maxArguments++;
            }
            if (metadataPrefix == null) {
                logger.info("Anfrage 'listRecords' enthaelt fehlerhafte Parameter.");
                return addError(document, "badArgument", ERR_ILLEGAL_ARGUMENT);
            }
        }
        // The number of arguments must not exceed maxArguments
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
            try {
                prefix = resStore.getPrefix(resumptionToken[0].substring(0, resumptionToken[0].indexOf("x")));
            } catch (StringIndexOutOfBoundsException ex) {
                logger.info("Error in resumption token.", ex);
                return addError(document, "badResumptionToken", ERR_BAD_RESUMPTION_TOKEN);
            }
            if (prefix == null) {
                logger.info("Error in resumption token.");
                return addError(document, "badResumptionToken", ERR_BAD_RESUMPTION_TOKEN);
            }
        }

        // Check, if the requested metadata format is supported
        try {
            // check if property is set, else Exception is thrown
            CONFIG.getString(STR_OAI_METADATA_TRANSFORMER + "." + prefix);
        } catch (MCRConfigurationException mcrx) {
            logger.info("Anfrage 'listIdentifiers' wegen unbekanntem Metadatenformat " + prefix + " abgebrochen.");
            return addError(document, "cannotDisseminateFormat", ERR_UNKNOWN_FORMAT);
        }

        Element eListIdentifiers = new Element("ListIdentifiers", ns);

        if (resumptionToken != null) {
            try {
                eListIdentifiers = listFromResumptionToken(eListIdentifiers, resumptionToken[0], prefix, ns, "identifiers");
                if (eListIdentifiers == null) {
                    logger.info("Anfrage 'listIdentifiers' enthaelt fehlerhaften Resumption Token " + resumptionToken[0] + ".");
                    return addError(document, "badArgument", ERR_ILLEGAL_ARGUMENT);
                }
                eRoot.addContent(eListIdentifiers);
            } catch (IOException e) {
                logger.error(e.getMessage());
                return addError(document, "badResumptionToken", ERR_BAD_RESUMPTION_TOKEN);
            }

            return document;
        }

        int elementCounter = 0;
        String sResumptionToken = null;
        List resumptionList = new ArrayList();
        MCROAIQuery query = null;
        try {

            query = (MCROAIQuery) CONFIG.getInstanceOf(STR_OAI_QUERYSERVICE);
        } catch (MCRConfigurationException e) {
            logger.fatal(e.getMessage());
            return addError(document, "noRecordsMatch", ERR_NO_RECORDS_MATCH);
        }

        String instance = getServletName();
        String repositoryID = getConfigBean(instance).getRepositoryIdentifier();
        do {
            List result = query.listIdentifiers(set, from, until, prefix, instance);
            if (result != null) {
                Iterator iterator = result.iterator();
                while (iterator.hasNext()) {
                    String objectId = (String) iterator.next();
                    elementCounter++;
                    if ((maxReturns == 0) || (elementCounter <= maxReturns)) {
                        MCRObject object = new MCRObject();
                        object.receiveFromDatastore(objectId);
                        String[] array = getHeader(object, objectId, repositoryID, instance);
                        Element eHeader = new Element("header", ns);
                        eHeader.addContent(newElementWithContent("identifier", ns, array[0]));
                        eHeader.addContent(newElementWithContent("datestamp", ns, array[1]));
                        eHeader = setSpec(eHeader, array[2], ns);
                        eListIdentifiers.addContent(eHeader);
                    }
                    resumptionList.add(objectId);
                }
            }
        } while (query.hasMore());

        int docs = elementCounter - maxReturns;
        if (docs > 0) {
            sResumptionToken = newResumptionToken();
            resStore.createResumptionToken(sResumptionToken, prefix, instance, resumptionList);
            sResumptionToken += "x1x" + docs;
        } else {
            sResumptionToken = null;
        }

        if (elementCounter > 0) {
            if (sResumptionToken != null) {
                Element eResumptionToken = new Element("resumptionToken", ns);
                eResumptionToken.setAttribute("completeListSize", Integer.toString(elementCounter));
                eResumptionToken.setAttribute("cursor", "0");

                String sDate = getUTCDate(oaiResumptionTokenTimeOut);
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
     * 
     * @param request
     *            The servlet request.
     * @param header
     *            The document so far
     * @return Document The document with all new elements added.
     * @throws JDOMException 
     * @throws IOException 
     */
    private org.jdom.Document getRecord(HttpServletRequest request, org.jdom.Document header) throws IOException, JDOMException {
        org.jdom.Document document = header;
        Element eRoot = document.getRootElement();
        Namespace ns = eRoot.getNamespace();

        // First: check if there was an identifier in the request (required)
        String identifier[] = getParameter("identifier", request);
        // Then: check if there was an metadataPrefix in the request (required)
        String metadataPrefix[] = getParameter("metadataPrefix", request);
        if ((identifier == null) || (metadataPrefix == null) || (identifier.length != 1) || (metadataPrefix.length != 1) || badArguments(request, 3)) {
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

        // Check, if the requested metadata format is supported
        String format = null;
        // Check, if the requested metadata format is supported
        try {
            format = CONFIG.getString(STR_OAI_METADATA_TRANSFORMER + "." + metadataPrefix[0]);
            logger.info("Transformer: " + format);
        } catch (MCRConfigurationException mcrx) {
            logger.info("Anfrage 'getRecord' wurde wegen fehlendem Metadatenformat " + metadataPrefix[0] + " abgebrochen.");
            return addError(document, "cannotDisseminateFormat", ERR_UNKNOWN_FORMAT);
        }

        // Then look, if the id exists
        MCROAIQuery query = null;
        try {
            query = (MCROAIQuery) CONFIG.getInstanceOf(STR_OAI_QUERYSERVICE);
        } catch (MCRConfigurationException mcrx) {
            logger.fatal(mcrx.getMessage());
            return addError(document, "idDoesNotExist", ERR_UNKNOWN_ID);
        }

        if (query.exists(id)) {
            List record = query.getRecord(id, metadataPrefix[0], getServletName());
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

                org.jdom.Document newDocument = doMCRXSLTransformation(request, document, format);
                if (newDocument != null) {
                    document = newDocument;
                } else {
                    logger.error("Die Transformation in 'getRecord' hat nicht funktioniert.");
                }
            } else {
                logger.info("Anfrage 'getRecord' wurde wegen fehlendem Metadatenformat " + metadataPrefix[0] + " abgebrochen.");
                return addError(document, "cannotDisseminateFormat", ERR_UNKNOWN_FORMAT);
            }
        } else {
            logger.info("Anfrage 'getRecord' wurde fegen fehlender ID " + id + " abgebrochen.");
            return addError(document, "idDoesNotExist", ERR_UNKNOWN_ID);
        }

        return document;
    }

    /**
     * Method listRecords. Implementation of the OAI Verb ListRecords.
     * 
     * @param request
     *            The servlet request.
     * @param header
     *            The document so far
     * @return Document The document with all new elements added.
     * @throws JDOMException 
     * @throws IOException 
     */
    private org.jdom.Document listRecords(HttpServletRequest request, org.jdom.Document header) throws IOException, JDOMException {
        org.jdom.Document document = header;
        Element eRoot = document.getRootElement();
        Namespace ns = eRoot.getNamespace();
        resStore.deleteOutdatedTokens();

        int maxArguments = 2; // verb and metadataPrefix are required!

        // Second: get the arguments from the request
        String from[] = getParameter("from", request);
        String until[] = getParameter("until", request);
        String set[] = getParameter("set", request);
        String metadataPrefix[] = getParameter("metadataPrefix", request);
        String resumptionToken[] = getParameter("resumptionToken", request);
        if (resumptionToken != null) {
            maxArguments++;
            if ((from != null) || (until != null) || (set != null) || (metadataPrefix != null)) {
                logger.info("Anfrage 'listRecords' enthaelt fehlerhafte Parameter.");
                return addError(document, "badArgument", ERR_ILLEGAL_ARGUMENT);
            }
        } else {
            Date fromDate = null;
            if (from != null) {
                fromDate = getDate(from[0]);
                if (fromDate == null) {
                    logger.info("Anfrage 'listRecords' enthaelt fehlerhafte Parameter.");
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
                    logger.info("Anfrage 'listRecords' enthaelt nicht das richtige Datumsformat.");
                    return addError(document, "badArgument", ERR_ILLEGAL_ARGUMENT);
                }
                if (fromDate != null) {
                    if (fromDate.after(untilDate)) {
                        logger.info("Anfrage 'listRecords' enthaelt nicht das richtige Datumsformat.");
                        return addError(document, "noRecordsMatch", ERR_NO_RECORDS_MATCH);
                    }
                }
                maxArguments++;
            }
            if (set != null) {
                maxArguments++;
            }
            if (metadataPrefix == null) {
                logger.info("Anfrage 'listRecords' enthaelt fehlerhafte Parameter.");
                return addError(document, "badArgument", ERR_ILLEGAL_ARGUMENT);
            }
        }
        // The number of arguments must not exceed maxArguments
        if (badArguments(request, maxArguments)) {
            logger.info("Anfrage 'listRecords' enthaelt fehlerhafte Parameter.");
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
            try {
                prefix = resStore.getPrefix(resumptionToken[0].substring(0, resumptionToken[0].indexOf("x")));
            } catch (StringIndexOutOfBoundsException ex) {
                logger.info("Error in resumption token.", ex);
                return addError(document, "badResumptionToken", ERR_BAD_RESUMPTION_TOKEN);
            }
            if (prefix == null) {
                logger.info("Error in resumption token.");
                return addError(document, "badResumptionToken", ERR_BAD_RESUMPTION_TOKEN);
            }
        }

        String format = null;

        // Check, if the requested metadata format is supported
        try {
            format = CONFIG.getString(STR_OAI_METADATA_TRANSFORMER + "." + prefix);
        } catch (MCRConfigurationException mcrx) {
            logger.info("Anfrage 'listRecords' wurde wegen fehlendem Metadatenformat " + prefix + " abgebrochen.");
            return addError(document, "cannotDisseminateFormat", ERR_UNKNOWN_FORMAT);
        }

        Element eListRecords = new Element("ListRecords", ns);

        if (resumptionToken != null) {
            try {
                eListRecords = listFromResumptionToken(eListRecords, resumptionToken[0], prefix, ns, "records");
                if (eListRecords == null) {
                    logger.info("Anfrage 'listRecords' enthaelt fehlerhaften Resumption Token " + resumptionToken[0] + ".");
                    return addError(document, "badArgument", ERR_ILLEGAL_ARGUMENT);
                }
                eRoot.addContent(eListRecords);
            } catch (IOException e) {
                logger.error(e.getMessage());
                return addError(document, "badResumptionToken", ERR_BAD_RESUMPTION_TOKEN);
            }

            org.jdom.Document newDocument = doMCRXSLTransformation(request, document, format);
            if (newDocument != null) {
                document = newDocument;
            } else {
                logger.error("Die Transformation in 'listRecords' hat nicht funktioniert.");
            }

            return document;
        }

        int elementCounter = 0;

        String sResumptionToken = null;
        List resumptionList = new ArrayList();
        MCROAIQuery query = null;
        try {

            query = (MCROAIQuery) CONFIG.getInstanceOf(STR_OAI_QUERYSERVICE);
        } catch (MCRConfigurationException e) {
            logger.fatal(e.getMessage());
            return addError(document, "noRecordsMatch", ERR_NO_RECORDS_MATCH);
        }
        String instance = getServletName();
        String repositoryID = getConfigBean(instance).getRepositoryIdentifier();

        do {
            List result = query.listRecords(set, from, until, prefix, instance);
            if (result != null) {
                Iterator iterator = result.iterator();
                while (iterator.hasNext()) {
                    String objectId = (String) iterator.next();
                    elementCounter++;

                    if ((maxReturns == 0) || (elementCounter <= maxReturns)) {

                        MCRObject object = new MCRObject();
                        object.receiveFromDatastore(objectId);
                        String[] array = getHeader(object, objectId, repositoryID, instance);
                        Element eMetadata = (Element) object.createXML().getRootElement().clone();
                        Element eHeader = new Element("header", ns);
                        eHeader.addContent(newElementWithContent("identifier", ns, array[0]));
                        eHeader.addContent(newElementWithContent("datestamp", ns, array[1]));
                        eHeader = setSpec(eHeader, array[2], ns);

                        Element eRecord = new Element("record", ns);
                        eRecord.addContent(eHeader);

                        eRecord.addContent(eMetadata);
                        eListRecords.addContent(eRecord);
                    }
                    // put all headers in resumptionList
                    resumptionList.add(objectId);
                }
            }
        } while (query.hasMore());

        int docs = elementCounter - maxReturns;
        if (docs > 0) {
            sResumptionToken = newResumptionToken();
            resStore.createResumptionToken(sResumptionToken, prefix, getServletName(), resumptionList);
            sResumptionToken += "x1x" + docs;
        } else {
            sResumptionToken = null;
        }

        if (elementCounter > 0) {
            if (sResumptionToken != null) {
                Element eResumptionToken = new Element("resumptionToken", ns);
                eResumptionToken.setAttribute("completeListSize", Integer.toString(elementCounter));
                eResumptionToken.setAttribute("cursor", "0");

                String sDate = getUTCDate(oaiResumptionTokenTimeOut);
                eResumptionToken.addContent(sResumptionToken);
                eResumptionToken.setAttribute("expirationDate", sDate);
                eListRecords.addContent(eResumptionToken);
            }

            eRoot.addContent(eListRecords);

            org.jdom.Document newDocument = doMCRXSLTransformation(request, document, format);
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
     * 
     * @param identifier
     *            a legal MCRException
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
        String repositoryIdentifier = getConfigBean(getServletName()).getRepositoryIdentifier();
        if (!"oai".equals(scheme) || !repositoryIdentifierFromIdentifier.equals(repositoryIdentifier)) {
            throw new MCRException("Error in legalOAIIdentifier");
        }

        String allowed = new String("1234567890abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ;/?:@&=+$,-_.!~*�()");
        StringBuffer buffer = new StringBuffer(tokenizer.nextToken());
        while (tokenizer.hasMoreTokens()) {
            buffer.append(":").append(tokenizer.nextToken());
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
     * @param element
     *            the Header element
     * @param spec
     *            the set specs
     * @param ns
     *            the Namespace of <code>element</code>
     * @return the new header element
     */
    private Element setSpec(Element element, String setSpec, Namespace ns) {
        if ((setSpec != null) && (setSpec.length() > 0)) {
            StringTokenizer tokenizer = new StringTokenizer(setSpec, " ");
            while (tokenizer.hasMoreTokens()) {
                String spec = tokenizer.nextToken();
                element.addContent(newElementWithContent("setSpec", ns, spec));
            }

        }
        return element;
    }

    private String newResumptionToken() {
        Date tmpDate = new Date();
        long id = tmpDate.getTime();
        while (id == lastTokenID) {
            tmpDate = new Date();
            id = tmpDate.getTime();
        }
        String token = Long.toString(id);
        return token;
    }

    /**
     * Method doMCRXSLTransformation. Executes the XSL-Transformation of the OAI-Metadata
     * 
     * @param request
     * @param document
     *            the document that has to be transformed
     * @param format
     *            name of the transforming stylesheet
     * @return document the transformed Jdom-Document
     * @throws JDOMException 
     * @throws IOException 
     */
    private Document doMCRXSLTransformation(HttpServletRequest request, Document document, String format) throws IOException, JDOMException {
        // biuld common property
        HttpSession session = request.getSession(false);
        Properties parameters = new Properties();
        if (session != null) {
            parameters.put("JSessionID", ";jsessionid=" + session.getId());
        }
        // set connection URL
        if (getBaseURL().length() != 0) {
            parameters.put("ServletsBaseURL", getBaseURL() + "servlets/");
            parameters.put("WebApplicationBaseURL", getBaseURL());
        } else {
            String contextPath = request.getContextPath() + "/";
            int pos = request.getRequestURL().indexOf(contextPath, 9);
            String servletsBaseURL = request.getRequestURL().substring(0, pos) + contextPath + "servlets/";
            String webApplicationBaseURL = request.getRequestURL().substring(0, pos) + contextPath;
            parameters.put("ServletsBaseURL", servletsBaseURL);
            parameters.put("WebApplicationBaseURL", webApplicationBaseURL);
        }
        // DNB erlaubt in Epicur-Beschreibung keinen Inhalt f�r das Element
        // <resupply>
        // String email = CONFIG.getString("MCR.oai.epicur.responseemail", "");
        // if(format.contains("epicur")&& !email.equals("")){
        // parameters.put("ResponseEmail", email);
        // }
        return MCRXSLTransformation.transform(document, new JDOMSource(MCRXMLResource.instance().getResource("xsl/" + format)), parameters);
    }

    static MCROAIConfigBean getConfigBean(String instance) {
        return (MCROAIConfigBean) oaiConfig.get(instance);
    }

    static int getMaximalHitsize() {
        return maxReturns;
    }

    /**
     * Method getHeader. Gets the header information from the MCRObject <i>object </i>.
     * 
     * @param object
     *            The MCRObject
     * @param objectId
     *            The objectId as String representation
     * @param repositoryId
     *            The repository id
     * @return String[] Array of three Strings: the identifier, a datestamp (modification date) and a string with a blank separated list of categories the
     *         element is classified in
     */
    public static String[] getHeader(MCRObject object, String objectId, String repositoryId, String instance) {
        Date date = object.getService().getDate("modifydate");

        // Format the date.
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        String datestamp = formatter.format(date);

        StringBuffer setSpec = new StringBuffer("");
        String[] identifier = new String[5];
        identifier[0] = "oai:" + repositoryId + ":" + objectId;
        identifier[1] = datestamp;
        identifier[2] = new String("");
        identifier[3] = objectId;

        List classifications = Arrays.asList(MCROAIProvider.getConfigBean(instance).getClassificationIDs());

        for (int j = 0; j < object.getMetadata().size(); j++) {
            if (object.getMetadata().getMetadataElement(j).getClassName().equals("MCRMetaClassification")) {
                MCRMetaElement element = object.getMetadata().getMetadataElement(j);

                for (int k = 0; k < element.size(); k++) {
                    MCRMetaClassification classification = (MCRMetaClassification) element.getElement(k);
                    String classificationId = classification.getClassId();
                    if (classifications.contains(classificationId)) {
                        String categoryId = classification.getCategId();
                        MCRCategory category = MCRCategoryDAOFactory.getInstance().getCategory(new MCRCategoryID(classificationId, categoryId), -1);
                        Collection<org.mycore.datamodel.classifications2.MCRLabel> labels = category.getLabels().values();
                        boolean found = false;
                        for (MCRLabel label : labels) {
                            if (label.getLang().equals("x-dini")) {
                                setSpec.append(" ").append(label.getText());
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            categoryId = category.getParent().getId().getID() + ":" + categoryId;
                            setSpec.append(" ").append(categoryId);
                        }
                    }
                }

                identifier[2] = setSpec.toString().trim();
            }
        }

        return identifier;
    }

}