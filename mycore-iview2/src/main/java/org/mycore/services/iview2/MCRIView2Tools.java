/*
 * $Id$
 * $Revision: 5697 $ $Date: 19.10.2009 $
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

package org.mycore.services.iview2;

import java.io.File;

import org.mycore.common.MCRConfiguration;
import org.mycore.datamodel.ifs.MCRDirectory;
import org.mycore.datamodel.ifs.MCRFile;
import org.mycore.datamodel.ifs.MCRFilesystemNode;
import org.mycore.datamodel.metadata.MCRDerivate;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRIView2Tools {

    private static final MCRConfiguration CONFIG = MCRConfiguration.instance();

    private static String SUPPORTED_CONTENT_TYPE = CONFIG.getString("MCR.Module-iview2.SupportedContentTypes", "");
    
    private static File TILE_DIR = new File(MCRIview2Props.getProperty("DirectoryForTiles"));

    /**
     * @return directory for tiles
     */
    public static File getTileDir() {
        return TILE_DIR;
    }

    public static String getSupportedMainFile(String derivateID) {
        MCRDerivate deriv = new MCRDerivate();
        deriv.receiveFromDatastore(derivateID);
        String nameOfMainFile = deriv.getDerivate().getInternals().getMainDoc();
        // verify support
        if (nameOfMainFile != null && !nameOfMainFile.equals("")) {
            MCRFile mainFile = getMCRFile(derivateID, nameOfMainFile);
            if (isFileSupported(mainFile))
                return mainFile.getAbsolutePath();
        }
        return "";
    }

    public static File getFile(String derivateID, String absolutePath) {
        MCRFile mcrFile = getMCRFile(derivateID, absolutePath);
        return getFile(mcrFile);
    }

    public static String getFilePath(String derivateID, String absolutePath) {
        MCRFile mcrFile = getMCRFile(derivateID, absolutePath);
        return mcrFile.getStorageID();
    }

    static MCRFile getMCRFile(String derivateID, String absolutePath) {
        MCRDirectory root = (MCRDirectory) MCRFilesystemNode.getRootNode(derivateID);
        // get main file
        MCRFile mainFile = (MCRFile) root.getChildByPath(absolutePath);
        return mainFile;
    }

    public static boolean isDerivateSupported(String derivateID) {
        return getSupportedMainFile(derivateID).length() > 0;
    }

    public static boolean isFileSupported(MCRFile file) {
        return SUPPORTED_CONTENT_TYPE.indexOf(file.getContentTypeID()) > -1;
    }

    static File getFile(MCRFile image) {
        String storageID = image.getStorageID();
        String storeID = image.getStoreID();
        String baseDirName = MCRConfiguration.instance().getString("MCR.IFS.ContentStore." + storeID + ".URI");
        File file = new File(baseDirName + File.separatorChar + storageID);
        return file;
    }

}
