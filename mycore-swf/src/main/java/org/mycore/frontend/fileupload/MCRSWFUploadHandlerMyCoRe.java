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

package org.mycore.frontend.fileupload;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.StringTokenizer;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.mycore.common.MCRUtils;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.frontend.workflow.MCRSimpleWorkflowManager;

/**
 * This class stores files uploaded from the client applet as derivates into the
 * workflow.
 * 
 * @author Harald Richter
 * @author Jens Kupferschmidt
 * @author Frank Lützenkirchen
 * 
 * @version $Revision$ $Date$
 * 
 * @see MCRUploadHandler
 * @deprecated
 */
public class MCRSWFUploadHandlerMyCoRe extends MCRUploadHandler {
    private static Logger LOGGER = Logger.getLogger(MCRSWFUploadHandlerIFS.class);

    private String mainfile = "";

    private File dirname;

    private String docId;

    private String derId;

    /**
     * Creates MCRUploadHandler for MyCoRe
     * 
     * @param docId
     *            document to which derivate belongs
     * @param derId
     *            derivate used to add files, if id="0" a new derivate is
     *            created
     * @param mode
     *            "append" add files to derivate, replace old files "replace"
     *            add files to derivate, delete old files "create" add files to
     *            new derivate
     * @param url
     *            when MCRUploadApplet is finished this url will be shown
     */
    public MCRSWFUploadHandlerMyCoRe(String docId, String derId, String mode, String url) {
        this.url = url;
        LOGGER.debug("MCRUploadHandlerMyCoRe DocID: " + docId + " DerId: " + derId + " Mode: " + mode);

        try {
            this.docId = MCRObjectID.getInstance(docId).toString();
        } catch (Exception e) {
            LOGGER.debug("Error while creating MCRObjectID : " + docId, e);
        }

        try {
            this.derId = MCRObjectID.getInstance(derId).toString();
        } catch (Exception e) {
            LOGGER.debug("Error while creating MCRObjectID : " + derId, e);
        }
    }

    /**
     * Start Upload for MyCoRe
     */
    public void startUpload(int numFiles) throws Exception {
        MCRObjectID ID = MCRObjectID.getInstance(derId);
        File workdir = MCRSimpleWorkflowManager.instance().getDirectoryPath(ID.getBase());
        dirname = new File(workdir, derId);
    }

    public long receiveFile(String path, InputStream in, long length, String md5) throws Exception {
        // prepare to save
        LOGGER.debug("Upload file path: " + path);

        // convert path
        String fname = path.replace(' ', '_');
        try {
            if (!dirname.isDirectory()) {
                dirname.mkdir();
                LOGGER.debug("Create directory " + dirname);
            }
        } catch (Exception e) {
        }
        File newdir = dirname;
        StringTokenizer st = new StringTokenizer(fname, "/");
        int i = st.countTokens();
        int j = 0;

        while (j < (i - 1)) {
            newdir = new File(newdir, st.nextToken());
            j++;

            try {
                if (!newdir.isDirectory()) {
                    newdir.mkdir();
                    LOGGER.debug("Create directory " + newdir);
                }
            } catch (Exception e) {
            }
        }

        String newfile = st.nextToken();

        // store file
        File fout = new File(newdir, newfile);

        try (BufferedOutputStream fouts = new BufferedOutputStream(new FileOutputStream(fout))) {
            IOUtils.copy(in, fouts);
            LOGGER.info("Data object stored under " + fout.getName());
        } catch (IOException e) {
            LOGGER.error("Can't store the data object " + fout.getName());
        }

        // set mainfile
        if (mainfile.length() == 0) {
            mainfile = fname;
        }

        long myLength = fout.length();
        if (myLength >= length)
            return myLength;
        else {
            fout.delete(); // Incomplete file transfer, user canceled upload
            return 0;
        }
    }

    /**
     * Finish upload, store derivate
     * 
     */
    public void finishUpload() throws Exception {
        // check for content
        if (dirname.list().length == 0) {
            dirname.delete();
            LOGGER.warn("No file were uploaded, delete directory " + dirname + " and return.");
            return;
        }
        // add the mainfile entry
        MCRDerivate der;
        try {
            try {
                File derXMLFile = new File(dirname.getAbsolutePath() + ".xml");
                der = new MCRDerivate(derXMLFile.toURI());
            } catch (Exception e) {
                der = MCRSimpleWorkflowManager.instance().createDerivate(MCRObjectID.getInstance(docId), MCRObjectID.getInstance(derId));
            }

            if (der.getDerivate().getInternals().getMainDoc().length() == 0) {
                der.getDerivate().getInternals().setMainDoc(mainfile);

                byte[] outxml = MCRUtils.getByteArray(der.createXML());

                try {
                    FileOutputStream out = new FileOutputStream(dirname.getAbsolutePath() + ".xml");
                    out.write(outxml);
                    out.flush();
                } catch (IOException ex) {
                    LOGGER.error("Exception while store to file " + dirname + ".xml", ex);
                }
            }
        } catch (Exception e) {
            LOGGER.error("while add mainfile entry", e);
        }
    }
}
