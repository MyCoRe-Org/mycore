/*
 * This file is part of ** M y C o R e **
 * Visit our homepage at http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, normally in the file license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */

package org.mycore.backend.hibernate.tables;

import java.io.Serializable;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

public class MCRUSERSPK implements Serializable{
    
    private int numid;
    private String uid;
    
    public MCRUSERSPK(){
        
    }
    
    public MCRUSERSPK(int numid, String uid){
        this.numid = numid;
        this.uid = uid;
    }

    /**
     * @return Returns the numid.
     */
    public int getNumid() {
        return numid;
    }
    /**
     * @param numid The numid to set.
     */
    public void setNumid(int numid) {
        this.numid = numid;
    }
    /**
     * @return Returns the uid.
     */
    public String getUid() {
        return uid;
    }
    /**
     * @param uid The uid to set.
     */
    public void setUid(String uid) {
        this.uid = uid;
    }
    
    public boolean equals(Object other){
        if (! (other instanceof MCRUSERSPK)) return false;
        MCRUSERSPK castother =  (MCRUSERSPK) other;
        	return new EqualsBuilder()
        	.append(this.getNumid(), castother.getNumid())
        	.append(this.getUid(), castother.getUid())
        	.isEquals();	
    }
    
    public int hashCode(){
        return new HashCodeBuilder()
        .append(getNumid())
        .append(getUid())
        .toHashCode();
    }
}
