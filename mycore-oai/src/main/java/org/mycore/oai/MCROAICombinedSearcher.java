package org.mycore.oai;

import java.time.Instant;
import java.time.ZonedDateTime;

import org.mycore.oai.pmh.Identify.DeletedRecordPolicy;
import org.mycore.oai.pmh.MetadataFormat;
import org.mycore.oai.pmh.Set;

/**
 * <p>Combines the solr searcher and the deleted searcher. Ignores the
 * deleted searcher if the DeletedRecordPolicy is set to 'No'.</p>
 * 
 * <p>The deleted records appear after the solr records.</p>
 * 
 * @author Matthias Eichner
 */
public class MCROAICombinedSearcher extends MCROAISearcher {

    private MCROAISolrSearcher solrSearcher;

    private MCROAIDeletedSearcher deletedSearcher;

    private Integer numHits;

    @Override
    public void init(MCROAIIdentify identify, MetadataFormat format, long expire, int partitionSize,
        MCROAISetManager setManager) {
        super.init(identify, format, expire, partitionSize, setManager);
        this.solrSearcher = new MCROAISolrSearcher();
        this.solrSearcher.init(identify, format, expire, partitionSize, setManager);
        if (!identify.getDeletedRecordPolicy().equals(DeletedRecordPolicy.No)) {
            this.deletedSearcher = new MCROAIDeletedSearcher();
            this.deletedSearcher.init(identify, format, expire, partitionSize, setManager);
        }
    }

    @Override
    public MCROAIResult query(String cursor) {
        if (this.deletedSearcher == null) {
            return this.solrSearcher.query(cursor);
        }
        // deleted query, no need to ask solr
        if (isDeletedCursor(cursor)) {
            MCROAISimpleResult result = this.deletedSearcher.query(cursor);
            result.setNumHits(this.numHits);
            return result;
        }
        return getMixedResult(this.solrSearcher.query(cursor));
    }

    @Override
    public MCROAIResult query(Set set, ZonedDateTime from, ZonedDateTime until) {
        MCROAIResult solrResult = this.solrSearcher.query(set, from, until);
        if (this.deletedSearcher == null) {
            this.numHits = solrResult.getNumHits();
            return solrResult;
        }
        MCROAIResult deletedResult = this.deletedSearcher.query(set, from, until);
        this.numHits = solrResult.getNumHits() + deletedResult.getNumHits();
        return getMixedResult(solrResult);
    }

    @Override
    public Instant getEarliestTimestamp() {
        Instant solrTimestamp = this.solrSearcher.getEarliestTimestamp();
        if (this.deletedSearcher != null) {
            Instant deletedTimestamp = this.deletedSearcher.getEarliestTimestamp();
            if (deletedTimestamp == null) {
                return solrTimestamp;
            }
            return solrTimestamp.isBefore(deletedTimestamp) ? solrTimestamp : deletedTimestamp;
        }
        return solrTimestamp;
    }

    private boolean isDeletedCursor(String cursor) {
        return cursor != null && cursor.startsWith(MCROAIDeletedSearcher.CURSOR_PREFIX);
    }

    private MCROAIResult getMixedResult(MCROAIResult solrResult) {
        MCROAISimpleResult result = new MCROAISimpleResult();
        result.setNumHits(this.numHits);
        // solr query - not at the end
        if (solrResult.nextCursor() != null) {
            result.setNextCursor(solrResult.nextCursor());
            result.setIdList(solrResult.list());
            return result;
        }
        // solr is at the end of the list, mix with deleted
        result.setIdList(solrResult.list());
        int delta = this.getPartitionSize() - solrResult.list().size();
        if (delta > 0) {
            String deletedCursor = this.deletedSearcher.buildCursor(0, delta);
            MCROAIResult deletedResult = this.deletedSearcher.query(deletedCursor);
            result.list().addAll(deletedResult.list());
            result.setNextCursor(deletedResult.nextCursor());
        } else if (this.deletedSearcher.getDeletedRecords().size() > 0) {
            result.setNextCursor(this.deletedSearcher.buildCursor(0, this.getPartitionSize()));
        }
        return result;
    }

}
