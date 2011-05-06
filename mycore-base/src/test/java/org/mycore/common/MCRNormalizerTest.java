/**
 * 
 * $Revision: 1.3 $ $Date: 2008/06/02 10:10:05 $
 *
 * This file is part of ** M y C o R e **
 * Visit our homepage at http://www.mycore.de/ for details.
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
 * along with this program, normally in the file license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 *
 **/

package org.mycore.common;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * This class is a JUnit test case for org.mycore.common.MCRNormalizer.
 * 
 * @author Jens Kupferschmidt
 * @version $Revision: 1.3 $ $Date: 2008/06/02 10:10:05 $
 * 
 */
public class MCRNormalizerTest extends MCRTestCase {

    /*
     * Test method for 'org.mycore.datamodel.metadata.MCRCalendar.getDateToFormattedString()'
     */
    @Test
    public void normalizeString() {
        String result_string;
        MCRConfiguration.instance().set("MCR.Metadata.Normalize", true);
        MCRConfiguration.instance().set("MCR.Metadata.Normalize.UseRuleFirst", false);
        MCRConfiguration.instance().set("MCR.Metadata.Normalize.DiacriticRule", true);
        MCRConfiguration.instance().set("MCR.Metadata.Normalize.AddRule", "");
        MCRConfiguration.instance().set("MCR.Metadata.Normalize.SetRule", "");
        MCRNormalizer.loadConfig();
        
        result_string = MCRNormalizer.normalizeString(null);
        assertEquals("input String is null", "", result_string);
        
        result_string = MCRNormalizer.normalizeString("");
        assertEquals("input String is empty", "", result_string);
        
        result_string = MCRNormalizer.normalizeString("abc ÄöÜß ñ");
        assertEquals("wrong output string", "abc aouss n", result_string);
        
        MCRConfiguration.instance().set("MCR.Metadata.Normalize.UseRuleFirst", true);
        MCRNormalizer.loadConfig();
        result_string = MCRNormalizer.normalizeString("abc ÄöÜß ñ");
        assertEquals("wrong output string", "abc aeoeuess n", result_string);
        
        MCRConfiguration.instance().set("MCR.Metadata.Normalize.DiacriticRule", false);
        MCRNormalizer.loadConfig();
        result_string = MCRNormalizer.normalizeString("abc ÄöÜß ñ");
        assertEquals("wrong output string", "abc aeoeuess ñ", result_string);
        
        MCRConfiguration.instance().set("MCR.Metadata.Normalize.AddRule", "ñ>x");
        MCRNormalizer.loadConfig();
        result_string = MCRNormalizer.normalizeString("abc ÄöÜß ñ");
        assertEquals("wrong output string", "abc aeoeuess x", result_string);
        
        MCRConfiguration.instance().set("MCR.Metadata.Normalize.AddRule", "é>");
        MCRNormalizer.loadConfig();
        result_string = MCRNormalizer.normalizeString("abc ÄöÜß éñ");
        assertEquals("wrong output string", "abc aeoeuess x", result_string);
        
        MCRConfiguration.instance().set("MCR.Metadata.Normalize.SetRule", "ß>sz");
        MCRNormalizer.loadConfig();
        result_string = MCRNormalizer.normalizeString("abc ÄöÜß ñ");
        assertEquals("wrong output string", "abc äöüsz ñ", result_string);
        
        MCRConfiguration.instance().set("MCR.Metadata.Normalize", false);
        MCRNormalizer.loadConfig();
        result_string = MCRNormalizer.normalizeString("abc ÄöÜß ñ");
        assertEquals("wrong output string", "abc ÄöÜß ñ", result_string);
        }
}