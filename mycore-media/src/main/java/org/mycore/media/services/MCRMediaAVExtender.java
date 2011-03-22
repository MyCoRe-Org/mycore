package org.mycore.media.services;

import java.io.OutputStream;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;
import org.mycore.common.MCRPersistenceException;
import org.mycore.datamodel.ifs.MCRAudioVideoExtender;
import org.mycore.datamodel.ifs.MCRFileReader;
import org.mycore.media.MCRAudioObject;
import org.mycore.media.MCRMediaObject;
import org.mycore.media.MCRMediaParser;
import org.mycore.media.MCRVideoObject;

public class MCRMediaAVExtender extends MCRAudioVideoExtender {
    /** The logger */
    private final static Logger LOGGER = Logger.getLogger(MCRMediaAVExtender.class);
    
    /** The asset file this extender belongs to */
    protected MCRFileReader file;
    
    protected MCRMediaObject media;
    
    /**
     * Creates a new MCRAudioVideoExtender. The instance has to be initialized
     * by invoking init() before it can be used.
     */
    public MCRMediaAVExtender() {
    }
    
    /**
     * Initializes this AudioVideoExtender and gets technical metadata from the
     * server that holds the streaming asset. Subclasses must override this
     * method!
     * 
     * @param file
     *            the MCRFile that this extender belongs to
     */
    public void init(MCRFileReader file) throws MCRPersistenceException {
        this.file = file;
        
        try {
            StringTokenizer tok = new StringTokenizer( file.getID(), "_" );
            String documentID = tok.nextToken();
            String derivateID = tok.nextToken();
            
            org.jdom.Document mediaXML = null;
            LOGGER.info( "Get metadata for file " + file.getStoreID() + "@" + file.getStorageID() + "...");
            try {
                mediaXML = MCRMediaIFSTools.getMetadataFromStore(derivateID, file.getPath());
            } catch ( Throwable e ) {
                LOGGER.warn(e.getMessage());
            }
            
            if ( mediaXML != null ) {
                LOGGER.info( "use metadata from Metadata Store.");
                this.media = MCRMediaObject.buildFromXML(mediaXML.getRootElement());
            } else {
                LOGGER.info( "get metadata from stored file.");
                MCRMediaParser mparser = new MCRMediaParser();
                this.media = mparser.parse( file );
            }
            
            LOGGER.debug( media );
        } catch ( Exception ex ) {
            LOGGER.warn( ex.getMessage() );
            ex.printStackTrace();
        }
    }
    
    public MCRMediaObject getMediaObject() {
        return media;
    }
    
    public MCRVideoObject getVideoObject() {
        return ( isVideo() ? (MCRVideoObject)media : null );
    }
    
    public MCRAudioObject getAudioObject() {
        return ( isAudio() ? (MCRAudioObject)media : null );
    }
    
    public boolean isVideo() {
        return ( media != null ? media.getType() == MCRMediaObject.MediaType.VIDEO : false );
    }
    
    public boolean isAudio() {
        return ( media != null ? media.getType() == MCRMediaObject.MediaType.AUDIO : false );
    }
    
    public void getPlayerStarterTo(OutputStream out, String startPos, String stopPos) throws MCRPersistenceException {};
}
