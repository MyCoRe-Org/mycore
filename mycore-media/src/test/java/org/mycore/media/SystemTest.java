package org.mycore.media;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.mycore.media.MCRMediaInfoParser;

/**
 * Unit test of system components.
 */
public class SystemTest extends TestCase
{
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public SystemTest( String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( SystemTest.class );
    }

    /**
     * Test installation of MediaInfo library btw. the extraction
     */
    public void testMediaInfoLib()
    {
        /*MCRMediaInfoParser mparser = new MCRMediaInfoParser();
        
        assertTrue( "MediaInfo Library seams to not been installed.", mparser.isValid() );*/
    }
    
    /**
     * Test installation of FFMpeg btw. the extraction
     */
    public void testFFMpeg()
    {
        //assertTrue( "FFMpeg seams to not been installed.", MCRMediaParser.isFFMpegInstalled() );
    }
}
