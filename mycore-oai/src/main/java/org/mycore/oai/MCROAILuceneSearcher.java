package org.mycore.oai;

import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.mycore.datamodel.common.MCRISO8601Date;
import org.mycore.datamodel.metadata.MCRBase;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.metadata.MCRObjectService;
import org.mycore.oai.pmh.Set;
import org.mycore.parsers.bool.MCRAndCondition;
import org.mycore.parsers.bool.MCRCondition;
import org.mycore.parsers.bool.MCROrCondition;
import org.mycore.services.fieldquery.MCRHit;
import org.mycore.services.fieldquery.MCRQuery;
import org.mycore.services.fieldquery.MCRQueryCondition;
import org.mycore.services.fieldquery.MCRQueryManager;
import org.mycore.services.fieldquery.MCRResults;
import org.mycore.services.fieldquery.MCRSortBy;

public class MCROAILuceneSearcher extends MCROAISearcher {
    protected final static Logger LOGGER = Logger.getLogger(MCROAISolrSearcher.class);

    protected MCROAIResult result;

    @Override
    public MCROAIResult query(int cursor) {
        return this.result;
    }

    @Override
    public MCROAIResult query(Set set, Date from, Date until) {
        this.result = luceneQuery(set, from, until);
        return this.result;
    }

    protected MCROAIResult luceneQuery(Set set, Date from, Date until) {
        MCRAndCondition queryCondition = new MCRAndCondition();
        // restriction
        MCRCondition restriction = buildRestrictionCondition();
        if (restriction != null) {
            queryCondition.addChild(restriction);
        }
        if (set != null) {
            queryCondition.addChild(buildSetCondition(set));
        }
        // from & until
        if (from != null) {
            queryCondition.addChild(buildFromCondition(from));
        }
        if (until != null) {
            queryCondition.addChild(buildUntilCondition(until));
        }
        // build query
        MCRQuery query = new MCRQuery(queryCondition);
        // sort
        List<MCRSortBy> sortBy = buildSortByList();
        query.setSortBy(sortBy);
        MCRResults queryResults = MCRQueryManager.search(query);
        // deleted records
        List<String> deletedItems = searchDeleted(from, until);
        MCRResults deletedResults = new MCRResults();
        for (String item : deletedItems) {
            deletedResults.addHit(new MCRHit(item));
        }
        MCROAICombinedResult oaiResults = new MCROAICombinedResult();
        oaiResults.getResults().add(queryResults);
        oaiResults.getResults().add(deletedResults);
        return new MCROAILuceneResult(oaiResults);
    }

    protected MCRCondition buildRestrictionCondition() {
        return MCROAIUtils.getDefaultRestriction(this.configPrefix);
    }

    protected MCRCondition buildSetCondition(Set set) {
        return MCROAIUtils.getDefaultSetCondition(set.getSpec(), this.configPrefix);
    }

    protected MCRCondition buildFromCondition(Date from) {
        return buildFromUntilCondition(from, ">=");
    }

    protected MCRCondition buildUntilCondition(Date until) {
        return buildFromUntilCondition(until, "<=");
    }

    private MCRCondition buildFromUntilCondition(Date date, String compareSign) {
        String fieldFromUntil = getConfig().getString(this.configPrefix + "Search.FromUntil", "modified");
        String[] fields = fieldFromUntil.split(" *, *");
        MCRISO8601Date mcrDate = new MCRISO8601Date();
        mcrDate.setDate(date);
        MCROrCondition orCond = new MCROrCondition();
        for (String fDef : fields) {
            orCond.addChild(new MCRQueryCondition(fDef, compareSign, mcrDate.getISOString()));
        }
        return orCond;
    }

    protected List<MCRSortBy> buildSortByList() {
        return MCROAIUtils.getSortByList(this.configPrefix + "Search.SortBy", "modified descending, id descending");
    }

    @Override
    public Date getEarliestTimestamp() {
        List<MCRSortBy> sortByList = MCROAIUtils.getSortByList(this.configPrefix + "EarliestDatestamp.SortBy", "modified ascending");
        MCRCondition condition = MCROAIUtils.getDefaultRestriction(this.configPrefix);
        MCRQuery q = new MCRQuery(condition, sortByList, 1);
        MCRResults result = MCRQueryManager.search(q);
        if (result.getNumHits() > 0) {
            MCRBase obj = MCRMetadataManager.retrieve(MCRObjectID.getInstance(result.getHit(0).getID()));
            return obj.getService().getDate(MCRObjectService.DATE_TYPE_MODIFYDATE);
        }
        return null;
    }

}
