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
 * This class implement the data sructure of the MCRURN table.
 * 
 * @author Heiko Helmbrecht
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 */
public class MCRURN {
    private MCRURNPK key;

    /**
     * The constructor of the class MCRURN
     */
    public MCRURN() {
        this.key = new MCRURNPK();
    }

    /**
     * The constructor of the class MCRURN
     * 
     * @param id
     *            The document ID
     * @param urn
     *            The URN
     */
    public MCRURN(String id, String urn) {
        this.key = new MCRURNPK();
        key.setMcrid(id);
        key.setMcrurn(urn);
    }

    /**
     * This method returns the primary key.
     * 
     * @return returns the primary key as class MCRURNPK.
     * @hibernate.property column="Primary Key" not-null="true" update="true"
     */
    public MCRURNPK getKey() {
        return key;
    }

    /**
     * This method set the primary key.
     * 
     * @param key
     *            the primary key as instance of the class MCRURNPK
     */
    public void setKey(MCRURNPK key) {
        this.key = key;
    }

    /**
     * Get the object ID value.
     * 
     * @return the object ID value as a String.
     * @hibernate.property column="MCRID" not-null="true" update="true"
     */
    public String getId() {
        return key.getMcrid();
    }

    /**
     * Set the object ID value.
     * 
     * @param id
     *            the object ID value as a string
     */
    public void setId(String id) {
        key.setMcrid(id);
    }

    /**
     * Get the URN value.
     * 
     * @return the URN value as a String.
     * @hibernate.property column="MCRURN" not-null="true" update="true"
     */
    public String getURN() {
        return key.getMcrurn();
    }

    /**
     * Set the URN value.
     * 
     * @param urn
     *            the urn value as a string
     */
    public void setURN(String urn) {
        key.setMcrurn(urn);
    }

}
