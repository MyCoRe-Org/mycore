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

import java.util.Objects;

import org.jdom2.Element;
import org.mycore.common.MCRException;

import com.google.gson.JsonObject;

/**
 * This class implements all method for handling the IFS metadata. The
 * MCRMetaIFS class present all informations to store and retrieve derivates to
 * the IFS.
 * <p>
 * &lt;tag class="MCRMetaIFS" &gt; <br>
 * &lt;subtag sourcepath="..." maindoc="..." ifsid="..." /&gt; <br>
 * &lt;/tag&gt; <br>
 * 
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 */
final public class MCRMetaIFS extends MCRMetaDefault {
    // MCRMetaIFS data
    private String sourcepath;

    private String maindoc;

    private String ifsid;

    /**
     * This is the constructor. <br>
     * The language element was set to <b>en </b>. All other data was set to
     * empty.
     */
    public MCRMetaIFS() {
        super();
        sourcepath = "";
        maindoc = "";
        ifsid = "";
    }

    /**
     * This is the constructor. <br>
     * The language element was set to <b>en </b>. The subtag element was set to
     * the value of <em>set_subtag</em>. If the
     * value of <em>set_subtag</em> is null or empty an exception was throwed.
     * The type element was set empty.
     * The sourcepath must be NOT null or empty.
     * @param set_subtag       the name of the subtag
     * @param set_sourcepath   the sourcepath attribute
     * @exception MCRException if the set_subtag value, the set_classid value or
     * the set_categid are null, empty, too long or not a MCRObjectID
     */
    public MCRMetaIFS(String set_subtag, String set_sourcepath) throws MCRException {
        super(set_subtag, null, null, 0);
        setSourcePath(set_sourcepath);
        maindoc = "";
        ifsid = "";
    }

    /**
     * The method return the derivate source path.
     * 
     * @return the sourcepath
     */
    public final String getSourcePath() {
        return sourcepath;
    }

    /**
     * The method return the derivate main document name.
     * 
     * @return the main document name.
     */
    public final String getMainDoc() {
        return maindoc;
    }

    /**
     * The method return the derivate IFS ID.
     * 
     * @return the IFS ID.
     */
    public final String getIFSID() {
        return ifsid;
    }

    /**
     * This method set the value of derivate source path.
     * 
     * @param set_sourcepath
     *            the derivate source path
     */
    public final void setSourcePath(String set_sourcepath) {
        sourcepath = set_sourcepath;
    }

    /**
     * This method set the value of derivate main document.
     * 
     * @param set_maindoc
     *            the derivate main document name
     */
    public final void setMainDoc(String set_maindoc) {
        if (set_maindoc == null) {
            maindoc = "";
        } else {
            maindoc = set_maindoc;
        }
    }

    /**
     * This method set the value of derivate IFS ID.
     * 
     * @param set_ifsid
     *            the derivate IFS ID
     */
    public final void setIFSID(String set_ifsid) {
        if (set_ifsid == null) {
            ifsid = "";
        } else {
            ifsid = set_ifsid;
        }
    }

    /**
     * This method read the XML input stream part from a DOM part for the
     * metadata of the document.
     * 
     * @param element
     *            a relevant JDOM element for the metadata
     * @exception MCRException
     *                if the set_sourcepath value is null or empty
     */
    @Override
    public final void setFromDOM(org.jdom2.Element element) throws MCRException {
        super.setFromDOM(element);
        setSourcePath(element.getAttributeValue("sourcepath"));
        setMainDoc(element.getAttributeValue("maindoc"));
        setIFSID(element.getAttributeValue("ifsid"));
    }

    /**
     * This method create a XML stream for all data in this class, defined by
     * the MyCoRe XML MCRMetaIFS definition for the given subtag.
     * 
     * @exception MCRException
     *                if the content of this class is not valid
     * @return a JDOM Element with the XML MCRClassification part
     */
    @Override
    public final Element createXML() throws MCRException {
        Element elm = super.createXML();
        if (sourcepath != null) {
            elm.setAttribute("sourcepath", sourcepath);
        }
        elm.setAttribute("maindoc", maindoc);
        elm.setAttribute("ifsid", ifsid);

        return elm;
    }

    /**
     * Creates the JSON representation. Extends the {@link MCRMetaDefault#createJSON()} method
     * with the following data.
     * 
     * <pre>
     *   {
     *     sourcepath: "...",
     *     maindoc: "image.tif",
     *     ifsid: "ve3s8a3j00xsfk8z"
     *   }
     * </pre>
     * 
     */
    @Override
    public JsonObject createJSON() {
        JsonObject obj = super.createJSON();
        if (sourcepath != null) {
            obj.addProperty("sourcepath", sourcepath);
        }
        obj.addProperty("maindoc", maindoc);
        obj.addProperty("ifsid", ifsid);
        return obj;
    }

    /**
     * Validates this MCRMetaIFS. This method throws an exception if:
     * <ul>
     * <li>the subtag is not null or empty</li>
     * <li>the lang value was supported</li>
     * <li>the inherited value is lower than zero</li>
     * <li>the trimmed sourcepath is null empty</li>
     * </ul>
     * 
     * @throws MCRException the MCRMetaIFS is invalid
     */
    public void validate() throws MCRException {
        super.validate();
        if (sourcepath != null && (sourcepath = sourcepath.trim()).length() == 0) {
            throw new MCRException(getSubTag() + ": sourcepath is null or empty");
        }
    }

    @Override
    public final MCRMetaIFS clone() {
        MCRMetaIFS ifs = new MCRMetaIFS(getSubTag(), getSourcePath());
        ifs.setMainDoc(getMainDoc());
        ifs.setIFSID(getIFSID());
        ifs.setType(getType());
        return ifs;
    }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) {
            return false;
        }
        final MCRMetaIFS other = (MCRMetaIFS) obj;
        return Objects.equals(sourcepath, other.sourcepath) && Objects.equals(maindoc, other.maindoc)
            && Objects.equals(ifsid, other.ifsid);
    }

}
