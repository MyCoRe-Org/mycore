/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
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

import java.io.IOException;
import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.mycore.common.content.MCRContent;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.solr.index.cs.MCRSolrAbstractContentStream;
import org.mycore.solr.index.cs.MCRSolrContentStream;

/**
 * Index one mycore object with a MCRSolrContentStream.
 *
 * @author Matthias Eichner
 */
public class MCRSolrSingleObjectStreamIndexHandler extends MCRSolrObjectStreamIndexHandler {

    private static final Logger LOGGER = LogManager.getLogger(MCRSolrSingleObjectStreamIndexHandler.class);

    protected MCRObjectID id;

    protected MCRContent content;

    public MCRSolrSingleObjectStreamIndexHandler(MCRObjectID id, MCRContent content) {
        this(id, content, null);
    }

    public MCRSolrSingleObjectStreamIndexHandler(MCRObjectID id, MCRContent content, SolrClient solrClient) {
        super(solrClient);
        this.id = Objects.requireNonNull(id);
        this.content = Objects.requireNonNull(content);
    }

    @Override
    protected MCRSolrAbstractContentStream<?> getStream() {
        return new MCRSolrContentStream(id.toString(), content);
    }

    /**
     * Invokes an index request for the current content stream.
     */
    public void index() throws IOException, SolrServerException {
        if (!MCRMetadataManager.exists(id)) {
            LOGGER.warn("Unable to index '{}' cause it doesn't exists anymore!", id);
            return;
        }
        super.index();
    }

    @Override
    public String toString() {
        return "index " + id;
    }

}
