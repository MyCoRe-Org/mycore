/*
 * $Id$
 * $Revision: 5697 $ $Date: 14.01.2010 $
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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.mycore.common.MCRException;
import org.mycore.common.MCRTestCase;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRUploadHelperTest extends MCRTestCase {

    @Test
    public void checkPathName() {
        String prefix = "junit";
        String suffix = "test..file";
        String[] genDelims = new String[] { ":", "?", "#", "[", "]", "@" };
        String[] subDelims = new String[] { "!", "$", "&", "'", "(", ")", "*", "+", ",", ";", "=" };
        List<String> genDelimTestNames = new ArrayList<String>(genDelims.length);
        for (String delim : genDelims) {
            genDelimTestNames.add(prefix + delim + suffix);
        }
        List<String> subDelimTestNames = new ArrayList<String>(subDelims.length);
        for (String delim : subDelims) {
            subDelimTestNames.add(prefix + delim + suffix);
        }
        for (String testPath : genDelimTestNames) {
            boolean failed = false;
            try {
                MCRUploadHelper.checkPathName(testPath);
            } catch (MCRException e) {
                Logger.getLogger(MCRUploadHelperTest.class).debug("Test successfully failed", e);
                failed = true;
            }
            assertTrue("Path " + testPath + " did not fail in gen-delims test.", failed);
        }
        for (String testPath : subDelimTestNames) {
            boolean failed = false;
            try {
                MCRUploadHelper.checkPathName(testPath);
            } catch (MCRException e) {
                Logger.getLogger(MCRUploadHelperTest.class).debug("Test successfully failed", e);
                failed = true;
            }
            assertTrue("Path " + testPath + " did not fail in sub-delims test.", failed);
        }
        boolean failed = false;
        String testPath = prefix + " " + suffix;
        try {
            MCRUploadHelper.checkPathName(testPath);
        } catch (MCRException e) {
            Logger.getLogger(MCRUploadHelperTest.class).info("Test failed", e);
            failed = true;
        }
        assertFalse("Path " + testPath + " did fail non reserved character test.", failed);
        //http://sourceforge.net/p/mycore/bugs/668/
        failed = false;
        testPath = "../../" + prefix + suffix;
        try {
            MCRUploadHelper.checkPathName(testPath);
        } catch (MCRException e) {
            Logger.getLogger(MCRUploadHelperTest.class).debug("Test successfully failed", e);
            failed = true;
        }
        assertTrue("Path " + testPath + " did not fail jail break test #668.", failed);
    }

}
