package org.mycore.mets.model.simple;

public enum MCRMetsFileUse {
    ALTO("ALTO"), TRANSLATION("TRANSLATION"), TRANSCRIPTION("TRANSCRIPTION"), MASTER("MASTER"), DEFAULT("DEFAULT");

    MCRMetsFileUse(final String use) {
        this.use = use;
    }

    private String use;

    @Override
    public String toString() {
        return this.use;
    }
}
