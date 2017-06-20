package org.mycore.datamodel.ifs2;

import java.util.Date;

import org.mycore.datamodel.common.MCRObjectIDDate;

public class MCRObjectIDDateImpl implements MCRObjectIDDate {

    protected Date lastModified;

    protected String id;

    protected MCRObjectIDDateImpl() {
        super();
    }

    public MCRObjectIDDateImpl(Date lastModified, String id) {
        super();
        this.lastModified = lastModified;
        this.id = id;
    }

    public Date getLastModified() {
        return lastModified;
    }

    public String getId() {
        return id;
    }

    protected void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    protected void setId(String id) {
        this.id = id;
    }

}
