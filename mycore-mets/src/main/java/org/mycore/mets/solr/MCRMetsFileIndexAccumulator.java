package org.mycore.mets.solr;

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

import static org.mycore.common.MCRConstants.XPATH_FACTORY;

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
                    MCRConstants.METS_NAMESPACE).evaluate(mets);

            // collect all label attributes
            for (Attribute a : attributeList) {
                solrInputDocument.addField(
                    MCRConfiguration2.getString("MCR.Solr.Mets.Label.Field").orElse("mets_label"),
                    a.getValue());
            }

        } catch (IOException | JDOMException e) {
            LOGGER.error("Could not process {}", path.toString(), e);
        }
    }
}
