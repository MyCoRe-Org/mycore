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

package org.mycore.datamodel.metadata;

import org.jdom.Namespace;

import org.mycore.common.MCRException;

/**
 * This class implements all method for handling with the MCRMetaLangText part
 * of a metadata object. The MCRMetaNBN class present a single item, which holds
 * a NBN.
 * 
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 */
public class MCRMetaNBN extends MCRMetaDefault implements MCRMetaInterface {

	// MetaNBN data
	protected String nbn;

	/**
	 * This is the constructor. <br>
	 * The language element was set to <b>en </b>. All other elemnts was set to
	 * an empty string.
	 */
	public MCRMetaNBN() {
		super();
		nbn = "";
	}

	/**
	 * This is the constructor. <br>
	 * The language element was set. If the value of <em>default_lang</em> is
	 * null, empty or false <b>en </b> was set. The subtag element was set to
	 * the value of <em>set_subtag<em>. If the value of <em>set_subtag</em>
	 * is null or empty an exception was throwed. The type element was set to
	 * the value of <em>set_type<em>, if it is null, an empty string was set
	 * to the type element. The text element was set to the value of 
	 * <em>set_text<em>, if it is null, an empty string was set
	 * to the text element.
	 *
	 * @param set_datapart     the global part of the elements like 'metadata'
	 *                         or 'service'
	 * @param set_subtag       the name of the subtag
	 * @param set_inherted     a value >= 0
	 * @param set_nbn         the NBN string
	 * @exception MCRException if the set_subtag value is null or empty
	 */
	public MCRMetaNBN(String set_datapart, String set_subtag, int set_inherted,
			String set_nbn) throws MCRException {
		super(set_datapart, set_subtag, "de", "", set_inherted);
		nbn = "";
		if (set_nbn != null) {
			nbn = set_nbn.trim();
		}
	}

	/**
	 * This method set the NBN.
	 * 
	 * @param set_nbn
	 *            the new NBN string
	 */
	public final void set(String set_nbn) {
		setLang("de");
		setType("");
		if (set_nbn != null) {
			nbn = set_nbn.trim();
		}
	}

	/**
	 * This method set the nbn.
	 * 
	 * @param set_nbn
	 *            the new NBN string
	 */
	public final void setNBN(String set_nbn) {
		set(set_nbn);
	}

	/**
	 * This method get the NBN element.
	 * 
	 * @return the NBN
	 */
	public final String getNBN() {
		return nbn;
	}

	/**
	 * This method read the XML input stream part from a DOM part for the
	 * metadata of the document.
	 * 
	 * @param element
	 *            a relevant JDOM element for the metadata
	 */
	public void setFromDOM(org.jdom.Element element) {
		super.setFromDOM(element);
		String temp_nbn = (element.getText()).trim();
		if (temp_nbn == null) {
			temp_nbn = "";
		}
		nbn = temp_nbn;
	}

	/**
	 * This method create a XML stream for all data in this class, defined by
	 * the MyCoRe XML MCRMetaLangText definition for the given subtag.
	 * 
	 * @exception MCRException
	 *                if the content of this class is not valid
	 * @return a JDOM Element with the XML MCRMetaLangText part
	 */
	public org.jdom.Element createXML() throws MCRException {
		if (!isValid()) {
			debug();
			throw new MCRException("The content of MCRMetaNBN is not valid.");
		}
		org.jdom.Element elm = new org.jdom.Element(subtag);
		elm.setAttribute("lang", lang, Namespace.XML_NAMESPACE);
		elm.setAttribute("inherited", (new Integer(inherited)).toString());
		if ((type != null) && ((type = type.trim()).length() != 0)) {
			elm.setAttribute("type", type);
		}
		elm.addContent(nbn);
		return elm;
	}

	/**
	 * This methode create a typed content list for all data in this instance.
	 * 
	 * @param parasearch
	 *            true if the data should parametric searchable
	 * @exception MCRException
	 *                if the content of this class is not valid
	 * @return a MCRTypedContent with the data of the MCRObject data
	 */
	public MCRTypedContent createTypedContent(boolean parasearch)
			throws MCRException {
		if (!isValid()) {
			debug();
			throw new MCRException("The content of MCRMetaNBN is not valid.");
		}
		MCRTypedContent tc = new MCRTypedContent();
		if (!parasearch) {
			return tc;
		}
		tc.addTagElement(MCRTypedContent.TYPE_SUBTAG, subtag);
		tc.addStringElement(MCRTypedContent.TYPE_VALUE, null, nbn);
		tc.addStringElement(MCRTypedContent.TYPE_ATTRIBUTE, "lang", lang);
		if ((type = type.trim()).length() != 0) {
			tc.addStringElement(MCRTypedContent.TYPE_ATTRIBUTE, "type", type);
		}
		return tc;
	}

	/**
	 * This methode create a String for all text searchable data in this
	 * instance.
	 * 
	 * @param textsearch
	 *            true if the data should text searchable
	 * @exception MCRException
	 *                if the content of this class is not valid
	 * @return a String with the text value
	 */
	public String createTextSearch(boolean textsearch) throws MCRException {
		if (textsearch)
			return nbn;
		return "";
	}

	/**
	 * This method check the validation of the content of this class. The method
	 * returns <em>true</em> if
	 * <ul>
	 * <li>the subtag is not null or empty
	 * <li>the nbn is not null or empty
	 * </ul>
	 * otherwise the method return <em>false</em>
	 * 
	 * @return a boolean value
	 */
	public boolean isValid() {
		if (!super.isValid()) {
			return false;
		}
		if ((nbn == null) || ((nbn = nbn.trim()).length() == 0)) {
			return false;
		}
		return true;
	}

	/**
	 * This method make a clone of this class.
	 */
	public Object clone() {
		MCRMetaNBN out = new MCRMetaNBN(datapart, subtag, inherited, nbn);
		return (Object) out;
	}

	/**
	 * This method put debug data to the logger (for the debug mode).
	 */
	public final void debug() {
		LOGGER.debug("Start Class : MCRMetaNBN");
		super.debugDefault();
		LOGGER.debug("NBN                = " + nbn);
	}

}
