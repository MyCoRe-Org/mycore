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
import java.sql.SQLException;
import java.io.InputStream;
import java.io.OutputStream;

public class MCRXMLTABLE
{
    private String id;
    private int version;
    private String type;
    private Blob xml;

    public MCRXMLTABLE()
    {
    }

    public MCRXMLTABLE(String id, int version, String type, Blob xml) 
    {
	this.id = id;
	this.version = version;
	this.type = type;
        this.xml = xml;
    }

    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public int getVersion() {
        return version;
    }
    public void setVersion(int version) {
        this.version = version;
    }
    public String getType() {
        return type;
    }
    public void setType(String type) {
        this.type = type;
    }
    public byte[] getXmlByteArray() {
	try {
	    java.io.InputStream in = xml.getBinaryStream();
	    byte[] b = new byte[in.available()];
	    int t;
	    for(t=0;t<b.length;t++)
		b[t] = (byte)in.read();
	    return b;
	} catch(java.sql.SQLException e) {
	    e.printStackTrace();
	    return null;
	} catch(java.io.IOException e) {
	    e.printStackTrace();
	    return null;
	}
    }
    public Blob getXml() {
            return xml;
    }
    public void setXml(byte[] xml) {
        this.xml = new XMLBlob(xml);
    }

    public class XMLBlob implements java.sql.Blob
    {
	byte[] xml;
	XMLBlob(byte[] xml) {
	    this.xml = xml;
	}
	public InputStream getBinaryStream() {
	    return new java.io.ByteArrayInputStream(this.xml);
	}

        public OutputStream setBinaryStream(long l) {
            throw new IllegalArgumentException("not implemented");
        }
        public int setBytes(long l, byte[] a, int u, int v) {
            throw new IllegalArgumentException("not implemented");
        }
        public int setBytes(long l, byte[] a) {
            throw new IllegalArgumentException("not implemented");
        }
        public void truncate(long l) {
            throw new IllegalArgumentException("not implemented");
        }


	public byte[] getBytes(long pos, int length) {
	    if(pos + length > xml.length)
		length = (int)(xml.length - pos);
	    byte[] result = new byte[length];
	    System.arraycopy(xml, (int)pos, result, 0, length);
	    return result;
	}
	public long length()
	{
	    return this.xml.length;
	}
        //Determines the byte position at which the specified byte pattern begins within the BLOB value that this Blob object represents. 
	public long position(byte[] pattern, long start)
	{
	    int t;
searchloop: for(t=(int)start;t<xml.length;t++) {
		int s;
		int len = xml.length - t;
		if(pattern.length > xml.length - t)
		    break searchloop;
		for(s=0;s<len;s++) {
		    if(pattern[s] != xml[t]) 
			continue searchloop;
		}
		return t;
	    }
            return -1;
	}
        //Determines the byte position in the BLOB value designated by this Blob object at which pattern begins.
	public long position(Blob pattern, long start) throws SQLException
	{
	    byte[] b = pattern.getBytes(0, (int)pattern.length());
	    return position(b, start);
	}
    }
}