/*
 * $Id$
 * $Revision: 5697 $ $Date: Nov 21, 2012 $
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

package org.mycore.frontend.editor.validation;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mycore.frontend.editor.validation.value.MCRDateTimeValidator;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRDateTimeConverterTest {

    @Test
    public final void test() {
        MCRDateTimeValidator validator = new MCRDateTimeValidator();
        validator.setProperty("format", "yyyy-MM-dd;yyyy-MM-dd'T'HH:mm:ss'Z'");
        assertTrue("Date test did not pass.", validator.isValid("2012-11-24"));
        assertFalse("Date test did not pass.", validator.isValid("2012-1-24"));
        assertTrue("DateTime test did not pass.", validator.isValid("2012-11-24T12:00:00Z"));
    }

}
