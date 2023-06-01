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

package org.mycore.mets.solr;

import static org.mycore.common.MCRConstants.XPATH_FACTORY;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.common.SolrInputDocument;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.mycore.common.MCRConstants;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.solr.index.file.MCRSolrFileIndexAccumulator;

/**
 * Class indexes label attributes in mets files.
 *
 * By default, the labels are indexed in solr field <code>mets_label</code>. Configure your own by providing a proper
 * name in property <code>MCR.Solr.Mets.Label.Field</code>.
 *
 * @author shermann (Silvio Hermann)
 * */
public class MCRMetsFileIndexAccumulator implements MCRSolrFileIndexAccumulator {
    protected static Logger LOGGER = LogManager.getLogger(MCRMetsFileIndexAccumulator.class);

    @Override
    public void accumulate(SolrInputDocument solrInputDocument, Path path, BasicFileAttributes basicFileAttributes)
        throws IOException {
        if (!MCRConfiguration2.getString("MCR.Mets.Filename").get().equals(path.getFileName().toString())) {
            return;
        }

        try (InputStream is = Files.newInputStream(path)) {
            Document mets = new SAXBuilder().build(is);

            List<Attribute> attributeList = XPATH_FACTORY
                .compile("/mets:mets/mets:structMap[@TYPE='LOGICAL']//*/@LABEL", Filters.attribute(), null,
                    MCRConstants.METS_NAMESPACE)
                .evaluate(mets);

            // collect all label attributes
            for (Attribute a : attributeList) {
                solrInputDocument.addField(
                    MCRConfiguration2.getString("MCR.Solr.Mets.Label.Field").orElse("mets_label"),
                    a.getValue());
            }

        } catch (JDOMException e) {
            throw new IOException(e);
        }
    }
}
