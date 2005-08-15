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

import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.HashSet;

import org.textmining.text.extraction.WordExtractor;

import org.mycore.datamodel.ifs.MCRFileContentType;
import org.mycore.datamodel.ifs.MCRFileContentTypeFactory;

/**
 * @author Thomas Scheffler (yagee)
 * 
 * Need to insert some things here
 *  
 */
public class MSWordPlugin implements TextFilterPlugin {

	private static final int MAJOR = 0;

	private static final int MINOR = 1;

	private static HashSet contentTypes;

	private static String info = null;

	/**
	 *  
	 */
	public MSWordPlugin() {
		super();
		if (contentTypes == null) {
			contentTypes = new HashSet();
			if (MCRFileContentTypeFactory.isTypeAvailable("msword95"))
				contentTypes.add(MCRFileContentTypeFactory.getType("msword95"));
			if (MCRFileContentTypeFactory.isTypeAvailable("msword97"))
				contentTypes.add(MCRFileContentTypeFactory.getType("msword97"));
		}
		if (info == null)
			info = new StringBuffer(
					"This filter extracts the text out of a Word Document")
					.toString();

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mycore.services.plugins.TextFilterPlugin#getName()
	 */
	public String getName() {
		return "Yagee's amazing Microsoft(R) Word(R) Text Filter";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mycore.services.plugins.TextFilterPlugin#getInfo()
	 */
	public String getInfo() {
		return info;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mycore.services.plugins.TextFilterPlugin#getSupportedContentTypes()
	 */
	public HashSet getSupportedContentTypes() {
		return contentTypes;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mycore.services.plugins.TextFilterPlugin#transform(org.mycore.datamodel.ifs.MCRFileContentType,org.mycore.datamodel.ifs.MCRContentInputStream,
	 *      java.io.OutputStream)
	 */
	public Reader transform(MCRFileContentType ct, InputStream input)
			throws FilterPluginTransformException {
		if (getSupportedContentTypes().contains(ct)) {
			try {
				System.out.println("Reading Word-Document");
				return getTextReader(input);
			} catch (Exception e) {
				throw new FilterPluginTransformException(
						"Error while parsing Word Document document.", e);
			}
		} else
			throw new FilterPluginTransformException("ContentType " + ct
					+ " is not supported by " + getName() + "!");
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

	private Reader getTextReader(InputStream word) throws Exception {
		return new StringReader(new WordExtractor().extractText(word));
	}

}