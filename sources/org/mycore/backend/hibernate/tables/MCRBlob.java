/*
 * This file is part of ** M y C o R e ** Visit our homepage at
 * http://www.mycore.de/ for details.
 * 
 * This program is free software; you can use it, redistribute it and / or
 * modify it under the terms of the GNU General Public License (GPL) as
 * published by the Free Software Foundation; either version 2 of the License or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program, normally in the file license.txt. If not, write to the Free
 * Software Foundation Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307
 * USA
 */

package org.mycore.backend.hibernate.tables;

import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Blob;
import java.sql.SQLException;

public class MCRBlob implements java.sql.Blob {
	byte[] data;

	MCRBlob(byte[] data) {
		this.data = data;
	}

	public InputStream getBinaryStream() {
		return new java.io.ByteArrayInputStream(this.data);
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
		if (pos + length > data.length)
			length = (int) (data.length - pos);
		byte[] result = new byte[length];
		System.arraycopy(data, (int) pos, result, 0, length);
		return result;
	}

	public long length() {
		return this.data.length;
	}

	//Determines the byte position at which the specified byte pattern begins
	// within the BLOB value that this Blob object represents.
	public long position(byte[] pattern, long start) {
		int t;
		searchloop: for (t = (int) start; t < data.length; t++) {
			int s;
			int len = data.length - t;
			if (pattern.length > data.length - t)
				break searchloop;
			for (s = 0; s < len; s++) {
				if (pattern[s] != data[t])
					continue searchloop;
			}
			return t;
		}
		return -1;
	}

	//Determines the byte position in the BLOB value designated by this Blob
	// object at which pattern begins.
	public long position(Blob pattern, long start) throws SQLException {
		byte[] b = pattern.getBytes(0, (int) pattern.length());
		return position(b, start);
	}

	public static byte[] getBytes(Blob blob) {
		try {
			java.io.InputStream in = blob.getBinaryStream();
			byte[] b = new byte[in.available()];
			int t;
			for (t = 0; t < b.length; t++)
				b[t] = (byte) in.read();
			return b;
		} catch (java.sql.SQLException e) {
			e.printStackTrace();
			return null;
		} catch (java.io.IOException e) {
			e.printStackTrace();
			return null;
		}
	}
}
