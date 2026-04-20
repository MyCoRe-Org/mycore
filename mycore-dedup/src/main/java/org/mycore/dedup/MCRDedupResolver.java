package org.mycore.dedup;

import java.io.IOException;


import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;

import org.apache.solr.client.solrj.SolrServerException;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.transform.JDOMSource;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.dedup.jpa.MCRDeduplicationKeyManager;


public class MCRDedupResolver implements URIResolver {

    private static final String PREFIX = "dedupResolver:";
    private final MCRSolrDeduplicationService dedupService = new MCRSolrDeduplicationService();

    @Override
    public Source resolve(String href, String base) throws TransformerException {
        if (!isValidHref(href)) {
            return null;
        }
        String id = extractId(href);
        String query = extractQuery(href);
        return performDeduplication(id, query);
    }

    private boolean isValidHref(String href) {
        return href != null && href.startsWith(PREFIX);
    }

    private String extractId(String href) throws TransformerException {
        int start = PREFIX.length();
        int braceStart = href.indexOf('{', start);
        if (braceStart < 0) {
            throw new TransformerException("Missing opening brace in href: " + href);
        }
        return href.substring(start, braceStart).replace(":", "");
    }

    private String extractQuery(String href) throws TransformerException {
        int braceStart = href.indexOf('{', PREFIX.length());
        int braceEnd = href.lastIndexOf('}');
        if (braceStart < 0 || braceEnd < 0 || braceEnd <= braceStart) {
            throw new TransformerException("Invalid query braces in href: " + href);
        }
        return href.substring(braceStart + 1, braceEnd);
    }


    private Source performDeduplication(String id, String query) {
        try {
            List<Document> duplicates = dedupService.checkForDuplicates(query, id);
            MCRDeDupCriteriaBuilder criteriaBuilder = new MCRDeDupCriteriaBuilder();
            MCRDeDupService deDupService = new MCRDeDupService(criteriaBuilder);
            Map<MCRDeDupCriterion, Set<String>> result = deDupService.calculateDeduplication(duplicates, id);
            deDupService.writeDeduplicationKeys(result, id);
            Element root = new Element("duplicates");
            if (duplicates == null || duplicates.isEmpty()) {
                Element message = new Element("message");
                message.setText("No duplicates found");
                root.addContent(message);
            } else {
                for (Document doc : duplicates) {
                    Element duplicate = new Element("duplicate");
                    duplicate.addContent(doc.getRootElement().clone());
                    root.addContent(duplicate);
                }
            }

            MCRDeduplicationKeyManager.getInstance()
                    .updateDeDupCriteria(root, MCRObjectID.getInstance(id), criteriaBuilder);
            Document jdomDoc = new Document(root);
            return new JDOMSource(jdomDoc);

        } catch (SolrServerException | IOException e) {
            Element error = new Element("error");
            error.setText("Deduplication error occurred: " + e.getMessage());
            Document errorDoc = new Document(error);
            return new JDOMSource(errorDoc);
        }
    }


}

