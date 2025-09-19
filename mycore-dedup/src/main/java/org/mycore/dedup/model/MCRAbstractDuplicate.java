package org.mycore.dedup.model;

public class MCRAbstractDuplicate {

    protected String mcrId1;
    protected String mcrId2;

    public MCRAbstractDuplicate() {
    }

    public MCRAbstractDuplicate(String mcrId1, String mcrId2) {
        this.mcrId1 = mcrId1;
        this.mcrId2 = mcrId2;
    }

    public String getMcrId1() {
        return mcrId1;
    }

    public void setMcrId1(String mcrId1) {
        this.mcrId1 = mcrId1;
    }

    public String getMcrId2() {
        return mcrId2;
    }

    public void setMcrId2(String mcrId2) {
        this.mcrId2 = mcrId2;
    }
}
