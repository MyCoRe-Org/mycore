/*
 * $RCSfile$
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

import org.mycore.common.MCRConfiguration;
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
 * @author Frank L\u00fctzenkirchen
 * 
 * @version $Revision$ $Date$
 * 
 * @see MCRUploadHandler
 */
public class MCRUploadHandlerMyCoRe extends MCRUploadHandler {
    private String mainfile = "";

    private String dirname;

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
    public MCRUploadHandlerMyCoRe(String docId, String derId, String mode, String url) {
        this.url = url;
        logger.debug("MCRUploadHandlerMyCoRe DocID: " + docId + " DerId: " + derId + " Mode: " + mode);

        try {
            new MCRObjectID(docId);
            this.docId = docId;
        } catch (Exception e) {
            logger.debug("Error while creating MCRObjectID : "+docId,e);
        }

        try {
            new MCRObjectID(derId);
            this.derId = derId;
        } catch (Exception e) {
            logger.debug("Error while creating MCRObjectID : "+derId,e);
        }
    }

    /**
     * Start Upload for MyCoRe
     */
    public void startUpload(int numFiles) throws Exception {
        MCRObjectID ID = new MCRObjectID(docId);
        MCRConfiguration config = MCRConfiguration.instance();
        String workdir = config.getString("MCR.SWF.Directory." + ID.getTypeId() , "/");
        dirname = workdir + File.separator + derId;
    }

    public long receiveFile(String path, InputStream in, long length, String md5) throws Exception {
        // prepare to save
        logger.debug("Upload file path: " + path);

        // convert path
        String fname = path.replace(' ', '_');
        File fdir = null;
        try {
            fdir = new File(dirname);
            if (!fdir.isDirectory()) {
                fdir.mkdir();
                logger.debug("Create directory " + dirname);
            }
        } catch (Exception e) {
        }
        String newdir = dirname;
        StringTokenizer st = new StringTokenizer(fname, "/");
        int i = st.countTokens();
        int j = 0;

        while (j < (i - 1)) {
            newdir = newdir + File.separatorChar + st.nextToken();
            j++;

            try {
                fdir = new File(newdir);
                if (!fdir.isDirectory()) {
                    fdir.mkdir();
                    logger.debug("Create directory " + newdir);
                }
            } catch (Exception e) {
            }
        }

        String newfile = st.nextToken();

        // store file
        File fout = new File(newdir, newfile);

        try {
            BufferedOutputStream fouts = new BufferedOutputStream(new FileOutputStream(fout));
            MCRUtils.copyStream(in, fouts);
            fouts.close();
            logger.info("Data object stored under " + fout.getName());
        } catch (IOException e) {
            logger.error("Can't store the data object " + fout.getName());
        }

        // set mainfile
        if (mainfile.length() == 0) {
            mainfile = fname;
        }
        
        long myLength = fout.length();
        if( myLength >= length )
          return myLength;
        else
        {
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
        File fdir = new File(dirname);
        if (fdir.list().length == 0) {
            fdir.delete();
            logger.warn("No file were uploaded, delete directory " + dirname+" and return.");
            return;
        }
        // add the mainfile entry
        MCRDerivate der = new MCRDerivate();
        try {
            try {
            der.setFromURI(dirname + ".xml");
            } catch (Exception e) {
                der = MCRSimpleWorkflowManager.instance().createDerivate(new MCRObjectID(docId), new MCRObjectID(derId));
            }

            if (der.getDerivate().getInternals().getMainDoc().length() == 0) {
                der.getDerivate().getInternals().setMainDoc(mainfile);

                byte[] outxml = MCRUtils.getByteArray(der.createXML());

                try {
                    FileOutputStream out = new FileOutputStream(dirname + ".xml");
                    out.write(outxml);
                    out.flush();
                } catch (IOException ex) {
                    logger.error(ex.getMessage());
                    logger.error("Exception while store to file " + dirname + ".xml");
                }
            }
        } catch (Exception e) {
        }
    }
}
