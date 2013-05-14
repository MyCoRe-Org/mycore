/**
 * 
 */
package org.mycore.solr.legacy;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.mycore.services.fieldquery.MCRFieldValue;
import org.mycore.services.fieldquery.MCRHit;
import org.mycore.services.fieldquery.MCRResults;

/**
 * @author shermann
 *
 */
public class MCRSolrResults extends MCRResults {

    private static Logger LOGGER = Logger.getLogger(MCRSolrResults.class);

    private SolrDocumentList sdl;

    /**
     * @param sdl
     * @param sortBy
     */
    public MCRSolrResults(SolrDocumentList sdl) {
        this.sdl = sdl;
    }

    @Override
    public MCRHit getHit(int i) {
        if (i >= sdl.size() || i < 0) {
            return null;
        }
        SolrDocument solrDocument = sdl.get(i);
        MCRHit hit = new MCRHit(solrDocument.get("id").toString());
        hit.addMetaData(new MCRFieldValue("score", solrDocument.get("score").toString()));
        Collection<String> fieldNames = solrDocument.getFieldNames();

        for (String fieldName : fieldNames) {
            try {
                hit.addMetaData(new MCRFieldValue(fieldName, solrDocument.getFieldValue(fieldName).toString()));
            } catch (Exception ex) {
                LOGGER.debug(ex + " Could not add hit metadata. Solr field is not defined in legacy searchfields.xml");
            }
        }
        return hit;
    }

    @Override
    public int getNumHits() {
        LOGGER
            .debug("getNumHits() might be inaccurate because of explicit cast from long to int. Better you fully migrate to solr soon.");
        return (int) this.sdl.getNumFound();
    }

    @Override
    public Iterator<MCRHit> iterator() {
        List<MCRHit> hitList = new Vector<MCRHit>(getNumHits());
        for (int i = 0; i < sdl.getNumFound(); i++) {
            hitList.add(getHit(i));
        }

        return hitList.iterator();
    }

    @Override
    public boolean isReadonly() {
        return true;
    }

    @Override
    public boolean isSorted() {
        return true;
    }

    public String toString() {
        return this.getClass().getSimpleName() + "(" + sdl.toString() + ")";
    }
}
