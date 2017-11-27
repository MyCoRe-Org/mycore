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

package org.mycore.datamodel.ifs;

/**
 * Instances of this class represent information about the content type of a
 * file.
 * 
 * @author Frank Lützenkirchen
 */
public class MCRFileContentType {
    /** The unique ID of this file content type */
    protected String ID;

    /** The label of this file content type */
    protected String label;

    /** The URL where information such as a plug-in download page can be found */
    protected String url;

    /** The MIME type used to deliver this file type to a client browser */
    protected String mimeType;

    /**
     * Constructs a new file content type instance. The list of known file
     * content types is defined in an XML file that is specified in the property
     * MCR.IFS.FileContentTypes.DefinitionFile, and that file is searched in the
     * CLASSPATH directories or JAR files and parsed by
     * MCRFileContentTypeFactory.
     * 
     * @see MCRFileContentTypeFactory
     * 
     * @param ID
     *            the unique content type ID
     * @param label
     *            the label of this content type
     * @param url
     *            the url where information like a plug-in can be found
     * @param mimeType
     *            the MIME type used for this content type
     */
    MCRFileContentType(String ID, String label, String url, String mimeType) {
        this.ID = ID;
        this.label = label;
        this.url = url;
        this.mimeType = mimeType;
    }

    /**
     * Returns the unique ID of this file content type
     * 
     * @return the unique ID of this file content type
     */
    public String getID() {
        return ID;
    }

    /**
     * Returns the label of this file content type
     * 
     * @return the label of this file content type
     * 
     */
    public String getLabel() {
        return label;
    }

    /**
     * Returns the MIME type used to deliver this file type to a client browser.
     * If no MIME type is set, the default type "application/octet-stream" for
     * binary content is returned.
     * 
     * @return the MIME type used to deliver this file type to a client browser
     */
    public String getMimeType() {
        return mimeType != null ? mimeType : "application/octet-stream";
    }

    /**
     * Returns the URL where additional information like a plug-in download page
     * can be found
     * 
     * @return the URL of additional information, or null
     */
    public String getURL() {
        return url;
    }

    @Override
    public String toString() {

        return "ID    = " + getID() + "\n" + "label = " + getLabel() + "\n" + "mime  = " + getMimeType() + "\n"
            + "url   = " + getURL();
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof MCRFileContentType)
            return ((MCRFileContentType) other).ID.equals(ID);
        else
            return false;
    }

    @Override
    public int hashCode() {
        return ID.hashCode();
    }
}
