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

package org.mycore.media;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import com.sun.jna.Platform;

/**
 * Used as exporter for the OS depended libraries.
 * 
 * @author René Adler (Eagle)
 *
 */
public class NativeLibExporter {
    private static NativeLibExporter instance = null;

    private File libraryFile;

    /**
     * @return an instance of this class.
     */
    public static NativeLibExporter getInstance() {
        if (instance == null) {
            instance = new NativeLibExporter();

            try {
                if (Platform.isMac())
                    instance.exportLibrary("lib/darwin/libmediainfo.dylib");
                else if (Platform.isWindows() && Platform.is64Bit())
                    instance.exportLibrary("lib/win64/MediaInfo64.dll");
                else if (Platform.isWindows())
                    instance.exportLibrary("lib/win32/MediaInfo.dll");
            } catch (Throwable e) {
                System.err.println(e.getMessage());
            }
        }

        return instance;
    }

    public boolean isValid() {
        return (instance.libraryFile != null || Platform.isLinux());
    }

    public void exportLibrary(final String libraryName) throws Exception {
        String fName = new File(libraryName).getName();

        InputStream inputStream = instance.getClass().getClassLoader().getResourceAsStream(libraryName);
        instance.libraryFile = new File(fName);
        instance.libraryFile.deleteOnExit();
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(instance.libraryFile);
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) > 0) {
                fileOutputStream.write(buffer, 0, bytesRead);
            }
            fileOutputStream.close();
            inputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
