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
import java.util.Date;

public class MCRNBNS {
    private String niss;

    private String url;

    private String author;

    private Blob comment;

    private Date date;

    private String documentid;

    public MCRNBNS() {
    }

    public MCRNBNS(String niss, String url, String author, String comment, Date date, String documentid) {
        this.niss = niss;
        this.url = url;
        this.author = author;
        this.comment = new MCRBlob(comment.getBytes());
        this.date = date;
        this.documentid = documentid;
    }

    public String getNiss() {
        return niss;
    }

    public void setNiss(String niss) {
        this.niss = niss;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public Blob getComment() {
        return comment;
    }

    public void setComment(Blob comment) {
        this.comment = comment;
    }

    public byte[] getCommentBytes() {
        return MCRBlob.getBytes(this.comment);
    }

    public String getCommentString() {
        return new String(getCommentBytes());
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getDocumentid() {
        return documentid;
    }

    public void setDocumentid(String documentid) {
        this.documentid = documentid;
    }
}
