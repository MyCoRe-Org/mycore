/*
 * $Id$
 * $Revision: 5697 $ $Date: 11.11.2011 $
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

package org.mycore.backend.lucene;

import static org.junit.Assert.*;

import java.text.ParseException;
import java.util.Date;

import org.junit.Test;
import org.mycore.common.MCRTestCase;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRLuceneToolsTest extends MCRTestCase {

    /**
     * Test method for {@link org.mycore.backend.lucene.MCRLuceneTools#getLongValue(java.lang.String)}.
     * @throws ParseException 
     */
    @Test
    public final void testGetLongValue() throws ParseException {
        testSmaller("2011-11-10", "2011-11-11");
        testSmaller("2011-11-11T11:11:10Z", "2011-11-11T11:11:11Z");
        testSmaller("11:11:10", "11:11:11");
    }

    private void testSmaller(String first, String second) throws ParseException {
        assertTrue(first + " does not result in a smaller long value than " + second, MCRLuceneTools.getLongValue(first) < MCRLuceneTools.getLongValue(second));
    }

}
