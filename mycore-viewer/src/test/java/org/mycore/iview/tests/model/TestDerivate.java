package org.mycore.iview.tests.model;

import java.io.IOException;
import java.net.URL;

/**
 * Abstract Class to resolve the Testfiles for a specific Test.
 * @author Sebastian Hofmann
 */
public abstract class TestDerivate {

    /**
     * @return gets the file wich should be show first
     */
    public abstract String getStartFile();

    /**
     * @return the location to zip file!
     * @throws IOException
     */
    public abstract URL getZipLocation() throws IOException;

    /**
     * Used to identify the TestDerivate for debugging 
     * @return a simple name
     */
    public abstract String getName();

}
