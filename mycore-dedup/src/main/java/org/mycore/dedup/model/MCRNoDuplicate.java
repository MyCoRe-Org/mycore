package org.mycore.dedup.model;

import java.util.Date;

public class MCRNoDuplicate extends MCRAbstractDuplicate {

    private Integer id;
    private String creator;
    private Date date;

    public MCRNoDuplicate() {
    }

    public MCRNoDuplicate(Integer id, String mcrId1, String mcrId2, String creator, Date date) {
        super(mcrId1, mcrId2);
        this.id = id;
        this.creator = creator;
        this.date = date;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }
}
