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

public class MCRCSTORE {
    private String storageid;

    private Blob content;

    public MCRCSTORE() {
    }

    public MCRCSTORE(String storageid, byte[] content) {
        setStorageid(storageid);
        setContentBytes(content);
    }

    public String getStorageid() {
        return storageid;
    }

    public void setStorageid(String storageid) {
        this.storageid = storageid;
    }

    public Blob getContent() {
        return content;
    }

    public byte[] getContentBytes() {
        return MCRBlob.getBytes(content);
    }

    public void setContent(Blob content) {
        this.content = content;
    }

    public void setContentBytes(byte[] content) {
        this.content = new MCRBlob(content);
    }
}
