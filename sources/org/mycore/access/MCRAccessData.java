package org.mycore.access;

import org.mycore.user.MCRUser;

import java.util.Date;

public class MCRAccessData
{
    public MCRUser user;
    public Date date;
    public MCRIPAddress ip;

    MCRAccessData(MCRUser user, Date date, MCRIPAddress ip)
    {
        this.user = user;
        this.date = date;
        this.ip = ip;
    }
}
