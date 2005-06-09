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

package org.mycore.frontend.fileupload;

import java.io.*;
import org.apache.log4j.Logger;

/**
 * This class handles upload of files. Subclasses handle storage of files.
 * 
 * @author Harald Richter
 * @version $Revision$ $Date$
 */
public abstract class MCRUploadHandlerBase {
    /** File separator */
    public static String SLASH = System.getProperty("file.separator");

    /** The logger */
    protected Logger logger = null;

    /** The ID of himself */
    protected String uploadId = "";

    /** data of the handle */
    protected String url = "";

    protected String docId = "";

    protected String derId = "";

    protected String mode = "";

    protected String mainfile = "";

    protected String dirname = "";

    /**
     * The constructor.
     */
    protected MCRUploadHandlerBase() {
    }

    /**
     * The method set the logger.
     * 
     * @param logger
     *            the log4j logger instance
     */
    public final void setLogger(Logger logger) {
        this.logger = logger;
    }

    /**
     * Get the ID of the handler
     * 
     * @return the handler ID
     */
    public final String getId() {
        return uploadId;
    }

    /**
     * Set the ID of the handler
     * 
     * @param id
     *            the handler ID as String
     */
    public final void setId(String id) {
        if ((id != null) && ((id = id.trim()).length() != 0)) {
            uploadId = id;
        }
    }

    /**
     * The method return teh URL for redirection.
     * 
     * @return the redirection URL
     */
    public String getRedirectURL() {
        return url;
    }

    public abstract void startUpload(int numFiles) throws Exception;

    public boolean acceptFile(String path, String checksum) throws Exception {
        return true;
    }

    public abstract void receiveFile(String path, InputStream in)
            throws Exception;

    public abstract void finishUpload() throws Exception;

}