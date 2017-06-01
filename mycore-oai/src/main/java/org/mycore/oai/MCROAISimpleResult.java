package org.mycore.oai;

import java.util.ArrayList;
import java.util.List;

import org.mycore.oai.pmh.Header;

/**
 * Simple implementation of a {@link MCROAIResult} with setter
 * and getter methods.
 * 
 * @author Matthias Eichner
 */
public class MCROAISimpleResult implements MCROAIResult {

    private List<Header> headerList;

    private int numHits;

    private String nextCursor;

    public MCROAISimpleResult() {
        this.headerList = new ArrayList<>();
        this.numHits = 0;
        this.nextCursor = null;
    }

    @Override
    public List<Header> list() {
        return this.headerList;
    }

    @Override
    public int getNumHits() {
        return this.numHits;
    }

    @Override
    public String nextCursor() {
        return this.nextCursor;
    }

    public MCROAISimpleResult setNextCursor(String nextCursor) {
        this.nextCursor = nextCursor;
        return this;
    }

    public MCROAISimpleResult setNumHits(int numHits) {
        this.numHits = numHits;
        return this;
    }

    public MCROAISimpleResult setHeaderList(List<Header> headerList) {
        this.headerList = headerList;
        return this;
    }

    public static MCROAISimpleResult from(MCROAIResult result) {
        MCROAISimpleResult newResult = new MCROAISimpleResult();
        newResult.setHeaderList(result.list());
        newResult.setNextCursor(result.nextCursor());
        newResult.setNumHits(result.getNumHits());
        return newResult;
    }

}
