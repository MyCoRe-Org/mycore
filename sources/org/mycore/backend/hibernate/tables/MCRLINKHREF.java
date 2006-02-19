/*
 * $RCSfile$
 * $Revision$ $Date$
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
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
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */

package org.mycore.backend.hibernate.tables;

public class MCRLINKHREF {
    private MCRLINKHREFPK key;

    public MCRLINKHREF() {
        this.key = new MCRLINKHREFPK();
    }

    public MCRLINKHREF(String from, String to, String type) {
        this.key = new MCRLINKHREFPK();
        key.setMcrfrom(from);
        key.setMcrto(to);
        key.setMcrtype(type);
    }

    /**
     * @hibernate.property column="Primary Key" not-null="true" update="true"
     */
    public MCRLINKHREFPK getKey() {
        return key;
    }

    public void setKey(MCRLINKHREFPK key) {
        this.key = key;
    }

    /**
     * @hibernate.property column="MCRFROM" not-null="true" update="true"
     */
    public String getMcrfrom() {
        return key.getMcrfrom();
    }

    public void setMcrfrom(String mcrfrom) {
        key.setMcrfrom(mcrfrom);
    }

    /**
     * @hibernate.property column="MCRTO" not-null="true" update="true"
     */
    public String getMcrto() {
        return key.getMcrto();
    }

    public void setMcrto(String mcrto) {
        key.setMcrto(mcrto);
    }
    
    /**
     * @hibernate.property column="MCRTYPE" not-null="true" update="true"
     */
    public String getMcrtype() {
        return key.getMcrtype();
    }

    public void setMcrtype(String mcrtype) {
        key.setMcrtype(mcrtype);
    }
}
