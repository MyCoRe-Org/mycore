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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.mycore.common.MCRInputStreamCloner;
import org.mycore.common.MCRUtils;
import org.mycore.datamodel.ifs.MCRFileContentType;
import org.mycore.datamodel.ifs.MCRFileContentTypeFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * @author Thomas Scheffler (yagee)
 * 
 * Need to insert some things here
 *
 */
public class OoTextPlugin implements TextFilterPlugin {

	private static final int MAJOR = 0;
	private static final int MINOR = 1;

	private static HashSet contentTypes;
	private static String info = null;

	private static int DEF_BYTE_SZ = 1024 * 16;

	private ByteArrayInputStream bis;

	/**
	 * 
	 */
	public OoTextPlugin() {
		super();
		if (contentTypes == null) {
			contentTypes = new HashSet();
			if (MCRFileContentTypeFactory.isTypeAvailable("sxw"))
				contentTypes.add(MCRFileContentTypeFactory.getType("sxw"));
		}
		if (info == null)
			info =
				new StringBuffer("This filter extracts the text out of a OpenOffice.org Text Document")
					.toString();

	}

	/* (non-Javadoc)
	 * @see org.mycore.services.plugins.TextFilterPlugin#getName()
	 */
	public String getName() {
		return "Yagee's amazing OpenOffice.org Text Filter";
	}

	/* (non-Javadoc)
	 * @see org.mycore.services.plugins.TextFilterPlugin#getInfo()
	 */
	public String getInfo() {
		return info;
	}

	/* (non-Javadoc)
	 * @see org.mycore.services.plugins.TextFilterPlugin#getSupportedContentTypes()
	 */
	public HashSet getSupportedContentTypes() {
		return contentTypes;
	}

	/* (non-Javadoc)
	 * @see org.mycore.services.plugins.TextFilterPlugin#transform(org.mycore.datamodel.ifs.MCRFileContentType,org.mycore.datamodel.ifs.MCRContentInputStream, java.io.OutputStream)
	 */
	public boolean transform(
		MCRFileContentType ct,
		InputStream input,
		OutputStream output)
		throws FilterPluginTransformException {
		if (getSupportedContentTypes().contains(ct)) {
			try {
				System.out.println("Reading Oo-Document");
				MCRInputStreamCloner mic=new MCRInputStreamCloner(getTextStream(getXMLStream(input)));
				System.out.println("Saving to textfile /tmp/oo.sxw.txt");
				FileOutputStream fout=new FileOutputStream("/tmp/oo.sxw.txt");
				MCRUtils.copyStream(mic.getNewInputStream(),fout);
				System.out.println("...done");
				return MCRUtils.copyStream(mic.getNewInputStream(),output);
//				return MCRUtils.copyStream(
//					getTextStream(getXMLStream(input)),
//					output);
			} catch (SAXException e) {
				throw new FilterPluginTransformException(
					"Error while parsing OpenOffice document.",
					e);
			} catch (IOException e) {
				throw new FilterPluginTransformException(
					"Error while parsing OpenOffice document.",
					e);
			}
		} else
			throw new FilterPluginTransformException(
				"ContentType "
					+ ct
					+ " is not supported by "
					+ getName()
					+ "!");
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

	private InputStream getXMLStream(InputStream inp) throws IOException {
		ZipInputStream zip = new ZipInputStream(inp);
		ZipEntry ze;
		//search for "content.xml" in ZipStream
		while ((ze = zip.getNextEntry()) != null) {
			if (ze.getName().equals("content.xml"))
				break;
		}
		if (ze == null || !ze.getName().equals("content.xml"))
			throw new FilterPluginTransformException("No content.xml was found in OpenOffice.org document!");
		ByteArrayOutputStream bos =
			new ByteArrayOutputStream(
				(ze.getSize() < 0) ? DEF_BYTE_SZ : (int) ze.getSize());
		int x;
		while ((x = zip.read()) != -1)
			bos.write(x);
		return new ByteArrayInputStream(bos.toByteArray());
	}

	private InputStream getTextStream(InputStream xml) throws SAXException {
		XMLReader reader =
			XMLReaderFactory.createXMLReader(
				"org.apache.xerces.parsers.SAXParser");
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		reader.setContentHandler(new TextHandler(bos));
		return new ByteArrayInputStream(bos.toByteArray());
	}

	private class TextHandler extends DefaultHandler {
		private static final String textNS = "http://openoffice.org/2000/text";
		private final OutputStream out;
		private boolean textElement = false;

		private TextHandler(OutputStream out) {
			this.out = out;
		}

		/* (non-Javadoc)
		* @see org.xml.sax.ContentHandler#characters(char[], int, int)
		*/
		public void characters(char[] ch, int start, int length)
			throws SAXException {
			if (textElement) {
				try {
					//write text to the stream
					out.write(bytes(ch, start, length));
					//write a space character to the stream
					out.write(bytes(new char[] { ' ' }, 0, 1));
				} catch (IOException e) {
					throw new FilterPluginTransformException(
						"Error while getting text Elements.",
						e);
				}
			}
		}

		private byte[] bytes(char[] ch, int start, int length) {
			byte[] bytes = new byte[length * 2];
			for (int i = 0; i <= length; i++) {
				bytes[i * 2] = (byte) (ch[start + i] & 0xff);
				bytes[i * 2 + 1] = (byte) (ch[start + i] >> 8 & 0xff);
			}
			return bytes;
		}

		/* (non-Javadoc)
		 * @see org.xml.sax.ContentHandler#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
		 */
		public void startElement(
			String uri,
			String localName,
			String qName,
			Attributes attributes)
			throws SAXException {
			if (uri.equals(textNS))
				textElement = true;
			else
				textElement = false;
		}

	}

}
