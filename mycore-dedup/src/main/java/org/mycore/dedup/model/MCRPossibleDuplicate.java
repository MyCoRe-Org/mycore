package org.mycore.dedup.model;

public class MCRPossibleDuplicate extends MCRAbstractDuplicate {

    private String deduplicationType;
    private String deduplicationKey;

    public MCRPossibleDuplicate() {
    }

    public MCRPossibleDuplicate(String mcrId1, String mcrId2, String deduplicationType, String deduplicationKey) {
        super(mcrId1, mcrId2);
        this.deduplicationType = deduplicationType;
        this.deduplicationKey = deduplicationKey;
    }

    public String getDeduplicationType() {
        return deduplicationType;
    }

    public void setDeduplicationType(String deduplicationType) {
        this.deduplicationType = deduplicationType;
    }

    public String getDeduplicationKey() {
        return deduplicationKey;
    }

    public void setDeduplicationKey(String deduplicationKey) {
        this.deduplicationKey = deduplicationKey;
    }

    @Override
    public String toString() {
        return "MCRPossibleDuplicate{" +
                "mcrId1='" + mcrId1 + '\'' +
                ", mcrId2='" + mcrId2 + '\'' +
                ", deduplicationType='" + deduplicationType + '\'' +
                ", deduplicationKey='" + deduplicationKey + '\'' +
                '}';
    }
}
