package org.mycore.viewer.alto.model;

public class MCRAltoChangePID {

    public MCRAltoChangePID(String pid) {
        this.pid = pid;
    }

    public MCRAltoChangePID() {
    }

    private String pid;

    public String getPid() {
        return pid;
    }

    public void setPid(String pid) {
        this.pid = pid;
    }
}
