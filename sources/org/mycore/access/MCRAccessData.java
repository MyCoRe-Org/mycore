package org.mycore.access;

import org.mycore.user.MCRUser;

import java.util.Date;

public class MCRAccessData
{
    private MCRUser user;
    private Date date;
    private MCRIPAddress ip;
    private String ruleid;
    private String objid;
    private String pool;

    MCRAccessData(MCRUser user, Date date, MCRIPAddress ip)
    {
        this.user = user;
        this.date = date;
        this.ip = ip;
    }
    
    public MCRAccessData(){
        
    }

    /**
     * date
     * @return
     */
    public Date getDate() {
        return date;
    }
    public void setDate(Date date) {
        this.date = date;
    }

    /**
     * ip
     * @return
     */
    public MCRIPAddress getIp() {
        return ip;
    }
    public void setIp(MCRIPAddress ip) {
        this.ip = ip;
    }

    /**
     * user
     * @return
     */
    public MCRUser getUser() {
        return user;
    }
    public void setUser(MCRUser user) {
        this.user = user;
    }

    /**
     * objid = MCRObjectID as string
     * @return
     */
    public String getObjId() {
        return objid;
    }
    public void setObjId(String objid) {
        this.objid = objid;
    }

    /**
     * pool
     * @return
     */
    public String getPool() {
        return pool;
    }
    public void setPool(String pool) {
        this.pool = pool;
    }

    /**
     * ruleid
     * @return
     */
    public String getRuleId() {
        return ruleid;
    }
    public void setRuleId(String ruleid) {
        this.ruleid = ruleid;
    }
}
