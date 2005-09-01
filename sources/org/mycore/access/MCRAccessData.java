package org.mycore.access;

import org.mycore.user.MCRUser;

import java.util.Date;

public class MCRAccessData
{
    private MCRUser user;
    private Date date;
    private MCRIPAddress ip;

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
}
