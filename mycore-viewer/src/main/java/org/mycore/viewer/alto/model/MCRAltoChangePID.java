package org.mycore.viewer.alto.model;


public class MCRAltoChangePID {

    public MCRAltoChangePID(int pid) {
        this.pid = pid;
    }

    public MCRAltoChangePID() {
    }

    private int pid;

    public int getPid() {
        return pid;
    }

    public void setPid(int pid) {
        this.pid = pid;
    }
}
