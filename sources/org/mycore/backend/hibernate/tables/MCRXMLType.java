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

import java.sql.Blob;

public class MCRXMLType{
    
    private MCRXMLTypePK key;
    private String mcrtype;
    private Blob mcrxml;
    
    /**
    * @hibernate.property
    * column="Primary Key"
    * not-null="true"
    * update="true"
    */
    public MCRXMLTypePK getKey() {
        return key;
    }
    public void setKey(MCRXMLTypePK key) {
        this.key = key;
    }

    /**
    * @hibernate.property
    * column="MCRID"
    * not-null="true"
    * update="true"
    */
    public String getMcrid() {
        return key.getMcrid();
    }
    public void setMcrid(String mcrid) {
        key.setMcrid(mcrid);
    }
    
    /**
    * @hibernate.property
    * column="MCRTYPE"
    * not-null="true"
    * update="true"
    */
    public String getMcrtype() {
        return mcrtype;
    }
    public void setMcrtype(String mcrtype) {
        this.mcrtype = mcrtype;
    }
    
    /**
    * @hibernate.property
    * column="MCRVERSION"
    * not-null="true"
    * update="true"
    */
    public int getMcrversion() {
        return key.getMcrversion();
    }
    public void setMcrversion(int mcrversion) {
        key.setMcrversion(mcrversion);
    }

    /**
    * @hibernate.property
    * column="MCRXML"
    * not-null="true"
    * update="true"
    */
    public Blob getMcrxml() {
        return mcrxml;
    }
    public void setMcrxml(Blob mcrxml) {
        this.mcrxml = mcrxml;
    }
 
}
