package org.mycore.dedup;
import org.jdom2.Element;
import org.mycore.dedup.jpa.MCRDeduplicationKeyManager;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.jdom2.Document;



public class MCRDeDupService {

    private final MCRDeDupCriteriaBuilder builder;
    private final MCRDeduplicationKeyManager keyManager;

    public MCRDeDupService(MCRDeDupCriteriaBuilder builder) {
        this.builder = builder;
        this.keyManager =MCRDeduplicationKeyManager.getInstance() ;
    }

    public Map<MCRDeDupCriterion, Set<String>> calculateDeduplication(List<Document> documents, String selfID) {
        if (documents == null || documents.isEmpty() || selfID == null) {
            return Collections.emptyMap();
        }

        Map<String, Set<MCRDeDupCriterion>> allCriteria = buildCriteriaPerDocument(documents);
        Optional<Element> reference = findReferenceElement(documents, selfID);
        if (reference.isEmpty()) {
            return Collections.emptyMap();
        }

        Set<MCRDeDupCriterion> referenceCriteria = builder.extractCriteriaFromDocument(reference.get().getDocument());
        return findMatchingCriteria(referenceCriteria, allCriteria);
    }

    public void writeDeduplicationKeys(Map<MCRDeDupCriterion, Set<String>> matchMap, String selfID) {
        matchMap.forEach((criterion, docIds) -> {
            if (!docIds.isEmpty()) {
                docIds.add(selfID);
                docIds.forEach(docID ->
                        keyManager.addDeduplicationKeyIfNotExists(docID, criterion.getType(), criterion.getKey()));
            }
        });
    }

    private Map<String, Set<MCRDeDupCriterion>> buildCriteriaPerDocument(List<Document> documents) {
        return documents.stream()
                .collect(Collectors.toMap(
                        doc -> doc.getRootElement().getAttributeValue("ID"),
                        builder::extractCriteriaFromDocument
                ));
    }

    private Optional<Element> findReferenceElement(List<Document> documents, String selfID) {
        return documents.stream()
                .map(Document::getRootElement)
                .filter(el -> !selfID.equals(el.getAttributeValue("ID")))
                .findFirst();
    }

    public Map<MCRDeDupCriterion, Set<String>> findMatchingCriteria(
            Set<MCRDeDupCriterion> referenceCriteria,
            Map<String, Set<MCRDeDupCriterion>> allCriteria) {

        Map<MCRDeDupCriterion, Set<String>> matchMap = new HashMap<>();
        for (MCRDeDupCriterion refCriterion : referenceCriteria) {
            Set<String> matchingIds = allCriteria.entrySet().stream()
                    .filter(entry -> entry.getValue().contains(refCriterion))
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toSet());

            if (!matchingIds.isEmpty()) {
                refCriterion.markAsUsedInMatch();
            }
            matchMap.put(refCriterion, matchingIds);
        }
        return matchMap;
    }
}

