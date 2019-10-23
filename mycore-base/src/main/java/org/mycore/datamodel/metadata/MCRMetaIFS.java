/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mycore.datamodel.metadata;

import java.util.Objects;

import org.jdom2.Element;
import org.mycore.common.MCRException;
import org.mycore.common.MCRUtils;

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
public final class MCRMetaIFS extends MCRMetaDefault {
    // MCRMetaIFS data
    private String sourcePath;

    private String maindoc;

    private String ifsId;

    /**
     * This is the constructor. <br>
     * The language element was set to <b>en </b>. All other data was set to
     * empty.
     */
    public MCRMetaIFS() {
        super();
        sourcePath = "";
        maindoc = "";
        ifsId = "";
    }

    /**
     * This is the constructor. <br>
     * The language element was set to <b>en </b>. The subtag element was set to
     * the value of <em>subtag</em>. If the
     * value of <em>subtag</em> is null or empty an exception was throwed.
     * The type element was set empty.
     * The sourcepath must be NOT null or empty.
     * @param subtag       the name of the subtag
     * @param sourcePath   the sourcepath attribute
     * @exception MCRException if the subtag value, the set_classid value or
     * the set_categid are null, empty, too long or not a MCRObjectID
     */
    public MCRMetaIFS(String subtag, String sourcePath) throws MCRException {
        super(subtag, null, null, 0);
        setSourcePath(sourcePath);
        maindoc = "";
        ifsId = "";
    }

    /**
     * The method return the derivate source path.
     * 
     * @return the sourcepath
     */
    public String getSourcePath() {
        return sourcePath;
    }

    /**
     * The method return the derivate main document name.
     * 
     * @return the main document name.
     */
    public String getMainDoc() {
        return maindoc;
    }

    /**
     * The method return the derivate IFS ID.
     * 
     * @return the IFS ID.
     */
    public String getIFSID() {
        return ifsId;
    }

    /**
     * This method set the value of derivate source path.
     * 
     * @param sourcePath
     *            the derivate source path
     */
    public void setSourcePath(String sourcePath) {
        this.sourcePath = sourcePath;
    }

    /**
     * This method set the value of derivate main document.
     * 
     * @param mainDoc
     *            the derivate main document name
     */
    public void setMainDoc(String mainDoc) {
        if (mainDoc == null) {
            maindoc = "";
        } else {
            maindoc = mainDoc.startsWith("/") ? mainDoc.substring(1) : mainDoc;
        }
    }

    /**
     * This method set the value of derivate IFS ID.
     * 
     * @param ifsId
     *            the derivate IFS ID
     */
    public void setIFSID(String ifsId) {
        if (ifsId == null) {
            this.ifsId = "";
        } else {
            this.ifsId = ifsId;
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
    public void setFromDOM(Element element) throws MCRException {
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
    public Element createXML() throws MCRException {
        Element elm = super.createXML();
        if (sourcePath != null) {
            elm.setAttribute("sourcepath", sourcePath);
        }
        elm.setAttribute("maindoc", maindoc);
        elm.setAttribute("ifsid", ifsId);

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
        if (sourcePath != null) {
            obj.addProperty("sourcepath", sourcePath);
        }
        obj.addProperty("maindoc", maindoc);
        obj.addProperty("ifsid", ifsId);
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
        sourcePath = MCRUtils.filterTrimmedNotEmpty(sourcePath)
            .orElseThrow(() -> new MCRException(getSubTag() + ": sourcepath is null or empty"));
    }

    @Override
    public MCRMetaIFS clone() {
        MCRMetaIFS clone = (MCRMetaIFS) super.clone();

        clone.maindoc = this.maindoc;
        clone.ifsId = this.ifsId;
        clone.sourcePath = this.sourcePath;

        return clone;
    }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) {
            return false;
        }
        final MCRMetaIFS other = (MCRMetaIFS) obj;
        return Objects.equals(sourcePath, other.sourcePath) && Objects.equals(maindoc, other.maindoc)
            && Objects.equals(ifsId, other.ifsId);
    }

}
