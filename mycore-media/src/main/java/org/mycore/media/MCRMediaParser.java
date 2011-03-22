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
import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.mycore.common.MCRConfiguration;
import org.mycore.datamodel.ifs.MCROldFile;
import org.mycore.datamodel.ifs.MCRFileReader;
import org.mycore.util.ProcessWrapper;

import com.sun.jna.Platform;

/**
 * Get technical metadata from Audio or Video file. 
 * 
 * @author René Adler (Eagle)
 * 
 */
public class MCRMediaParser {
    private static final Logger LOGGER = Logger.getLogger( MCRMediaParser.class );
    
    private static final NativeLibExporter libExporter = NativeLibExporter.getInstance();
    
    private static MCRConfiguration config;
    
    protected MCRMediaInfoParser mparser;
    
    static {
        //export ffmpeg app to classpath
        try {
            if ( Platform.isMac() )
                libExporter.exportLibrary( "lib/darwin/ffmpeg" );
            else if ( Platform.isWindows() )
                libExporter.exportLibrary( "lib/win32/ffmpeg.exe" );
            else if ( Platform.isLinux() && !isFFMpegInstalled() )
                LOGGER.warn("Please install ffmpeg on your system.");
        } catch ( Throwable e ) {
            LOGGER.warn("Couldn't export ffmpeg to classpath!");
        }
    }
    
    public MCRMediaParser() {
        mparser = new MCRMediaInfoParser();
        config  = MCRConfiguration.instance();
    }
    
    
    /**
     * Checks if given file is supported.
     * 
     * @param File file
     * @return boolean if true
     */
    public boolean isFileSupported( File file ) {
        if ( mparser.isValid() )
            return mparser.isFileSupported( file );
        else {
            MCRMediaViewSourceParser vsparser = new MCRMediaViewSourceParser();
            return vsparser.isFileSupported( file );
        }
    }
    
    /**
     * Checks if given file is supported.
     * 
     * @param MCROldFile file
     * @return boolean if true
     */
    public boolean isFileSupported( MCROldFile file ) {
        return isFileSupported( toFile(file) );
    }
    
    /**
     * Checks if given file is supported.
     * 
     * @param MCRFile file
     * @return boolean if true
     */
    public boolean isFileSupported( org.mycore.datamodel.ifs.MCRFile file ) {
        return isFileSupported( toFile(file) );
    }
    
    /**
     * Checks if given file is supported.
     * 
     * @param MCRFileReader file
     * @return boolean if true
     */
    public boolean isFileSupported( MCRFileReader file ) {
        return isFileSupported( toFile(file) );
    }
    
    /**
     * Parse MediaInfo of the given file and store metadata in related Object.
     * 
     * @return MCRMediaObject
     *              can be held any MCRMediaObject
     * @see MCRMediaObject#clone()
     */
    private synchronized MCRMediaObject parse( File file, String vsURL ) throws Exception {
        MCRMediaObject media = new MCRMediaObject();
        
        if ( mparser.isValid() ) {
            try {
                LOGGER.info( "Try to get MediaInfo with MediaInfo lib..." );
                media = mparser.parse( file );
            } catch ( Throwable e ) {
                LOGGER.warn( "Use less accurate ViewSource parsing method." );
                LOGGER.warn( "Failed with: " + e.getMessage() );
                
                try {
                    media = parseWithViewSource( vsURL, file );
                } catch ( Exception ex ) {
                    LOGGER.warn( ex.getMessage() );
                }
            } finally {
                //Close MediaInfo lib.
                mparser.close();
            }
        } else {
            try {
                media = parseWithViewSource( vsURL, file );
            } catch ( Exception ex ) {
                LOGGER.warn( ex.getMessage() );
            }
        }
        
        return media;
    }
    
    /**
     * Parse MediaInfo of the given file and store metadata in related Object.
     * 
     * @param MCROldFile file
     * @return MCRMediaObject
     *              can be held any MCRMediaObject
     * @see MCRMediaObject#clone()
     */
    public synchronized MCRMediaObject parse( MCROldFile file ) throws Exception {
        return parse( toFile( file ), buildViewSourceURL(file) );
    }
    
    /**
     * Parse MediaInfo of the given file and store metadata in related Object.
     * 
     * @param MCRFile file
     * @return MCRMediaObject
     *              can be held any MCRMediaObject
     * @see MCRMediaObject#clone()
     */
    public synchronized MCRMediaObject parse( org.mycore.datamodel.ifs.MCRFile file ) throws Exception {
        return parse( toFile( file ), buildViewSourceURL(file) );
    }
    
    /**
     * Parse MediaInfo of the given file and store metadata in related Object.
     * 
     * @param MCRFileReader file
     * @return MCRMediaObject
     *              can be held any MCRMediaObject
     * @see MCRMediaObject#clone()
     */
    public synchronized MCRMediaObject parse( MCRFileReader file ) throws Exception {
        return parse( toFile( file ), buildViewSourceURL(file) );
    }
    
    /**
     * Builds ViewSource URL from configuration.
     * 
     * @param MCROldFile file
     * @return String
     * @throws Exception
     */
    private String buildViewSourceURL( MCROldFile file ) throws Exception {
        String prefix = "MCR.IFS.AVExtender." + file.getStoreID() + ".";
        String baseMetadata = config.getString(prefix + "ViewSourceBaseURL");
        
        return baseMetadata + file.getStorageID();
    }
    
    /**
     * Builds ViewSource URL from configuration.
     * 
     * @param MCROldFile file
     * @return String
     * @throws Exception
     */
    private String buildViewSourceURL( org.mycore.datamodel.ifs.MCRFile file ) throws Exception {
        String prefix = "MCR.IFS.AVExtender." + file.getStoreID() + ".";
        String baseMetadata = config.getString(prefix + "ViewSourceBaseURL");
        
        return baseMetadata + file.getStorageID();
    }
    
    /**
     * Builds ViewSource URL from configuration.
     * 
     * @param MCRFileReader file
     * @return String
     * @throws Exception
     */
    private String buildViewSourceURL( MCRFileReader file ) throws Exception {
        String prefix = "MCR.IFS.AVExtender." + file.getStoreID() + ".";
        String baseMetadata = config.getString(prefix + "ViewSourceBaseURL");
        
        return baseMetadata + file.getStorageID();
    }
    
    private File toFile( MCROldFile file ) {
        String storeURI = config.getString("MCR.IFS.ContentStore." + file.getStoreID() + ".URI");
        
        return new File( storeURI + "/" + file.getStorageID() );
    }
    
    private File toFile( org.mycore.datamodel.ifs.MCRFile file ) {
        String storeURI = config.getString("MCR.IFS.ContentStore." + file.getStoreID() + ".URI");
        
        return new File( storeURI + "/" + file.getStorageID() );
    }
    
    private File toFile( MCRFileReader file ) {
        String storeURI = config.getString("MCR.IFS.ContentStore." + file.getStoreID() + ".URI");
        
        return new File( storeURI + "/" + file.getStorageID() );
    }
    
    private MCRMediaObject parseWithViewSource( String vsURL, File file ) throws Exception {
        MCRMediaObject media = new MCRMediaObject();
        
        try {
            MCRMediaViewSourceParser vsparser = new MCRMediaViewSourceParser();
            media = vsparser.parse( vsURL );
            media.fileName = file.getName();
            String path = file.getAbsolutePath();
            media.folderName = path.substring(path.indexOf(file.getName()));
        } catch ( Exception e ) {
            throw new Exception( e );
        }
        
        return media;
    }
    
    /**
     * Checks if ffmpeg is installed btw. exported to classpath.
     * 
     * @return boolean if is
     */
    public static boolean isFFMpegInstalled() {
        ProcessWrapper pw = new ProcessWrapper();
        
        try {
            return pw.runCommand("ffmpeg") == 1;
        } catch ( Exception e ) {
            return false;
        }
    }
    
    /**
     * Take a Snapshot from a supported MediaObject.
     * 
     * @param media
     *              the MediaObject
     * @param seek
     *              position to take a snapshot
     * @param maxWidth
     *              maximum output width
     * @param maxHeight
     *              maximum output height
     * @param keepAspect
     *              set to keep aspect ratio
     * @return
     *              the snapshot
     * @throws Exception
     */
    public synchronized static byte[] getThumbnail( MCRMediaObject media, long seek, int maxWidth, int maxHeight, boolean keepAspect ) throws Exception {
        if ( media instanceof MCRVideoObject ) {
            byte[] thumb = ((MCRVideoObject)media).getThumbnail( ((MCRVideoObject)media), seek, maxWidth, maxHeight, keepAspect );
            
            return thumb;
        } else {
            throw new Exception("The " + media.getClass().getName() + " hasn't support for getThumbnail");
        }
    }
    
    /**
     * Take a Snapshot at the half from a supported MediaObject.
     * 
     * @param media
     * @return
     *              the snapshot
     * @throws Exception
     * @see #getThumbnail( MCRMediaObject , long, int, int, boolean )
     */
    public synchronized static byte[] getThumbnail( MCRMediaObject media ) throws Exception {
        return getThumbnail( media, (media.duration / 1000) / 2, 0, 0, true );
    }
    
    /**
     * Take multiple Snapshot from a supported MediaObject.
     * 
     * @param media
     * @param steps
     *              count of steps to take a snapshot
     * @return ArrayList<bytes[]>
     *              a Array of snapshots  
     * @throws Exception
     * @see #getThumbnail( MCRMediaObject , long, int, int, boolean )
     */
    public synchronized static ArrayList<byte[]> getThumbnail( MCRMediaObject media, int steps ) throws Exception {
        ArrayList<byte[]> thumbs = new ArrayList<byte[]>();
        
        for ( int c=0; c<steps; c++ ) {
            long seek = (media.duration / steps) * c;
            seek = ( (seek + 5000) < media.duration ? seek + 5000 : seek ) / 1000;
            byte[] thumb = getThumbnail( media, seek, 0, 0, true );
            thumbs.add(thumb);
        }
        
        return thumbs;
    }
}
