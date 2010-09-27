package org.mycore.iview2.services;

public enum MCRJobState {
    NEW('n'), PROCESS('p'), FIN('f');

    private char status;

    MCRJobState(char status) {
        this.status = status;
    }

    public char toChar() {
        return status;
    }
}