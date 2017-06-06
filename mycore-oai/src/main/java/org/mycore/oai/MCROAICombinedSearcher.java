package org.mycore.oai;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Optional;

import org.mycore.oai.pmh.Header;
import org.mycore.oai.pmh.Identify.DeletedRecordPolicy;
import org.mycore.oai.pmh.MetadataFormat;
import org.mycore.oai.set.MCRSet;

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

    private Optional<MCROAIDeletedSearcher> deletedSearcher = Optional.empty();

    private Integer numHits;

    @Override
    public void init(MCROAIIdentify identify, MetadataFormat format, long expire, int partitionSize,
        MCROAISetManager setManager, MCROAIObjectManager objectManager) {
        super.init(identify, format, expire, partitionSize, setManager, objectManager);
        this.solrSearcher = new MCROAISolrSearcher();
        this.solrSearcher.init(identify, format, expire, partitionSize, setManager, objectManager);
        if (!identify.getDeletedRecordPolicy().equals(DeletedRecordPolicy.No)) {
            this.deletedSearcher = Optional.of(new MCROAIDeletedSearcher());
            this.deletedSearcher.get().init(identify, format, expire, partitionSize, setManager, objectManager);
        }
    }

    @Override
    public Optional<Header> getHeader(String mcrId) {
        Optional<Header> header = this.solrSearcher.getHeader(mcrId);
        return header.isPresent() ? header : deletedSearcher.flatMap(oais -> oais.getHeader(mcrId));
    }

    @Override
    public MCROAIResult query(String cursor) {
        if (!this.deletedSearcher.isPresent()) {
            return this.solrSearcher.query(cursor);
        }
        // deleted query, no need to ask solr
        if (isDeletedCursor(cursor)) {
            MCROAISimpleResult result = this.deletedSearcher.get().query(cursor);
            result.setNumHits(this.numHits);
            return result;
        }
        return getMixedResult(this.solrSearcher.query(cursor));
    }

    @Override
    public MCROAIResult query(MCRSet set, ZonedDateTime from, ZonedDateTime until) {
        MCROAIResult solrResult = this.solrSearcher.query(set, from, until);
        if (!this.deletedSearcher.isPresent()) {
            this.numHits = solrResult.getNumHits();
            return solrResult;
        }
        MCROAIResult deletedResult = this.deletedSearcher.get().query(set, from, until);
        this.numHits = solrResult.getNumHits() + deletedResult.getNumHits();
        return getMixedResult(solrResult);
    }

    @Override
    public Instant getEarliestTimestamp() {
        Instant solrTimestamp = this.solrSearcher.getEarliestTimestamp();
        return deletedSearcher
            .map(MCROAISearcher::getEarliestTimestamp)
            .filter(solrTimestamp::isAfter)
            .orElse(solrTimestamp);
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
            result.setHeaderList(solrResult.list());
            return result;
        }
        // solr is at the end of the list, mix with deleted
        result.setHeaderList(solrResult.list());
        int delta = this.getPartitionSize() - solrResult.list().size();
        MCROAIDeletedSearcher delSearcher = this.deletedSearcher.get();
        if (delta > 0) {
            String deletedCursor = delSearcher.buildCursor(0, delta);
            MCROAIResult deletedResult = delSearcher.query(deletedCursor);
            result.list().addAll(deletedResult.list());
            result.setNextCursor(deletedResult.nextCursor());
        } else if (delSearcher.getDeletedRecords().size() > 0) {
            result.setNextCursor(delSearcher.buildCursor(0, this.getPartitionSize()));
        }
        return result;
    }

}
