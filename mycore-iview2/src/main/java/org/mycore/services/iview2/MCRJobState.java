package org.mycore.services.iview2;

public enum MCRJobState {
	NEW ('n'),
    PROCESS ('p'),
    FIN ('f');

    private char status;
    
    MCRJobState(char status) {
        this.status = status;
    }
    
    public char toChar() {
		return status;
    }
}