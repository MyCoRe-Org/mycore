/**
 * $RCSfile$
 * $Revision$ $Date$
 *
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
 *
 **/
package org.mycore.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * makes it possible to copy a InputStream and use it multiple times
 * 
 * The InputStream is closed after calling the constructor even if Exception is
 * thrown and instance is not useable afterwards.
 * 
 * @author Thomas Scheffler (yagee)
 */
public class MCRInputStreamCloner {
	private final File streamSource;

	/**
	 * default constructor reading InputStream and close him
	 * @param source InputStream to be cloned
	 * @throws IOException if write access to temp directory fails
	 */
	public MCRInputStreamCloner(InputStream source) throws IOException {
		super();
		File tmpdir = new File(System.getProperty("java.io.tmpdir"));
		streamSource = File.createTempFile("JavaStream", ".mycore");
		//File is new and created
		FileOutputStream fout = new FileOutputStream(streamSource);
		if (!MCRUtils.copyStream(source, fout)) {
			source.close(); //you can't use it safely again
			fout.close();
			streamSource.delete();
			throw new IOException(
				"Could not save InputStream to file " + streamSource.getName());
		}
		//we don't need the streams any longer
		fout.close();
		source.close();
		//all went well to this point
		streamSource.setReadOnly();
		streamSource.deleteOnExit();
	}

	private static final File getStreamFile(File dir, int hash)
		throws IOException {
		File returns = null;
		final String prefix = "javaStream";
		if (!dir.canRead())
			throw new IOException("Access denied(read): " + dir.getName());
		if (!dir.canWrite())
			throw new IOException("Access denied(write): " + dir.getName());
		for (int i = 0; i < Integer.MAX_VALUE; i++) {
			returns = new File(dir, prefix + hash + "-" + i + ".tmp");
			if (!returns.exists()) {
				returns.createNewFile();
				break;
			}
		}
		return returns;
	}

	public InputStream getNewInputStream() throws IOException {
		if (!streamSource.exists())
			throw new IOException(
				"Access denied(file does not exist): "
					+ streamSource.getName());
		return new FileInputStream(streamSource);
	}

	public void close() {
		streamSource.delete();
	}

	protected void finalize() throws Throwable {
		close();
		super.finalize();
	}
}
