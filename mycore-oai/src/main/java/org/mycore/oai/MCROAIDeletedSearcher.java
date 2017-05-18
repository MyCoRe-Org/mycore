package org.mycore.oai;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.mycore.backend.jpa.deleteditems.MCRDeletedItemManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.oai.pmh.Identify.DeletedRecordPolicy;
import org.mycore.oai.pmh.Set;

/**
 * Searcher for deleted records. The schema for the cursor
 * is <b>deleted_from_rows</b>.
 * 
 * @author Matthias Eichner
 */
public class MCROAIDeletedSearcher extends MCROAISearcher {

    public static final String CURSOR_PREFIX = "deleted";

    public static final String CURSOR_DELIMETER = "_";

    private List<String> deletedRecords;

    @Override
    public MCROAISimpleResult query(String cursor) {
        this.updateRunningExpirationTimer();
        int from = 0;
        int rows = this.getPartitionSize();
        if (cursor != null) {
            try {
                String[] parts = cursor.substring((CURSOR_PREFIX + CURSOR_DELIMETER).length()).split(CURSOR_DELIMETER);
                from = Integer.valueOf(parts[0]);
                rows = Integer.valueOf(parts[1]);
            } catch (Exception exc) {
                throw new IllegalArgumentException("Invalid cursor " + cursor, exc);
            }
        }
        int numHits = this.deletedRecords.size();
        int nextFrom = from + rows;
        String nextCursor = nextFrom < numHits ? buildCursor(nextFrom, this.getPartitionSize()) : null;
        MCROAISimpleResult result = new MCROAISimpleResult();
        result.setNumHits(numHits);
        result.setNextCursor(nextCursor);
        int to = Math.min(nextFrom, numHits);
        for (int i = from; i < to; i++) {
            result.list().add(this.deletedRecords.get(i));
        }
        return result;
    }

    @Override
    public MCROAIResult query(Set set, ZonedDateTime from, ZonedDateTime until) {
        this.deletedRecords = this.searchDeleted(from, until);
        return this.query(null);
    }

    @Override
    public Instant getEarliestTimestamp() {
        return MCRDeletedItemManager.getFirstDate().map(ZonedDateTime::toInstant).orElse(null);
    }

    public List<String> getDeletedRecords() {
        return deletedRecords;
    }

    /**
     * Builds the cursor in the form of <b>deleted_from_rows</b>.
     * 
     * @param from where to start
     * @param rows how many rows
     * @return the cursor as string
     */
    public String buildCursor(int from, int rows) {
        StringBuilder b = new StringBuilder(CURSOR_PREFIX);
        b.append(CURSOR_DELIMETER);
        b.append(from);
        b.append(CURSOR_DELIMETER);
        b.append(rows);
        return b.toString();
    }

    /**
     * Returns a list with identifiers of the deleted objects within the given date boundary.
     * If the record policy indicates that there is no support for tracking deleted an empty
     * list is returned.
     * 
     * @param from from date
     * @param until to date
     * 
     * @return a list with identifiers of the deleted objects
     */
    protected List<String> searchDeleted(ZonedDateTime from, ZonedDateTime until) {
        DeletedRecordPolicy deletedRecordPolicy = this.identify.getDeletedRecordPolicy();
        if (from == null || DeletedRecordPolicy.No.equals(deletedRecordPolicy)
            || DeletedRecordPolicy.Transient.equals(deletedRecordPolicy)) {
            return new ArrayList<>();
        }
        LOGGER.info("Getting identifiers of deleted items");
        List<String> deletedItems = MCRDeletedItemManager.getDeletedItems(from, Optional.ofNullable(until));
        List<String> types = getConfig().getStrings(getConfigPrefix() + "DeletedRecordTypes", null);
        if (types == null || types.isEmpty()) {
            return deletedItems;
        }
        return deletedItems.stream()
                           .map(MCRObjectID::getInstance)
                           .filter(id -> {
                               return types.contains(id.getTypeId());
                           })
                           .map(MCRObjectID::toString)
                           .collect(Collectors.toList());
    }

}
