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

package org.mycore.common.xml;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.file.FileStore;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.Normalizer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TimeZone;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.text.StringEscapeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.xml.utils.XMLChar;
import org.jdom2.JDOMException;
import org.jdom2.output.DOMOutputter;
import org.mycore.access.MCRAccessManager;
import org.mycore.common.MCRCalendar;
import org.mycore.common.MCRClassTools;
import org.mycore.common.MCRException;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRSystemUserInformation;
import org.mycore.common.MCRUtils;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.content.MCRSourceContent;
import org.mycore.datamodel.classifications2.MCRCategLinkReference;
import org.mycore.datamodel.classifications2.MCRCategLinkService;
import org.mycore.datamodel.classifications2.MCRCategLinkServiceFactory;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryDAO;
import org.mycore.datamodel.classifications2.MCRCategoryDAOFactory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.classifications2.MCRLabel;
import org.mycore.datamodel.common.MCRISO8601Date;
import org.mycore.datamodel.common.MCRLinkTableManager;
import org.mycore.datamodel.metadata.MCRMetaLinkID;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.metadata.MCRObjectStructure;
import org.mycore.datamodel.niofs.MCRPath;
import org.mycore.resource.MCRResourceHelper;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import jakarta.activation.MimetypesFileTypeMap;
import jakarta.ws.rs.core.UriBuilder;

/**
 * @author Thomas Scheffler (yagee)
 * @author Jens Kupferschmidt
 * @author shermann
 * @author René Adler (eagle)
 */
public class MCRXMLFunctions {
    private static final String TAG_START = "\\<\\w+((\\s+\\w+(\\s*\\=\\s*(?:\".*?\"|'.*?'|[^'\"\\>\\s]+))?)+"
        + "\\s*|\\s*)\\>";

    private static final String TAG_END = "\\</\\w+\\>";

    private static final String TAG_SELF_CLOSING = "\\<\\w+((\\s+\\w+(\\s*\\=\\s*(?:\".*?\"|'.*?'|[^'\"\\>\\s]+))?)+"
        + "\\s*|\\s*)/\\>";

    private static final String HTML_ENTITY = "&[a-zA-Z][a-zA-Z0-9]+;";

    private static final Pattern TAG_PATTERN = Pattern.compile(TAG_START + "((.*?[^\\<]))" + TAG_END, Pattern.DOTALL);

    private static final Pattern HTML_MATCH_PATTERN = Pattern
        .compile("(" + TAG_START + "((.*?[^\\<]))" + TAG_END + ")|(" + TAG_SELF_CLOSING + ")|(" + HTML_ENTITY + ")",
            Pattern.DOTALL);

    private static final Logger LOGGER = LogManager.getLogger();
    private static volatile MimetypesFileTypeMap mimetypeMap;

    public static Node document(String uri) throws JDOMException, IOException, TransformerException {
        MCRSourceContent sourceContent = MCRSourceContent.createInstance(uri);
        if (sourceContent == null) {
            throw new TransformerException("Could not load document: " + uri);
        }
        DOMOutputter out = new DOMOutputter();
        return out.output(sourceContent.asXML());
    }

    /**
     * returns the given String trimmed
     *
     * @param arg0
     *            String to be trimmed
     * @return trimmed copy of arg0
     * @see java.lang.String#trim()
     */
    public static String trim(String arg0) {
        return arg0.trim();
    }

    /**
     * returns the given String in unicode NFC normal form.
     *
     * @param arg0 String to be normalized
     * @see Normalizer#normalize(CharSequence, java.text.Normalizer.Form)
     */
    public static String normalizeUnicode(String arg0) {
        return Normalizer.normalize(arg0, Normalizer.Form.NFC);
    }

    public static String formatISODate(String isoDate, String simpleFormat, String iso639Language) {
        return formatISODate(isoDate, null, simpleFormat, iso639Language);
    }

    public static String formatISODate(String isoDate, String isoFormat, String simpleFormat, String iso639Language) {
        return formatISODate(isoDate, isoFormat, simpleFormat, iso639Language, TimeZone.getDefault().getID());
    }

    public static String formatISODate(String isoDate, String isoFormat, String simpleFormat, String iso639Language,
        String timeZone) {
        if (LOGGER.isDebugEnabled()) {
            String sb = "isoDate=" + isoDate + ", simpleFormat=" + simpleFormat + ", isoFormat=" + isoFormat
                + ", iso639Language=" + iso639Language + ", timeZone=" + timeZone;
            LOGGER.debug(sb);
        }
        Locale locale = Locale.forLanguageTag(iso639Language);
        MCRISO8601Date mcrdate = new MCRISO8601Date();
        mcrdate.setFormat(isoFormat);
        mcrdate.setDate(isoDate);
        try {
            String formatted = mcrdate.format(simpleFormat, locale, timeZone);
            return formatted == null ? "?" + isoDate + "?" : formatted;
        } catch (RuntimeException iae) {
            LOGGER.error("Unable to format date {} to {} with locale {} and timezone {}",
                mcrdate::getISOString, () -> simpleFormat, () -> locale, () -> timeZone, () -> iae);
            return "?";
        }
    }

    public static String getISODate(String simpleDate, String simpleFormat, String isoFormat) throws ParseException {
        Date date;
        if (simpleFormat.equals("long")) {
            date = new Date(Long.parseLong(simpleDate));
        } else {
            SimpleDateFormat df = new SimpleDateFormat(simpleFormat, Locale.ROOT);
            df.setTimeZone(TimeZone.getTimeZone("UTC"));
            // or else testcase
            // "1964-02-24" would
            // result "1964-02-23"
            date = df.parse(simpleDate);
        }
        return getISODate(date, isoFormat);
    }

    public static String getISODate(Date date, String isoFormat) {
        MCRISO8601Date mcrdate = new MCRISO8601Date();
        mcrdate.setDate(date);
        mcrdate.setFormat(isoFormat);
        return mcrdate.getISOString();
    }

    public static String getISODate(String simpleDate, String simpleFormat) throws ParseException {
        return getISODate(simpleDate, simpleFormat, null);
    }

    /**
     * The method get a date String in format yyyy-MM-ddThh:mm:ssZ for ancient date values.
     *
     * @param date the date string
     * @param fieldName the name of field of MCRMetaHistoryDate, it should be 'von' or 'bis'
     * @param calendarName the name if the calendar defined in MCRCalendar
     * @return the date in format yyyy-MM-ddThh:mm:ssZ
     */
    public static String getISODateFromMCRHistoryDate(String date, String fieldName, String calendarName) {
        if (fieldName == null || fieldName.isBlank()) {
            return "";
        }
        boolean useLastValue = Objects.equals(fieldName, "bis");
        return MCRCalendar.getISODateToFormattedString(date, useLastValue, calendarName);
    }

    /**
     * Returns a string representing the current date. One can customize the
     * format of the returned string by providing a proper value for the format
     * parameter. If null or an invalid format is provided the default format
     * "yyyy-MM-dd" will be used.
     *
     * @param format
     *            the format in which the date should be formatted
     * @return the current date in the desired format
     * @see SimpleDateFormat format description
     */
    public static String getCurrentDate(String format) {
        SimpleDateFormat sdf;
        try {
            sdf = new SimpleDateFormat(format, Locale.ROOT);
        } catch (Exception i) {
            LOGGER.warn("Could not parse date format string, will use default \"yyyy-MM-dd\"", i);
            sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT);
        }
        return sdf.format(new Date());
    }

    /**
     * A delegate for {@link String#compareTo(String)}.
     *
     * @return s1.compareTo(s2)
     */
    public static int compare(String s1, String s2) {
        return s1.compareTo(s2);
    }

    /**
     * @param source the source string to operate on
     * @param regex the regular expression to apply
     */
    public static String regexp(String source, String regex, String replace) {
        try {
            return source.replaceAll(regex, replace);
        } catch (Exception e) {
            LOGGER.warn("Could not apply regular expression. Returning source string ({}).", source);
            return source;
        }
    }

    /**
     * Get the string matching the regular expression from the source paramater.
     *
     * @param source the string to search
     * @param regex the regular expression
     *
     * @return the string matching in the source parameter matching the given regular expression
     */
    public static String getMatchingString(String source, String regex) {
        Pattern pattern = Pattern.compile(regex);
        Matcher m = pattern.matcher(source);
        return m.find() ? m.group() : "";
    }

    public static boolean classAvailable(String className) {
        try {
            MCRClassTools.forName(className);
            LOGGER.debug("found class: {}", className);
            return true;
        } catch (ClassNotFoundException e) {
            LOGGER.debug("did not find class: {}", className);
            return false;
        }
    }

    public static boolean resourceAvailable(String resourceName) {
        URL resource = MCRResourceHelper.getResourceUrl(resourceName);
        if (resource == null) {
            LOGGER.debug("did not find resource: {}", resourceName);
            return false;
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("resource: {} found at {}", resourceName, resource.toString());
        }
        return true;
    }

    /**
     * Encodes the given url so that one can safely embed that string in a part
     * of an URI
     *
     * @return the encoded source
     */
    public static String encodeURL(String source, String encoding) {
        String result = null;
        try {
            result = URLEncoder.encode(source, encoding);
        } catch (UnsupportedEncodingException e) {
            LOGGER.error(e);
        }
        return result;
    }

    /**
     * Encodes the given URL so, that it is a valid RFC 2396 URL.
     */
    public static String normalizeAbsoluteURL(String url) throws MalformedURLException, URISyntaxException {
        try {
            return new URI(url).toASCIIString();
        } catch (Exception e) {
            return UriBuilder.fromUri(url)
                .build()
                .toString();
        }
    }

    /**
     * Encodes the path so that it can be safely used in an URI.
     * Same as calling {@link #encodeURIPath(String, boolean)} with boolean parameter set to false.
     * @return encoded path as described in RFC 2396
     */
    public static String encodeURIPath(String path) throws URISyntaxException {
        return encodeURIPath(path, false);
    }

    /**
     * Encodes the path so that it can be safely used in an URI.
     * @param asciiOnly
     *          if true, return only ASCII characters (e.g. encode umlauts)
     * @return encoded path as described in RFC 2396
     */
    public static String encodeURIPath(String path, boolean asciiOnly) throws URISyntaxException {
        URI relativeURI = new URI(null, null, path, null, null);
        return asciiOnly ? relativeURI.toASCIIString() : relativeURI.getRawPath();
    }

    /**
     * Decodes the path so that it can be displayed without encoded octets.
     * @param path
     *            encoded path as described in RFC 2396
     * @return decoded path
     */
    public static String decodeURIPath(String path) throws URISyntaxException {
        URI relativeURI = new URI(path);
        return relativeURI.getPath();
    }

    public static boolean isDisplayedEnabledDerivate(String derivateId) {
        return MCRAccessManager.checkDerivateDisplayPermission(derivateId);
    }

    /**
     * Checks if the given object has derivates that are all accessible to guest user.
     * <p>
     * Normally this implies that all derivates are readable by everyone. Only non-hidden
     * derivates are taken into account. So if an object only contains hidden
     * @param objId MCRObjectID as String
     * @see #isWorldReadable(String)
     */
    public static boolean isWorldReadableComplete(String objId) {
        LOGGER.info("World completely readable: {}", objId);
        if (!MCRObjectID.isValid(objId)) {
            return false;
        }
        MCRObjectID mcrObjectID = MCRObjectID.getInstance(objId);
        try {
            return MCRAccessManager.checkPermission(
                MCRSystemUserInformation.GUEST,
                () -> MCRAccessManager.checkPermission(mcrObjectID, MCRAccessManager.PERMISSION_READ)
                    && checkReadPermissionOfDerivates(mcrObjectID));
        } catch (RuntimeException e) {
            LOGGER.error("Error while retriving ACL information for Object {}", objId, e);
            return false;
        }
    }

    private static boolean checkReadPermissionOfDerivates(MCRObjectID mcrObjectID) {
        List<MCRObjectID> derivateIds = MCRMetadataManager.getDerivateIds(mcrObjectID, 0, TimeUnit.SECONDS);
        if (derivateIds.isEmpty()) {
            return MCRConfiguration2.getOrThrow("MCR.XMLFunctions.WorldReadableComplete.NoDerivates",
                Boolean::parseBoolean);
        }
        Set<String> displayableDerivates = derivateIds
            .stream()
            .map(MCRObjectID::toString)
            .filter(MCRXMLFunctions::isDisplayedEnabledDerivate)
            .collect(Collectors.toSet());
        return !displayableDerivates.isEmpty() && displayableDerivates.stream()
            .allMatch(derId -> MCRAccessManager.checkPermission(derId, MCRAccessManager.PERMISSION_READ));
    }

    /**
     * Checks if the given object is readable to guest user.
     * @param objId MCRObjectID as String
     */
    public static boolean isWorldReadable(String objId) {
        if (!MCRObjectID.isValid(objId)) {
            return false;
        }
        MCRObjectID mcrObjectID = MCRObjectID.getInstance(objId);
        try {
            return MCRAccessManager.checkPermission(
                MCRSystemUserInformation.GUEST,
                () -> MCRAccessManager.checkPermission(mcrObjectID, MCRAccessManager.PERMISSION_READ));
        } catch (RuntimeException e) {
            LOGGER.error("Error while retriving ACL information for Object {}", objId, e);
            return false;
        }
    }

    /**
     * @param objectId
     *            the id of the derivate owner
     * @return <code>true</code> if the derivate owner has a least one derivate
     *         with the display attribute set to true, <code>false</code>
     *         otherwise
     */
    public static boolean hasDisplayableDerivates(String objectId) {
        return Optional.of(MCRObjectID.getInstance(objectId))
            .filter(MCRMetadataManager::exists)
            .map(MCRMetadataManager::retrieveMCRObject)
            .map(MCRObject::getStructure)
            .map(MCRObjectStructure::getDerivates)
            .map(List::stream)
            .map(s -> s.map(MCRMetaLinkID::getXLinkHrefID)
                .map(MCRMetadataManager::retrieveMCRDerivate)
                .anyMatch(d -> MCRAccessManager.checkDerivateDisplayPermission(d.getId().toString())))
            .orElse(false);
    }

    /**
     * Returns a list of link targets of a given MCR object type. The structure
     * is <em>link</em>. If no links are found an empty NodeList is returned.
     *
     * @param mcrid
     *            MCRObjectID as String as the link source
     * @param destinationType
     *            MCR object type
     * @return a NodeList with <em>link</em> elements
     */
    public static NodeList getLinkDestinations(String mcrid, String destinationType) {
        DocumentBuilder documentBuilder = MCRDOMUtils.getDocumentBuilderUnchecked();
        try {
            Document document = documentBuilder.newDocument();
            Element rootElement = document.createElement("linklist");
            document.appendChild(rootElement);
            MCRLinkTableManager ltm = MCRLinkTableManager.getInstance();
            for (String id : ltm.getDestinationOf(mcrid, destinationType)) {
                Element link = document.createElement("link");
                link.setTextContent(id);
                rootElement.appendChild(link);
            }
            return rootElement.getChildNodes();
        } finally {
            MCRDOMUtils.releaseDocumentBuilder(documentBuilder);
        }
    }

    /**
     * Returns a list of link sources of a given MCR object type. The structure
     * is <em>link</em>. If no links are found an empty NodeList is returned.
     *
     * @param mcrid
     *            MCRObjectID as String as the link target
     * @param sourceType
     *            MCR object type
     * @return a NodeList with <em>link</em> elements
     */
    public static NodeList getLinkSources(String mcrid, String sourceType) {
        DocumentBuilder documentBuilder = MCRDOMUtils.getDocumentBuilderUnchecked();
        try {
            Document document = documentBuilder.newDocument();
            Element rootElement = document.createElement("linklist");
            document.appendChild(rootElement);
            MCRLinkTableManager ltm = MCRLinkTableManager.getInstance();
            for (String id : ltm.getSourceOf(mcrid)) {
                if (sourceType == null || MCRObjectID.getIDParts(id)[1].equals(sourceType)) {
                    Element link = document.createElement("link");
                    link.setTextContent(id);
                    rootElement.appendChild(link);
                }
            }
            return rootElement.getChildNodes();
        } finally {
            MCRDOMUtils.releaseDocumentBuilder(documentBuilder);
        }
    }

    /**
     * same as {@link #getLinkSources(String, String)} with
     * <code>sourceType</code>=<em>null</em>
     *
     */
    public static NodeList getLinkSources(String mcrid) {
        return getLinkSources(mcrid, null);
    }

    /**
     * Determines the mime type for the file given by its name.
     *
     * @param f
     *            the name of the file
     * @return the mime type of the given file
     */
    public static String getMimeType(String f) {
        if (f == null) {
            return "application/octet-stream";
        }

        if (mimetypeMap == null) {
            synchronized (MCRXMLFunctions.class) {
                if (mimetypeMap == null) {
                    mimetypeMap = new MimetypesFileTypeMap();
                }
            }
        }

        return mimetypeMap.getContentType(f.toLowerCase(Locale.ROOT));
    }

    /**
     * @return the name of the maindoc of the given derivate or null if maindoc is not set
     */
    public static String getMainDocName(String derivateId) {
        if (derivateId == null || derivateId.isEmpty()) {
            return null;
        }

        MCRObjectID objectID = MCRObjectID.getInstance(derivateId);
        if (!MCRMetadataManager.exists(objectID)) {
            return null;
        }

        return MCRMetadataManager.retrieveMCRDerivate(objectID).getDerivate().getInternals().getMainDoc();
    }

    /**
     * The method return a org.w3c.dom.NodeList as subpath of the doc input
     * NodeList selected by a path as String.
     *
     * @param doc
     *            the input org.w3c.dom.Nodelist
     * @param path
     *            the path of doc as String
     * @return a subpath of doc selected by path as org.w3c.dom.NodeList
     */
    public static NodeList getTreeByPath(NodeList doc, String path) {
        NodeList n = null;
        DocumentBuilder documentBuilder = MCRDOMUtils.getDocumentBuilderUnchecked();
        try {
            // build path selection
            XPathFactory factory = XPathFactory.newInstance();
            XPath xpath = factory.newXPath();
            XPathExpression expr = xpath.compile(path);
            // select part
            Document document = documentBuilder.newDocument();
            if (doc.item(0).getNodeName().equals("#document")) {
                // LOGGER.debug("NodeList is a document.");
                Node child = doc.item(0).getFirstChild();
                if (child != null) {
                    Node node = doc.item(0).getFirstChild();
                    Node imp = document.importNode(node, true);
                    document.appendChild(imp);
                } else {
                    document.appendChild(doc.item(0));
                }
            }
            n = (NodeList) expr.evaluate(document, XPathConstants.NODESET);
        } catch (Exception e) {
            LOGGER.error("Error while getting tree by path {}", path, e);
        } finally {
            MCRDOMUtils.releaseDocumentBuilder(documentBuilder);
        }
        return n;
    }

    /**
     * checks if the current user is in a specific role
     *
     * @param role
     *            a role name
     * @return true if user has this role
     */
    public static boolean isCurrentUserInRole(String role) {
        return MCRSessionMgr.getCurrentSession().getUserInformation().isUserInRole(role);
    }

    public static boolean isCurrentUserSuperUser() {
        return MCRSessionMgr.getCurrentSession().getUserInformation().getUserID()
            .equals(MCRSystemUserInformation.SUPER_USER.getUserID());
    }

    public static boolean isCurrentUserGuestUser() {
        return MCRSessionMgr.getCurrentSession().getUserInformation().getUserID()
            .equals(MCRSystemUserInformation.GUEST.getUserID());
    }

    public static String getCurrentUserAttribute(String attribute) {
        return MCRSessionMgr.getCurrentSession().getUserInformation().getUserAttribute(attribute);
    }

    public static boolean exists(String objectId) {
        return MCRMetadataManager.exists(MCRObjectID.getInstance(objectId));
    }

    /**
     * Verifies if object is in specified category.
     * @see MCRCategLinkService#isInCategory(MCRCategLinkReference, MCRCategoryID)
     * @param objectId valid MCRObjectID as String
     * @param categoryId valid MCRCategoryID as String
     * @return true if object is in category, else false
     */
    public static boolean isInCategory(String objectId, String categoryId) {
        try {
            MCRCategoryID categID = MCRCategoryID.ofString(categoryId);
            MCRObjectID mcrObjectID = MCRObjectID.getInstance(objectId);
            MCRCategLinkReference reference = new MCRCategLinkReference(mcrObjectID);
            return MCRCategLinkServiceHolder.INSTANCE.isInCategory(reference, categID);
        } catch (MCRException e) {
            LOGGER.error("Error while checking if object is in category", e);
            return false;
        }
    }

    /**
     * Checks if the User-Agent is sent from a mobile device
     * @return true if the User-Agent is sent from a mobile device
     */
    public static boolean isMobileDevice(String userAgent) {
        return userAgent.toLowerCase(Locale.ROOT).contains("mobile");
    }

    public static boolean hasParentCategory(String classificationId, String categoryId) {
        MCRCategoryID categID = new MCRCategoryID(classificationId, categoryId);
        //root category has level 0
        return !categID.isRootID() && MCRCategoryDAOFactory.obtainInstance().getCategory(categID, 0).getLevel() > 1;
    }

    public static String getDisplayName(String classificationId, String categoryId) {
        try {
            MCRCategoryID categID = new MCRCategoryID(classificationId, categoryId);
            MCRCategoryDAO dao = MCRCategoryDAOFactory.obtainInstance();
            MCRCategory category = dao.getCategory(categID, 0);
            return Optional.ofNullable(category)
                .map(MCRCategory::getCurrentLabel)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(MCRLabel::getText)
                .orElse("");
        } catch (MCRException e) {
            LOGGER.error("Could not determine display name for classification id {} and category id {}",
                classificationId, categoryId, e);
            return "";
        }
    }

    /**
     * Get the display name for the given category in the desired language.
     *
     * @param classificationId the id of the classification
     * @param categoryId the category id
     * @param lang the desired language for which to get the display name
     *
     * @return the display name in the desired language, if the label for the desired language cannot be found the
     *         method trys to deliver the default label. If this fails an empty string is returned.
     */
    public static String getDisplayName(String classificationId, String categoryId, String lang) {
        try {
            MCRCategoryID categID = new MCRCategoryID(classificationId, categoryId);
            MCRCategoryDAO dao = MCRCategoryDAOFactory.obtainInstance();
            MCRCategory category = dao.getCategory(categID, 0);
            Optional<MCRLabel> label = category.getLabel(lang);
            return label.isEmpty() ? getDisplayName(classificationId, categoryId)
                                   : label.get().getText();
        } catch (MCRException e) {
            LOGGER.error("Could not determine display name for classification id {} and category id {}",
                classificationId, categoryId, e);
            return "";
        }
    }

    public static boolean isCategoryID(String classificationId, String categoryId) {
        if (classificationId == null || classificationId.isEmpty()) {
            LOGGER.debug("Classification identifier is null or empty");
            return false;
        }

        if (categoryId == null || categoryId.isEmpty()) {
            LOGGER.debug("Category identifier is null or empty");
            return false;
        }

        if (classificationId.length() > MCRCategoryID.ROOT_ID_LENGTH) {
            LOGGER.debug("Could not determine state for classification id {} and category id {}", classificationId);
            return false;
        }

        MCRCategory category = null;
        try {
            MCRCategoryID categID = MCRCategoryID.ofString(classificationId + ":" + categoryId);
            MCRCategoryDAO dao = MCRCategoryDAOFactory.obtainInstance();
            category = dao.getCategory(categID, 0);
        } catch (MCRException e) {
            LOGGER.error("Could not determine state for classification id {} and category id {}", classificationId,
                categoryId, e);
        }

        return category != null;
    }

    /**
     * Method returns the amount of space consumed by the files contained in the
     * derivate container. The returned string is already formatted meaning it
     * has already the optimal measurement unit attached (e.g. 142 MB, ).
     *
     * @param derivateId
     *            the derivate id for which the size should be returned
     * @return the size as formatted string
     */
    public static String getSize(String derivateId) throws IOException {
        MCRPath rootPath = MCRPath.getPath(derivateId, "/");
        final AtomicLong size = new AtomicLong();
        Files.walkFileTree(rootPath, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                size.addAndGet(attrs.size());
                return super.visitFile(file, attrs);
            }

        });
        return MCRUtils.getSizeFormatted(size.get());
    }

    /**
     * Returns the number of files of the given derivate.
     *
     * @param derivateId the id of the derivate
     *
     * @return the number of files
     * */
    public static long getFileCount(String derivateId) throws IOException {
        AtomicInteger i = new AtomicInteger(0);
        Files.walkFileTree(MCRPath.getPath(derivateId, "/"), new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                i.incrementAndGet();
                return FileVisitResult.CONTINUE;
            }
        });
        return i.get();
    }

    /**
     * @param derivateID
     *            the derivate id
     * @return the id of the mycore object that contains the derivate with the
     *         given id
     */
    public static String getMCRObjectID(String derivateID) {
        return getMCRObjectID(derivateID, 5000);
    }

    /**
     * Same as {@link MCRMetadataManager#getObjectId(MCRObjectID, long, TimeUnit)} with String representation.
     */
    public static String getMCRObjectID(final String derivateID, final long expire) {
        return MCRMetadataManager.getObjectId(MCRObjectID.getInstance(derivateID), expire, TimeUnit.MILLISECONDS)
            .toString();
    }

    /**
     * @param uri the uri to resolve
     */
    public static NodeList resolve(String uri) throws JDOMException {
        org.jdom2.Element element = MCRURIResolver.obtainInstance().resolve(uri);
        element.detach();
        org.jdom2.Document document = new org.jdom2.Document(element);
        return new DOMOutputter().output(document).getDocumentElement().getChildNodes();
    }

    /**
     * Helper function for xslImport URI Resolver and {@link #hasNextImportStep(String)}
     * @param includePart substring after "xmlImport:"
     */
    public static String nextImportStep(String includePart) {
        int border = includePart.indexOf(':');
        String includePartSubString;
        String selfName = null;
        if (border > 0) {
            selfName = includePart.substring(border + 1);
            includePartSubString = includePart.substring(0, border);
        } else {
            includePartSubString = includePart;
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("get next import step for {}", includePartSubString);
        }
        // get the parameters from mycore.properties
        List<String> importList = MCRConfiguration2.getString("MCR.URIResolver.xslImports." + includePartSubString)
            .map(MCRConfiguration2::splitValue)
            .map(s -> s.collect(Collectors.toList()))
            .orElseGet(Collections::emptyList);
        if (importList.isEmpty()) {
            LOGGER.info("MCR.URIResolver.xslImports.{} has no Stylesheets defined", includePartSubString);
        } else {
            ListIterator<String> listIterator = importList.listIterator(importList.size());

            if (selfName == null && listIterator.hasPrevious()) {
                return listIterator.previous();
            }

            while (listIterator.hasPrevious()) {
                String currentStylesheet = listIterator.previous();
                if (currentStylesheet.equals(selfName)) {
                    if (listIterator.hasPrevious()) {
                        return listIterator.previous();
                    } else {
                        LOGGER.debug("xslImport reached end of chain:{}", importList);
                        return "";
                    }
                }
                //continue;
            }
            LOGGER.warn("xslImport could not find {} in {}", selfName, importList);
        }
        return "";
    }

    public static boolean hasNextImportStep(String uri) {
        boolean returns = !nextImportStep(uri).isEmpty();
        LOGGER.debug("hasNextImportStep('{}') -> {}", uri, returns);
        return returns;
    }

    public static String shortenText(String text, int length) {
        if (text.length() <= length) {
            return text;
        }
        int i = text.indexOf(' ', length);
        if (i < 0) {
            return text;
        }
        return text.substring(0, i) + "…";
    }

    public static String shortenPersonLabel(String text) {
        int pos = text.indexOf('(');
        if (pos == -1) {
            return text;
        }
        return text.substring(0, pos - 1);
    }

    /**
     * Return <code>true</code> if s contains HTML markup tags or entities.
     *
     * @param s string to test
     * @return true if string contains HTML
     */
    public static boolean isHtml(final String s) {
        boolean ret = false;
        if (s != null) {
            ret = HTML_MATCH_PATTERN.matcher(s).find();
        }
        return ret;
    }

    /**
     * Stripes HTML tags from string.
     *
     * @param s string to strip HTML tags of
     * @return the plain text without tags
     */
    public static String stripHtml(final String s) {
        StringBuilder res = new StringBuilder(s);
        for (Matcher m = TAG_PATTERN.matcher(res.toString()); m.find(); m = TAG_PATTERN.matcher(res.toString())) {
            res.delete(m.start(), m.end());
            res.insert(m.start(), stripHtml(m.group(m.groupCount() - 1)));
        }
        return StringEscapeUtils.unescapeHtml4(res.toString()).replaceAll(TAG_SELF_CLOSING, "");
    }

    /**
     * Returns approximately the usable space in the MCR.datadir in bytes as in {@link FileStore#getUsableSpace()}.
     *
     * @return approximately the usable space in the MCR.datadir
     * @throws IOException
     * */
    public static long getUsableSpace() throws IOException {
        Path dataDir = Paths.get(MCRConfiguration2.getStringOrThrow("MCR.datadir"));
        dataDir = dataDir.toRealPath();
        FileStore fs = Files.getFileStore(dataDir);

        return fs.getUsableSpace();
    }

    /**
     * Converts a string to valid NCName.
     *
     * @see <a href="https://www.w3.org/TR/1999/WD-xmlschema-2-19990924/#NCName">w3.org</a>
     *
     * @param name the string to convert
     * @return a string which is a valid NCName
     * @throws IllegalArgumentException if there is no way to convert the string to an NCName
     */
    public static String toNCName(String name) {
        String remainingName = name;
        while (remainingName.length() > 0 && !XMLChar.isNameStart(remainingName.charAt(0))) {
            remainingName = remainingName.substring(1);
        }
        String validNCName = toNCNameSecondPart(remainingName);
        if (validNCName.length() == 0) {
            throw new IllegalArgumentException("Unable to convert '" + validNCName + "' to valid NCName.");
        }
        return validNCName;
    }

    /**
     * Converts a string to a valid second part (everything after the first character) of a NCName. This includes
     * "a-Z A-Z 0-9 - . _".
     *
     * @see <a href="https://www.w3.org/TR/1999/WD-xmlschema-2-19990924/#NCName">w3.org</a>
     *
     * @param name the string to convert
     * @return a valid NCName
     */
    public static String toNCNameSecondPart(String name) {
        return name.replaceAll("[^\\w\\-\\.]*", "");
    }

    /**
     * This only works with text nodes
     * @return the order of nodes maybe changes
     */
    public static NodeList distinctValues(NodeList nodes) {
        SortedSet<Node> distinctNodeSet = new TreeSet<>((node, t1) -> {
            String nodeValue = node.getNodeValue();
            String nodeValue1 = t1.getNodeValue();
            return Objects.equals(nodeValue1, nodeValue) ? 0 : -1;
        });
        for (int i = 0; i < nodes.getLength(); i++) {
            distinctNodeSet.add(nodes.item(i));
        }

        return new SetNodeList(distinctNodeSet);
    }

    private static final class MCRCategLinkServiceHolder {
        public static final MCRCategLinkService INSTANCE = MCRCategLinkServiceFactory.obtainInstance();
    }

    private static final class SetNodeList implements NodeList {

        private final Object[] objects;

        SetNodeList(Set<Node> set) {
            objects = set.toArray();
        }

        @Override
        public Node item(int index) {
            return (Node) objects[index];
        }

        @Override
        public int getLength() {
            return objects.length;
        }
    }
}
