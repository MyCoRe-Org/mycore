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

public class MCRXMLTypePK implements Serializable{
    private String mcrid;
    private int mcrversion; 
    
    /**
     * @return Returns the id.
     */
    public String getMcrid() {
        return mcrid;
    }
    /**
     * @param id The id to set.
     */
    public void setMcrid(String mcrid) {
        this.mcrid = mcrid;
    }
    /**
     * @return Returns the version.
     */
    public int getMcrversion() {
        return mcrversion;
    }
    /**
     * @param version The version to set.
     */
    public void setMcrversion(int mcrversion) {
        this.mcrversion = mcrversion;
    }
    
    public boolean equals(Object other){
        if (! (other instanceof MCRXMLTypePK)) return false;
        MCRXMLTypePK castother =  (MCRXMLTypePK) other;
        	return new EqualsBuilder()
        	.append(this.getMcrversion(), castother.getMcrversion())
        	.append(this.getMcrid(), castother.getMcrid())
        	.isEquals();
        	
    }
    
    public int hashCode(){
        return new HashCodeBuilder()
        .append(getMcrversion())
        .append(getMcrid())
        .toHashCode();
    }
}
