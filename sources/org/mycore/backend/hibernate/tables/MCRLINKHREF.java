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

/**
 * This class implement the data sructure of the MCRLinkHref table.
 * 
 * @author Heiko Helmbrecht
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 */
public class MCRLINKHREF {
    private MCRLINKHREFPK key;

    private String attr;

    /**
     * The constructor of the class MCRLINKHREF
     */
    public MCRLINKHREF() {
        this.key = new MCRLINKHREFPK();
        this.attr = "";
    }

    /**
     * The constructor of the class MCRLINKHREF
     * 
     * @param from
     *            The link source
     * @param to
     *            The link target
     * @param type
     *            The type of the link (defined by using of this class)
     * @param attr
     *            The optional attribute of the link (defined by using of this
     *            class)
     */
    public MCRLINKHREF(String from, String to, String type, String attr) {
        this.key = new MCRLINKHREFPK();
        key.setMcrfrom(from);
        key.setMcrto(to);
        key.setMcrtype(type);
        if (attr != null)
            this.attr = attr;
    }

    /**
     * This method returns the primary key.
     * 
     * @return returns the primary key as class MCRLINKHREFPK.
     * @hibernate.property column="Primary Key" not-null="true" update="true"
     */
    public MCRLINKHREFPK getKey() {
        return key;
    }

    /**
     * This method set the primary key.
     * 
     * @param key
     *            the primary key as instance of the class MCRLINKHREFPK
     */
    public void setKey(MCRLINKHREFPK key) {
        this.key = key;
    }

    /**
     * Get the from value.
     * 
     * @return the from value as a String.
     * @hibernate.property column="MCRFROM" not-null="true" update="true"
     */
    public String getMcrfrom() {
        return key.getMcrfrom();
    }

    /**
     * Set the from value.
     * 
     * @param mcrfrom
     *            the from value as a string
     */
    public void setMcrfrom(String mcrfrom) {
        key.setMcrfrom(mcrfrom);
    }

    /**
     * Get the to value.
     * 
     * @return the to value as a String.
     * @hibernate.property column="MCRTO" not-null="true" update="true"
     */
    public String getMcrto() {
        return key.getMcrto();
    }

    /**
     * Set the to value.
     * 
     * @param mcrto
     *            the to value as a string
     */
    public void setMcrto(String mcrto) {
        key.setMcrto(mcrto);
    }

    /**
     * Get the type value.
     * 
     * @return the type value as a String.
     * @hibernate.property column="MCRTYPE" not-null="true" update="true"
     */
    public String getMcrtype() {
        return key.getMcrtype();
    }

    /**
     * Set the type value.
     * 
     * @param mcrtype
     *            the type value as a string
     */
    public void setMcrtype(String mcrtype) {
        key.setMcrtype(mcrtype);
    }
    /**
     * Get the attribute value.
     * 
     * @return the attr value as a String.
     * @hibernate.property column="MCRATTR" not-null="true" update="true"
     */
    public String getMcrattr() {
        return this.attr;
    }

    /**
     * Set the attr value.
     * 
     * @param mcrattr
     *            the attr value as a string
     */
    public void setMcrattr(String mcrattr) {
        if (mcrattr == null) return;
        this.attr = mcrattr;
    }
}
