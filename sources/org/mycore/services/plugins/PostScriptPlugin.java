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
package org.mycore.services.plugins;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashSet;

import org.mycore.common.MCRConfigurationException;
import org.mycore.common.MCRUtils;
import org.mycore.datamodel.ifs.MCRFileContentTypeFactory;
import org.mycore.datamodel.ifs.MCRFileContentType;

/**
 * Provide some info about your class!
 * 
 * @author Thomas Scheffler (yagee)
 */
public class PostScriptPlugin implements TextFilterPlugin {
	private static HashSet contentTypes = null;
	private static String name = "Yagee's amazing PostScript Filter";
	private static final int MAJOR = 0;
	private static final int MINOR = 1;
	private static String info = null;
	private static String p2t_info = null;
	private static final String textencoding = "ISO-8859-1";

	/**
	 * 
	 */
	public PostScriptPlugin() {
		super();
		if (contentTypes == null) {
			contentTypes = new HashSet();
			if (MCRFileContentTypeFactory.isTypeAvailable("ps"))
				contentTypes.add(MCRFileContentTypeFactory.getType("ps"));
		}
		if (p2t_info == null && !ps2ascii())
			throw new FilterPluginInstantiationException(
				new StringBuffer("The execution of \"p2ascii\" failed.")
					.append("Maybe it's not installed or in your search path!\n")
					.append("To use this Plugin you have to install GhostScript(")
					.append("http://www.cs.wisc.edu/~ghost/) and ensure ")
					.append("the ps2ascii binary is in your search path.\n")
					.append("Another reason maybe that you are using a version that")
					.append(" is not compatible with this Plugin:\n")
					.append(getName())
					.append(" v")
					.append(MAJOR)
					.append('.')
					.append(MINOR)
					.toString());
		if (info == null)
			info =
				new StringBuffer("This filter uses GhostScript for transformation.")
					.append("\nSource code is available on http://www.cs.wisc.edu/~ghost/")
					.append("\nCurrently using: ")
					.append(p2t_info)
					.toString();
	}

	/* (non-Javadoc)
	 * @see org.mycore.services.plugins.TextFilterPlugin#getName()
	 */
	public String getName() {
		return name;
	}

	/* (non-Javadoc)
	 * @see org.mycore.services.plugins.TextFilterPlugin#getInfo()
	 */
	public String getInfo() {
		return info;
	}

	private boolean ps2ascii() {
		int rc;
		final String[] testcommand = { "ps2ascii", "--version" };
		String s;
		StringBuffer infofetch = new StringBuffer("GhostScript v");
		try {
			Process p = Runtime.getRuntime().exec(testcommand);
			BufferedReader stdOut =
				new BufferedReader(new InputStreamReader(p.getInputStream()));
			while ((s = stdOut.readLine()) != null) {
				infofetch.append(s).append(", ");
			}
			rc = p.waitFor();
			p2t_info =
				infofetch.deleteCharAt(infofetch.length() - 2).toString();
		} catch (IOException e) {
			if (e.getMessage().indexOf("not found") > 0)
				throw new FilterPluginInstantiationException(
					new StringBuffer(testcommand[0])
						.append(" is not installed or in search path!\n")
						.append("To use this Plugin you have to install GhostScript(")
						.append("http://www.cs.wisc.edu/~ghost/) and ensure ")
						.append("the ps2ascii binary is in your search path.")
						.toString(),
					e);
			else
				throw new FilterPluginInstantiationException(
					"Error while excuting " + testcommand,
					e);
		} catch (InterruptedException e) {
			throw new FilterPluginInstantiationException(
				"Error while excuting " + testcommand,
				e);
		}
		return (rc == 0);
	}

	private boolean ps2ascii(File psfile, StringBuffer result) {
		int rc;
		final String[] testcommand = { "ps2ascii", psfile.getAbsolutePath()};
		String s;
		try {
			Process p = Runtime.getRuntime().exec(testcommand);
			BufferedReader stdError =
				new BufferedReader(new InputStreamReader(p.getErrorStream()));
			BufferedReader stdOut =
				new BufferedReader(
					new InputStreamReader(p.getInputStream(), textencoding));
			System.out.println("Reading StdOut");
			while ((s = stdOut.readLine()) != null) {
				result.append(s);
			}
			while ((s = stdError.readLine()) != null) {
				System.err.println(s);
			}
			rc = p.waitFor();
		} catch (IOException e) {
			if (e.getMessage().indexOf("not found") > 0)
				throw new MCRConfigurationException(
					testcommand[0] + " is not installed or in search path!",
					e);
			else
				throw new MCRConfigurationException(
					"Error while excuting " + testcommand,
					e);
		} catch (InterruptedException e) {
			throw new MCRConfigurationException(
				"Error while excuting " + testcommand,
				e);
		}
		return (rc == 00);
	}

	/* (non-Javadoc)
	 * @see org.mycore.services.plugins.TextFilterPlugin#getSupportedContentTypes()
	 */
	public HashSet getSupportedContentTypes() {
		return contentTypes;
	}

	public Reader transform(MCRFileContentType ct, InputStream input)
		throws FilterPluginTransformException {
		if (!getSupportedContentTypes().contains(ct))
			throw new FilterPluginTransformException(
				"ContentType "
					+ ct
					+ " is not supported by "
					+ getName()
					+ "!");
		System.err.println("===== PS decoding starts ====");
		try {
			File psfile = File.createTempFile("inp", ".ps");
			BufferedOutputStream out =
				new BufferedOutputStream(new FileOutputStream(psfile));
			psfile.deleteOnExit();
			MCRUtils.copyStream(input, out);
			out.close();
			StringBuffer text = new StringBuffer();
			if (!ps2ascii(psfile, text)) {
				throw new FilterPluginTransformException("ps2ascii reported an error while exporting text of PostScript file!");
			}
			return new StringBufferReader(text);
		} catch (FileNotFoundException e) {
			throw new FilterPluginTransformException("File was not found!", e);
		} catch (IOException e) {
			throw new FilterPluginTransformException(
				"General I/O Exception occured",
				e);
		}
	}

	/**
	 * @see org.mycore.services.plugins.TextFilterPlugin#getMajorNumber()
	 */
	public int getMajorNumber() {
		return MAJOR;
	}

	/**
	 * @see org.mycore.services.plugins.TextFilterPlugin#getMinorNumber()
	 */
	public int getMinorNumber() {
		return MINOR;
	}

	private static class StringBufferReader extends Reader {
		private final StringBuffer buf;
		private int pos;
		public StringBufferReader(StringBuffer buf) {
			this.buf = buf;
			pos = 0;
		}
		/* (non-Javadoc)
		* @see java.io.Reader#close()
		*/
		public void close() throws IOException {
		}

		/* (non-Javadoc)
		 * @see java.io.Reader#read(char[], int, int)
		 */
		public int read(char[] cbuf, int off, int len) throws IOException {
			if (pos == buf.length())
				return -1;
			else {
				int start = pos + off;
				int charsRead =
					(buf.length() < (start + len))
						? (buf.length() - start)
						: len;
				int end = start + charsRead;
				buf.getChars(start, end, cbuf, 0);
				pos = end;
				return charsRead;
			}
		}

	}

}
