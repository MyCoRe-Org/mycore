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

package org.mycore.solr.index.handlers.stream;

import static org.mycore.solr.MCRSolrConstants.SOLR_EXTRACT_PATH;
import static org.mycore.solr.MCRSolrConstants.SOLR_UPDATE_PATH;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.commons.text.StringSubstitutor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.ContentStreamUpdateRequest;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.SolrInputField;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.util.ContentStreamBase;
import org.mycore.common.MCRUtils;
import org.mycore.solr.MCRSolrCoreType;
import org.mycore.solr.auth.MCRSolrAuthenticationLevel;
import org.mycore.solr.index.cs.MCRSolrPathContentStream;
import org.mycore.solr.index.file.MCRSolrPathDocumentFactory;
import org.mycore.solr.index.statistic.MCRSolrIndexStatistic;
import org.mycore.solr.index.statistic.MCRSolrIndexStatisticCollector;

public class MCRSolrFileIndexHandler extends MCRSolrAbstractStreamIndexHandler {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final String TMPL_XML_FIELD = "\n  <field name=\"${name}\">${value}</field>";
    private static final String TMPL_XML_UPDATE_FIELD = "\n  <field name=\"${name}\" update=\"set\">${value}</field>";

    protected Path file;

    protected BasicFileAttributes attrs;

    public MCRSolrFileIndexHandler(Path file, BasicFileAttributes attrs) {
        this.file = file;
        this.attrs = attrs;
        this.setCoreType(MCRSolrCoreType.MAIN);
    }

    @Override
    public MCRSolrPathContentStream getStream() {
        return new MCRSolrPathContentStream(file, attrs);
    }

    @Override
    public void index() throws SolrServerException, IOException {
        String solrID = file.toUri().toString();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Solr: indexing file \"{}\"", file);
        }
        /* create the update request object */
        ContentStreamUpdateRequest updateRequest = new ContentStreamUpdateRequest(SOLR_EXTRACT_PATH);
        getSolrAuthenticationFactory().applyAuthentication(updateRequest,
            MCRSolrAuthenticationLevel.INDEX);
        for (SolrClient client : getClients()) {
            try (MCRSolrPathContentStream stream = getStream()) {
                updateRequest.addContentStream(stream);

                /* set the additional parameters */
                ModifiableSolrParams solrParams = getSolrParams(file, attrs);

                // collect Alto specific parameters and remove those from the given solrParams
                // then send them as atomic update doc with a 2nd request if applicable
                List<Entry<String, String[]>> altoParams = extractAltoParams(solrParams);

                updateRequest.setParams(solrParams);
                updateRequest.setCommitWithin(getCommitWithin());

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Solr: sending binary data ({} ({}), size is {}) to solr server.", file, solrID,
                        MCRUtils.getSizeFormatted(attrs.size()));
                    LOGGER.debug("Solr Update-URL-Parameter {}", updateRequest.getParams().toQueryString());
                }
                long t = System.currentTimeMillis();
                /* actually send the request */
                updateRequest.process(client);

                if (!altoParams.isEmpty()) {
                    String altoSolrDocXml = buildSolrAtomicUpdateDocXML(solrID, altoParams);
                    ContentStreamUpdateRequest updateAltoReq = new ContentStreamUpdateRequest(SOLR_UPDATE_PATH);
                    updateAltoReq.addContentStream(
                        new ContentStreamBase.StringStream(altoSolrDocXml, "application/xml"));
                    updateAltoReq.setCommitWithin(getCommitWithin());
                    updateAltoReq.process(client);
                }

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Solr: sending binary data \"{} ({})\" done in {}ms", file, solrID,
                        System.currentTimeMillis() - t);
                }
            } //MCR-1911: close any open resource
        }
    }

    private ModifiableSolrParams getSolrParams(Path file, BasicFileAttributes attrs) {
        ModifiableSolrParams params = new ModifiableSolrParams();
        SolrInputDocument doc = MCRSolrPathDocumentFactory.obtainInstance().getDocument(file, attrs);
        for (SolrInputField field : doc) {
            String name = "literal." + field.getName();
            if (field.getValueCount() > 1) {
                String[] values = getValues(field.getValues());
                params.set(name, values);
            } else {
                params.set(name, field.getValue().toString());
            }
        }
        return params;
    }

    private String[] getValues(Collection<Object> values) {
        List<String> strValues = new ArrayList<>(values.size());
        for (Object o : values) {
            strValues.add(o.toString());
        }
        return strValues.toArray(String[]::new);
    }

    /**
     * This method extracts Alto word and content parameter into a new list of key-value entries
     * and removes them from the given ModifiableSolrParams object. 
     * e.g. &amp;literal.alto_words=alles%7C781%7C1086%7C148%7C32
     *      &amp;literal.alto_content=alles+wird+gut
     * 
     * @param solrParams the SolrParameterMap
     * @return Optional, containing the generated Solr update document as XML String
     */
    private List<Entry<String, String[]>> extractAltoParams(ModifiableSolrParams solrParams) {
        List<Entry<String, String[]>> altoParams = new ArrayList<>();
        Iterator<Entry<String, String[]>> it = solrParams.iterator();
        while (it.hasNext()) {
            Entry<String, String[]> param = it.next();
            if (param.getKey().startsWith("literal.alto")) {
                altoParams.add(Map.entry(param.getKey(), param.getValue()));
                it.remove();
            }
        }
        return altoParams;
    }

    /**
     * This method creates a Solr update document as atomic update for the given list of fields.
     * Each field (other than 'id') has an update operation attribute. 
     * This means, that the fields are added to an existing Solr object 
     * otherwise the document would be replaced and old fields are lost.
     */
    private String buildSolrAtomicUpdateDocXML(String docId, List<Entry<String, String[]>> fields) {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        xml.append("\n<add>");
        xml.append("\n <doc>");
        xml.append(StringSubstitutor.replace(TMPL_XML_FIELD, Map.of("name", "id", "value", docId)));
        for (Entry<String, String[]> entry : fields) {
            for (String v : entry.getValue()) {
                xml.append(StringSubstitutor.replace(TMPL_XML_UPDATE_FIELD, Map.of(
                    "name", StringUtils.removeStart(entry.getKey(), "literal."),
                    "value", StringEscapeUtils.escapeXml10(v))));
            }
        }
        xml.append("\n </doc>");
        xml.append("\n</add>\n");

        return xml.toString();
    }

    @Override
    public MCRSolrIndexStatistic getStatistic() {
        return MCRSolrIndexStatisticCollector.FILE_TRANSFER;
    }

    @Override
    public String toString() {
        return "index " + this.file;
    }

}
