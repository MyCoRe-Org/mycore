package org.mycore.oai;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;

import org.mycore.oai.pmh.MetadataFormat;
import org.mycore.services.fieldquery.MCRHit;
import org.mycore.services.fieldquery.MCRResults;

/**
 * OAI Results container.
 * 
 * @author Matthias Eichner
 */
public class MCROAIResults implements Iterable<MCRHit> {

    protected Date expirationDate;

    protected MetadataFormat metadataFormat;

    protected List<MCRResults> results;

    /** The unique ID of this result set */
    protected String id;
    
    private static Random random = new Random(System.currentTimeMillis());
    
    public MCROAIResults(Date expirationDate, MetadataFormat format) {
        this.expirationDate = expirationDate;
        this.metadataFormat = format;
        this.results = new ArrayList<MCRResults>();
        this.id = Long.toString(random.nextLong(), 36) + Long.toString(System.currentTimeMillis(), 36);
    }

    boolean isExpired() {
        return (new Date().compareTo(expirationDate) > 0);
    }

    public MetadataFormat getMetadataFormat() {
        return metadataFormat;
    }

    public Date getExpirationDate() {
        return expirationDate;
    }

    public List<MCRResults> getResults() {
        return results;
    }

    public String getId() {
        return id;
    }

    @Override
    public Iterator<MCRHit> iterator() {
        return new MCROAIResultsIterator();
    }

    /**
     * Gets a single MCRHit.
     * 
     * @param i
     *            the position of the hit.
     * @return the hit at this position
     * @throws IndexOutOfBoundsException if the index is out of range (index < 0 || index >= size())
     */
    public MCRHit getHit(int i) {
        if(i < 0) {
            throw new IndexOutOfBoundsException("index is < 0");
        }
        int internalCursor = 0;
        for(MCRResults r : results) {
            int numHits = r.getNumHits();
            internalCursor += numHits;
            if(i < internalCursor) {
                return r.getHit((i + numHits) - internalCursor);
            }
        }
        throw new IndexOutOfBoundsException(MessageFormat.format("index {0} is too big({1})", i, internalCursor));
    }

    public int size() {
        int size = 0;
        for(MCRResults r : results) {
            size += r.getNumHits();
        }
        return size;
    }

    private class MCROAIResultsIterator implements Iterator<MCRHit> {

        private int cursor;

        public MCROAIResultsIterator() {
            this.cursor = 0;
        }

        @Override
        public boolean hasNext() {
            return this.cursor < size();
        }

        @Override
        public MCRHit next() {
            return getHit(this.cursor++);
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

    }

}
