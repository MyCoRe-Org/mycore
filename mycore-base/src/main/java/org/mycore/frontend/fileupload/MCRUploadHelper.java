/*
 * 
 * $Revision: 34120 $ $Date: 2015-12-02 23:16:17 +0100 (Mi, 02 Dez 2015) $
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

package org.mycore.frontend.fileupload;

import org.apache.log4j.Logger;
import org.hibernate.Transaction;
import org.mycore.backend.hibernate.MCRHIBConnection;
import org.mycore.common.MCRException;

/**
 * Common helper class for all services handling file upload.
 * 
 * @author Frank Lützenkirchen
 * 
 * @version $Revision: 34120 $ $Date: 2015-12-02 23:16:17 +0100 (Mi, 02 Dez 2015) $
 */
public abstract class MCRUploadHelper {

    private final static Logger LOGGER = Logger.getLogger(MCRUploadHelper.class);

    /**
     * reserved URI characters should not be in uploaded filenames. See RFC3986,
     * Section 2.2
     */
    private static final char[] reserverdCharacters = { ':', '?', '%', '#', '[', ']', '@', '!', '$', '&', '\'', '(',
        ')', '*', ',', ';', '=', '\'', '+' };

    /**
     * checks if path contains reserved URI characters or path starts or ends with whitespace. There are some characters
     * that are maybe allowed in file names but are reserved in URIs.
     * 
     * @see <a href="http://tools.ietf.org/html/rfc3986#section-2.2">RFC3986, Section 2.2</a>
     * @param path
     *            complete path name
     * @throws MCRException
     *             if path contains reserved character
     */
    static void checkPathName(String path) throws MCRException {
        for (char c : reserverdCharacters)
            if (path.contains("" + c))
                throw new MCRException("File path " + path + " contains reserved character: '" + c + "'");

        if (path.contains("../") || path.contains("..\\"))
            throw new MCRException("File path " + path + " may not contain \"../\".");

        String fileName = getFileName(path);
        if (!fileName.equals(fileName.trim())) {
            throw new MCRException("File name '" + fileName + "' may not start or end with whitespace character.");
        }
    }

    static String getFileName(String path) {
        int pos = Math.max(path.lastIndexOf('\\'), path.lastIndexOf("/"));
        return path.substring(pos + 1);
    }

    static Transaction startTransaction() {
        LOGGER.debug("Starting transaction");
        return MCRHIBConnection.instance().getSession().beginTransaction();
    }

    static void commitTransaction(Transaction tx) {
        LOGGER.debug("Committing transaction");
        if (tx != null) {
            tx.commit();
            tx = null;
        } else {
            LOGGER.error("Cannot commit transaction. Transaction is null.");
        }
    }

    static void rollbackAnRethrow(Transaction tx, Exception e) throws Exception {
        LOGGER.debug("Rolling back transaction");
        if (tx != null) {
            tx.rollback();
            tx = null;
        } else {
            LOGGER.error("Error while rolling back transaction. Transaction is null.");
        }
        throw e;
    }
}