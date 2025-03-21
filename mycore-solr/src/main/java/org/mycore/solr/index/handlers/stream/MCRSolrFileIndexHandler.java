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

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.ContentStreamUpdateRequest;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.SolrInputField;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.mycore.common.MCRUtils;
import org.mycore.solr.MCRSolrCoreType;
import org.mycore.solr.auth.MCRSolrAuthenticationLevel;
import org.mycore.solr.index.cs.MCRSolrPathContentStream;
import org.mycore.solr.index.file.MCRSolrPathDocumentFactory;
import org.mycore.solr.index.statistic.MCRSolrIndexStatistic;
import org.mycore.solr.index.statistic.MCRSolrIndexStatisticCollector;

public class MCRSolrFileIndexHandler extends MCRSolrAbstractStreamIndexHandler {

    private static final Logger LOGGER = LogManager.getLogger();

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
                updateRequest.setParams(solrParams);
                updateRequest.setCommitWithin(getCommitWithin());

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Solr: sending binary data ({} ({}), size is {}) to solr server.", file, solrID,
                        MCRUtils.getSizeFormatted(attrs.size()));
                }
                long t = System.currentTimeMillis();
                /* actually send the request */

                client.request(updateRequest);

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

    @Override
    public MCRSolrIndexStatistic getStatistic() {
        return MCRSolrIndexStatisticCollector.FILE_TRANSFER;
    }

    @Override
    public String toString() {
        return "index " + this.file;
    }

}
