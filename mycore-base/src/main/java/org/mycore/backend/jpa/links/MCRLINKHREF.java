/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mycore.backend.jpa.links;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Transient;

/**
 * This class implement the data sructure of the MCRLinkHref table.
 *
 * @author Heiko Helmbrecht
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 */
@Entity
@Table(indexes = {
    @Index(name = "LinkFrom", columnList = "MCRFROM, MCRTYPE"),
    @Index(name = "LinkTo", columnList = "MCRTO, MCRTYPE"),
})
@NamedQueries({
    @NamedQuery(name = "MCRLINKHREF.getDestinations",
        query = "SELECT key.mcrto FROM MCRLINKHREF WHERE key.mcrfrom=:from"),
    @NamedQuery(name = "MCRLINKHREF.getDestinationsWithType",
        query = "SELECT key.mcrto FROM MCRLINKHREF WHERE key.mcrfrom=:from AND key.mcrtype=:type"),
    @NamedQuery(name = "MCRLINKHREF.getSources", query = "SELECT key.mcrfrom FROM MCRLINKHREF WHERE key.mcrto=:to"),
    @NamedQuery(name = "MCRLINKHREF.getSourcesWithType",
        query = "SELECT key.mcrfrom FROM MCRLINKHREF WHERE key.mcrto=:to AND key.mcrtype=:type"),
    @NamedQuery(name = "MCRLINKHREF.group",
        query = "SELECT count(key.mcrfrom), key.mcrto FROM MCRLINKHREF WHERE key.mcrto like :like GROUP BY key.mcrto")
})
public class MCRLINKHREF {
    private MCRLINKHREFPK key;

    private String attr;

    /**
     * The constructor of the class MCRLINKHREF
     */
    public MCRLINKHREF() {
        key = new MCRLINKHREFPK();
        attr = "";
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
        key = new MCRLINKHREFPK();
        key.setMcrfrom(from);
        key.setMcrto(to);
        key.setMcrtype(type);
        if (attr != null) {
            this.attr = attr;
        }
    }

    /**
     * This method returns the primary key.
     *
     * @return returns the primary key as class MCRLINKHREFPK.
     */
    @EmbeddedId
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
     */
    @Transient
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
     */
    @Transient
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
     */
    @Transient
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
     */
    @Basic
    @Column(length = 194, name = "MCRATTR")
    public String getMcrattr() {
        return attr;
    }

    /**
     * Set the attr value.
     *
     * @param mcrattr
     *            the attr value as a string
     */
    public void setMcrattr(String mcrattr) {
        if (mcrattr == null) {
            return;
        }
        attr = mcrattr;
    }
}
