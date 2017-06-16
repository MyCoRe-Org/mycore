package org.mycore.datamodel.metadata.history;

public enum MCRMetadataHistoryEventType {

    Create('c'), Delete('d');

    private char abbr;

    MCRMetadataHistoryEventType(char abbr) {
        this.abbr = abbr;
    }

    public static MCRMetadataHistoryEventType fromAbbr(char abbr) {
        switch (abbr) {
            case 'c':
                return MCRMetadataHistoryEventType.Create;
            case 'd':
                return MCRMetadataHistoryEventType.Delete;
            default:
                throw new IllegalArgumentException(
                    "No such " + MCRMetadataHistoryEventType.class.getSimpleName() + ": " + abbr);
        }
    }

    protected char getAbbr() {
        return abbr;
    }

}
