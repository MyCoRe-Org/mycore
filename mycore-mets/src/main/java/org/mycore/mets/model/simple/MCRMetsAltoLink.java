package org.mycore.mets.model.simple;

public class MCRMetsAltoLink {

    public MCRMetsAltoLink(MCRMetsFile file, String begin, String end) {
        this.file = file;
        this.begin = begin;
        this.end = end;
    }

    private MCRMetsFile file;

    private String begin;

    private String end;

    public MCRMetsFile getFile() {
        return file;
    }

    public void setFile(MCRMetsFile file) {
        this.file = file;
    }

    public String getBegin() {
        return begin;
    }

    public void setBegin(String begin) {
        this.begin = begin;
    }

    public String getEnd() {
        return end;
    }

    public void setEnd(String end) {
        this.end = end;
    }
}
