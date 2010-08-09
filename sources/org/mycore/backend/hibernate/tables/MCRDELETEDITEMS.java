/**
 * 
 */
package org.mycore.backend.hibernate.tables;

import java.io.Serializable;
import java.util.Date;

/**
 * @author shermann
 * @version $Revision: 13085 $ $Date: 2008-02-06 18:27:24 +0100 (Wed, 06 Feb 2008) $
 */
public class MCRDELETEDITEMS implements Serializable {

    private static final long serialVersionUID = 206480527372771428L;

    private MCRDELETEDITEMSPK key;

    private String userid, ip;

    /**
     * @return the userid
     */
    public String getUserid() {
        return userid;
    }

    /**
     * @param userid the userid to set
     */
    public void setUserid(String userid) {
        this.userid = userid;
    }

    /**
     * @return the ip
     */
    public String getIp() {
        return ip;
    }

    /**
     * @param ip the ip to set
     */
    public void setIp(String ip) {
        this.ip = ip;
    }

    public MCRDELETEDITEMS() {
        key = null;
    }

    public MCRDELETEDITEMS(String identifier, Date dateDeleted) {
        this.key = new MCRDELETEDITEMSPK(identifier, dateDeleted);
    }

    /**
     * @return the key
     */
    public MCRDELETEDITEMSPK getKey() {
        return key;
    }

    /**
     * @param key the key to set
     */
    public void setKey(MCRDELETEDITEMSPK key) {
        this.key = key;
    }
}
