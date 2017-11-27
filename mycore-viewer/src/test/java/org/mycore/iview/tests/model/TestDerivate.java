/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

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
