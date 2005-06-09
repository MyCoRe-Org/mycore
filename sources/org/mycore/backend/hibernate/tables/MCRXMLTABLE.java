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

public class MCRXMLTABLE
{
    private String id;
    private int version;
    private String type;
    private byte[] xml;

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
    public Blob getXml() {
        return new XMLBlob(xml);
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
    public void setXml(Blob xml) {
	try {
	    java.io.InputStream in = xml.getBinaryStream();
	    byte[] b = new byte[in.available()];
	    int t;
	    for(t=0;t<b.length;t++)
		b[t] = (byte)in.read();
	    this.xml = b;
	} catch(java.sql.SQLException e) {
	    e.printStackTrace();
	    this.xml = null;
	} catch(java.io.IOException e) {
	    e.printStackTrace();
	    this.xml = null;
	}
    }
    public void setXml(byte[] xml) {
        this.xml = xml;
    }

    public class XMLBlob implements java.sql.Blob
    {
	byte[] xml;
	XMLBlob(byte[] xml) {
	    this.xml = xml;
	}
	InputStream getBinaryStream() {
	    return BinaryInputStream(this.xml);
	}
	byte[] getBytes(long pos, int length) {
	    if(pos + length > xml.length)
		length = xml.length - pos;
	    byte[] result = new byte[length];
	    arraycopy(xml, pos, result, 0, length);
	    return result;
	}
	long length()
	{
	    return this.xml.length;
	}
        //Determines the byte position at which the specified byte pattern begins within the BLOB value that this Blob object represents. 
	long position(byte[] pattern, long start)
	{
	    int t;
searchloop: for(t=start;t<xml.length;t++) {
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
	}
        //Determines the byte position in the BLOB value designated by this Blob object at which pattern begins.
	long position(Blob pattern, long start)
	{
	    byte[] b = pattern.getBytes(0, pattern.length());
	    return position(b, start);
	}
    }
}
