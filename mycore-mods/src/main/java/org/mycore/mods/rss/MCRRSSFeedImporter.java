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

package org.mycore.mods.rss;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.common.SolrDocumentList;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.filter.ElementFilter;
import org.mycore.access.MCRAccessException;
import org.mycore.common.MCRConstants;
import org.mycore.common.MCRException;
import org.mycore.common.MCRMailer;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.common.xml.MCRURIResolver;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.mods.MCRMODSWrapper;
import org.mycore.solr.MCRSolrCoreManager;
import org.mycore.solr.MCRSolrUtils;
import org.mycore.solr.auth.MCRSolrAuthenticationLevel;
import org.mycore.solr.auth.MCRSolrAuthenticationManager;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;

/**
 * Reads an RSS feed referencing new publications and imports those publications that are not stored yet.
 * <p>
 * Usage:
 *   MCRRSSFeedImporter.importFromFeed( [sourceSystemID], [targetProjectID] );
 *   where targetProjectID is the target project ID to import mods objects to, e.g. "mir".
 * <p>
 * Reads the RSS feed configured via
 * MCR.MODS.RSSFeedImporter.[sourceSystemID].FeedURL=[http(s) URL of remote RSS feed to read]
 * <p>
 * For each entry,
 * <p>
 * 1. Gets the link given in that entry (assuming it points to the publications) and
 * extracts the publication ID from the link, using a regular expression configured via
 * MCR.MODS.RSSFeedImporter.[sourceSystemID].Pattern2FindID=
 * <p>
 * 2. Queries the SOLR index to check if this publication isn't already stored. The field to query is
 * MCR.MODS.RSSFeedImporter.[sourceSystemID].Field2QueryID=[SOLR field name]
 * <p>
 * 3. Retrieves the publication metadata from the remote system and converts it to &lt;mycoreobject /&gt; XML.
 * MCR.MODS.RSSFeedImporter.[sourceSystemID].PublicationURI=xslStyle:...:http://...{0}...
 * where the remote publication ID will be replaced in Java message format syntax as {0}.
 * <p>
 * 4. Saves the publication in persistent store, with the given projectID and object type "mods".
 * <p>
 * When the total number of publications imported is &gt; 0 AND the property
 * MCR.MODS.RSSFeedImporter.[sourceSystemID].XSL2BuildNotificationMail=foo.xsl
 * is set, builds and sends a notification mail via MCRMailer.
 *
 * @author Frank Lützenkirchen
 */
public class MCRRSSFeedImporter {

    protected final MCRSolrAuthenticationManager solrAuthenticationManager;

    private String sourceSystemID;

    private String feedURL;

    private Pattern pattern2findID;

    private String field2queryID;

    private String importURI;

    private String xsl2BuildNotificationMail;

    private static final String STATUS_FLAG = "imported";

    private static final String PROPERTY_MAIL_ADDRESS = "MCR.Mail.Address";

    private static final Logger LOGGER = LogManager.getLogger();

    public static void importFromFeed(String sourceSystemID, String projectID) throws Exception {
        MCRRSSFeedImporter importer = new MCRRSSFeedImporter(sourceSystemID);
        importer.importPublications(projectID);
    }

    public MCRRSSFeedImporter(String sourceSystemID) {
        this.sourceSystemID = sourceSystemID;
        this.solrAuthenticationManager = MCRSolrAuthenticationManager.obtainInstance();

        String prefix = "MCR.MODS.RSSFeedImporter." + sourceSystemID + ".";

        feedURL = MCRConfiguration2.getStringOrThrow(prefix + "FeedURL");
        importURI = MCRConfiguration2.getStringOrThrow(prefix + "PublicationURI");
        field2queryID = MCRConfiguration2.getStringOrThrow(prefix + "Field2QueryID");
        xsl2BuildNotificationMail = MCRConfiguration2.getString(prefix + "XSL2BuildNotificationMail").orElse(null);

        pattern2findID = retrievePattern2FindID(prefix);
    }

    private Pattern retrievePattern2FindID(String prefix) {
        String patternProperty = prefix + "Pattern2FindID";
        try {
            String pattern = MCRConfiguration2.getStringOrThrow(patternProperty);
            return Pattern.compile(pattern);
        } catch (PatternSyntaxException ex) {
            String msg = "Regular expression syntax error: " + patternProperty;
            throw new MCRConfigurationException(msg, ex);
        }
    }

    public void importPublications(String projectID) throws Exception {
        LOGGER.info("Getting new publications from {} RSS feed...", sourceSystemID);
        SyndFeed feed = retrieveFeed();

        List<MCRObject> importedObjects = new ArrayList<>();
        for (SyndEntry entry : feed.getEntries()) {
            MCRObject importedObject = handleFeedEntry(entry, projectID);
            if (importedObject != null) {
                importedObjects.add(importedObject);
            }
        }

        int numPublicationsImported = importedObjects.size();
        LOGGER.info("imported {} publications.", numPublicationsImported);

        if ((numPublicationsImported > 0) && (xsl2BuildNotificationMail != null)) {
            sendNotificationMail(importedObjects);
        }
    }

    private SyndFeed retrieveFeed() throws IOException, FeedException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(feedURL)).build();
        HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        try (InputStream resIS = response.body(); XmlReader feedReader = new XmlReader(resIS)) {
            SyndFeedInput input = new SyndFeedInput();
            return input.build(feedReader);
        }
    }

    private MCRObject handleFeedEntry(SyndEntry entry, String projectID)
        throws MCRPersistenceException, MCRAccessException {
        String publicationID = getPublicationID(entry);
        if (publicationID == null) {
            return null;
        }

        if (isAlreadyStored(publicationID)) {
            LOGGER.info("publication with ID {} already existing, will not import.", publicationID);
            return null;
        }

        LOGGER.info("publication with ID {} does not exist yet, retrieving data...", publicationID);
        Element publicationXML = retrieveAndConvertPublication(publicationID);
        if (shouldIgnore(publicationXML)) {
            LOGGER.info("publication will be ignored, do not store.");
            return null;
        }

        MCRObject obj = buildMCRObject(publicationXML, projectID);
        MCRMetadataManager.create(obj);
        return obj;
    }

    private String getPublicationID(SyndEntry entry) {
        String link = entry.getLink();
        if (link == null) {
            LOGGER.warn("no link found in feed entry");
            return null;
        }
        link = link.trim();
        Matcher m = pattern2findID.matcher(link);
        if (m.matches()) {
            return m.group(1);
        } else {
            LOGGER.warn("no publication ID found in link {}", link);
            return null;
        }
    }

    private boolean isAlreadyStored(String publicationID) {
        SolrClient solrClient = MCRSolrCoreManager.getMainSolrClient();
        SolrQuery query = new SolrQuery();
        query.setQuery(field2queryID + ":" + MCRSolrUtils.escapeSearchValue(publicationID));
        query.setRows(0);
        SolrDocumentList results;
        try {
            QueryRequest queryRequest = new QueryRequest(query);
            solrAuthenticationManager.applyAuthentication(queryRequest,
                MCRSolrAuthenticationLevel.SEARCH);
            results = queryRequest.process(solrClient).getResults();

            return (results.getNumFound() > 0);
        } catch (Exception ex) {
            throw new MCRException(ex);
        }
    }

    private Element retrieveAndConvertPublication(String externalID) {
        String uri = new MessageFormat(importURI, Locale.ROOT).format(new String[] { externalID });
        return MCRURIResolver.obtainInstance().resolve(uri);
    }

    /** If mods:genre was not mapped by conversion/import function, ignore this publication and do not import */
    private static boolean shouldIgnore(Element publication) {
        return !publication.getDescendants(new ElementFilter("genre", MCRConstants.MODS_NAMESPACE)).hasNext();
    }

    private MCRObject buildMCRObject(Element publicationXML, String projectID) {
        MCRObject obj = new MCRObject(new Document(publicationXML));
        MCRMODSWrapper wrapper = new MCRMODSWrapper(obj);
        wrapper.setServiceFlag("status", STATUS_FLAG);
        MCRObjectID oid = MCRMetadataManager.getMCRObjectIDGenerator().getNextFreeId(projectID, "mods");
        obj.setId(oid);
        return obj;
    }

    private void sendNotificationMail(List<MCRObject> importedObjects) throws Exception {
        Element xml = new Element(STATUS_FLAG).setAttribute("source", this.sourceSystemID);
        for (MCRObject obj : importedObjects) {
            xml.addContent(obj.createXML().detachRootElement());
        }

        Map<String, String> parameters = new HashMap<>();
        parameters.put(PROPERTY_MAIL_ADDRESS, MCRConfiguration2.getStringOrThrow(PROPERTY_MAIL_ADDRESS));
        MCRMailer.sendMail(new Document(xml), xsl2BuildNotificationMail, parameters);
    }
}
