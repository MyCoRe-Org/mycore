/*
 * 
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

package org.mycore.datamodel.metadata;

import org.apache.log4j.Logger;
import org.jdom2.Element;
import org.mycore.common.MCRException;

/**
 * This class implements all method for handling with the MCRMetaLangText part
 * of a metadata object. The MCRMetaISBN class present a single item, which
 * holds a ISBN.
 * 
 * @author Jens Kupferschmidt
 * @author Thomas Scheffler (yagee)
 * @version $Revision$ $Date$
 */
public class MCRMetaISBN extends MCRMetaDefault {
    // MetaISBN data
    protected String isbn;

    protected int sum1;

    protected int sum2;

    protected boolean invalid;

    private static final Logger LOGGER = Logger.getLogger(MCRMetaISBN.class);

    /**
     * This is the constructor. <br>
     * The language element was set to <b>en </b>. All other elemnts was set to
     * an empty string.
     */
    public MCRMetaISBN() {
        super();
        isbn = null;
        invalid = true;
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
     * @param set_subtag       the name of the subtag
     * @param set_inherted     a value&gt;= 0
     * @param set_isbn         the ISBN string
     *
     * @exception MCRException if the set_subtag value is null or empty
     */
    public MCRMetaISBN(String set_subtag, int set_inherted, String set_isbn) throws MCRException {
        super(set_subtag, null, null, set_inherted);
        invalid = true;
        setISBN(set_isbn);
    }

    /**
     * This method set the ISBN.
     * 
     * @param set_isbn
     *            the new ISBN string
     */
    public final void setISBN(String set_isbn) {
        isbn = set_isbn;
        getSums();
    }

    /**
     * This method get the ISBN element.
     * 
     * @return the ISBN
     */
    public final String getISBN() {
        return isbn;
    }

    /**
     * This method read the XML input stream part from a DOM part for the
     * metadata of the document.
     * 
     * @param element
     *            a relevant JDOM element for the metadata
     */
    @Override
    public void setFromDOM(org.jdom2.Element element) {
        super.setFromDOM(element);
        setISBN(element.getText());
    }

    /**
     * This method create a XML stream for all data in this class, defined by
     * the MyCoRe XML MCRMetaLangText definition for the given subtag.
     * 
     * @exception MCRException
     *                if the content of this class is not valid
     * @return a JDOM Element with the XML MCRMetaLangText part
     */
    @Override
    public org.jdom2.Element createXML() throws MCRException {
        Element elm = super.createXML();
        elm.addContent(isbn);

        return elm;
    }

    /**
     * This method check the validation of the content of this class. The method
     * returns <em>true</em> if
     * <ul>
     * <li>the subtag is not null or empty
     * <li>the isbn is not null or empty
     * </ul>
     * otherwise the method return <em>false</em>
     * 
     * @return a boolean value
     */
    @Override
    public boolean isValid() {
        if (!super.isValid()) {
            return false;
        }

        if (isbn == null || invalid || isbn.length() == 0 || sum2 % 11 > 0) {
            LOGGER.warn(getSubTag() + ": isbn is invalid: " + isbn);
            return false;
        }

        return true;
    }

    /**
     * calculates the sum of the ISBN to check later the correctness
     * 
     */
    protected void getSums() {
        if (isbn != null) {
            char[] nums = isbn.toCharArray();

            invalid = false;
            for (char num : nums) {
                switch (num) {
                case 'X':
                    sum1++;

                case '9':
                    sum1++;

                case '8':
                    sum1++;

                case '7':
                    sum1++;

                case '6':
                    sum1++;

                case '5':
                    sum1++;

                case '4':
                    sum1++;

                case '3':
                    sum1++;

                case '2':
                    sum1++;

                case '1':
                    sum1++;

                case '0':
                    sum2 += sum1;

                    break;

                case '-':
                    break;

                default:
                    invalid = true;
                }
            }
        } else {
            invalid = true;
        }
    }

    /**
     * This method make a clone of this class.
     */
    @Override
    public MCRMetaISBN clone() {
        return new MCRMetaISBN(subtag, inherited, isbn);
    }

    /**
     * This method put debug data to the logger (for the debug mode).
     */
    @Override
    public final void debug() {
        super.debugDefault();
        LOGGER.debug("ISBN               = " + isbn);
    }
}
