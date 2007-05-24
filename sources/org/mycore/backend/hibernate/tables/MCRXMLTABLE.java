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

import java.sql.Blob;

public class MCRXMLTABLE {
    private MCRXMLTABLEPK key;
    
    private String type;

    private Blob xml;

    public MCRXMLTABLE() {
        this.key = new MCRXMLTABLEPK();
    }

    public MCRXMLTABLE(String id, int version, String type, Blob xml) {
        this.key = new MCRXMLTABLEPK(id, version);
        this.type = type;
        this.xml = xml;
    }

    public MCRXMLTABLEPK getKey() {
        return key;
    }

    public void setKey(MCRXMLTABLEPK key) {
        this.key = key;
    }

    public String getId() {
        return this.key.getId();
    }

    public void setId(String id) {
        this.key.setId(id);
    }

    public int getVersion() {
        return this.key.getVersion();
    }

    public void setVersion(int version) {
        this.key.setVersion(version);
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public byte[] getXmlByteArray() {
        return MCRBlob.getBytes(this.xml);
    }

    public Blob getXml() {
        return xml;
    }

    public void setXml(Blob xml) {
        this.xml = xml;
    }

    public void setXmlByteArray(byte[] xml) {
        this.xml = new MCRBlob(xml);
    }
}
