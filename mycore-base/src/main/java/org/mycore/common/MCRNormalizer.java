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

package org.mycore.common;

import java.util.StringTokenizer;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.mycore.common.config.MCRConfiguration;

import com.ibm.icu.text.Normalizer;

/**
 * This class implements only static methods to normalize text values. Rules
 * written as x&gt;u .You can configure this normalization with following property
 * values<br>
 * <ul>
 * <li>MCR.Metadata.Normalize - true (standard) | false - switch normalization on or off</li>
 * <li>MCR.Metadata.Normalize.UseRuleFirst - true to run private rules before diacritics replacement | false (standard) run after diacritics replacement
 * <li>MCR.Metadata.Normalize.DiacriticRule true (standard) | false - switch diacritics replacement on or off
 * <li>MCR.Metadata.Normalize.AddRule - add more rules to the default rule</li>
 * <li>MCR.Metadata.Normalize.SetRule - replace the default rule</li>
 * </ul><br>
 * Here you can see how decomposition works:
 * http://www.icu-project.org/apiref/icu4j/com/ibm/icu/text/Normalizer.html <br>
 * These diacritics will be removed from letters when property is true: <br>
 * "\u0301", // &amp;#769; (0xcc 0x81 = 204 129) COMBINING ACUTE ACCENT <br>
 * "\u0300", // &amp;#768; (0xcc 0x80 = 204 128) COMBINING GRAVE ACCENT <br>
 * "\u0302", // &amp;#770; (0xcc 0x82 = 204 130) COMBINING CIRCUMFLEX ACCENT<br>
 * "\u0307", // &amp;#775; (0xcc 0x87 = 204 135) COMBINING DOT ABOVE <br>
 * "\u0308", // &amp;#776; (0xcc 0x88 = 204 136) COMBINING DIAERESIS <br>
 * "\u0306", // &amp;#774; (0xcc 0x86 = 204 134) COMBINING BREVE <br>
 * "\u030B", // &amp;#779; (0xcc 0x8b = 204 139) COMBINING DOUBLE ACUTE ACCENT<br>
 * "\u030C", // &amp;#780; (0xcc 0x8c = 204 140) COMBINING CARON (Hacek) <br>
 * "\u030A", // &amp;#778; (0xcc 0x8a = 204 138) COMBINING RING ABOVE <br>
 * "\u0304", // &amp;#772; (0xcc 0x84 = 204 132) COMBINING MACRON <br>
 * "\u032E", // &amp;#814; (0xcc 0xae = 204 174) COMBINING BREVE BELOW <br>
 * "\u0328", // &amp;#808; (0xcc 0xa8 = 204 168) COMBINING OGONEK <br>
 * "\u0327", // &amp;#807; (0xcc 0xa7 = 204 167) COMBINING CEDILLA <br>
 * "\u0323", // &amp;#803; (0xcc 0xa3 = 204 163) COMBINING DOT BELOW <br>
 * "\u0338", // &amp;#824; (0xcc 0xb8 = 204 184) COMBINING LONG SOLIDUS OVERLAY<br>
 * "\u0336", // &amp;#822; (0xcc 0xb6 = 204 182) COMBINING LONG STROKE OVERLAY<br>
 * "\u0332", // &amp;#818; (0xcc 0xb2 = 204 178) COMBINING LOW LINE <br>
 * "\u0303"};// &amp;#771; (0xcc 0x83 = 204 131) COMBINING TILDE <br>
 * 
 * @author Frank Lützenkirchen
 * @author Thomas Scheffler (yagee)
 * @author Jens Kupferschmidt
 * @author Harald Richter
 * 
 * @version $Revision$ $Date$
 */
public class MCRNormalizer {
    static Logger logger = Logger.getLogger(MCRNormalizer.class);

    /** List of characters that will be replaced */
    private static String rules = "\u00DF>ss \u00E4>ae \u00C4>ae \u00F6>oe \u00D6>oe \u00FC>ue \u00DC>ue"; // ss ae ae oe oe ue ue

    private static Pattern[] patterns;

    private static String[] replace;

    private static boolean normalize;

    private static boolean diacriticRule;

    private static boolean useRuleFirst;

    static {
        loadConfig();
    }
    
    public static void loadConfig() {
        MCRConfiguration config = MCRConfiguration.instance();
        normalize = config.getBoolean("MCR.Metadata.Normalize", true);
        String addRule = config.getString("MCR.Metadata.Normalize.AddRule", "");
        String setRule = config.getString("MCR.Metadata.Normalize.SetRule", "");
        diacriticRule = config.getBoolean("MCR.Metadata.Normalize.DiacriticRule", true);
        useRuleFirst = config.getBoolean("MCR.Metadata.Normalize.UseRuleFirst", false);
        
        if (setRule != null && setRule.trim().length() != 0) {
            rules = setRule;
        } else {
            if (addRule != null && addRule.trim().length() != 0) {
                rules = rules + " " + addRule;
            }

        }
        logger.debug("normalize rules --> " + rules);
        StringTokenizer tokens = new StringTokenizer(rules, " ");
        
        int numPatterns = tokens.countTokens();
        logger.debug("normalize count token --> " + numPatterns);
        patterns = new Pattern[numPatterns];
        replace = new String[numPatterns];
        for (int i = 0; i < numPatterns; i++) {
        	String token = tokens.nextToken();
        	int j = token.indexOf('>');
        	if ( j != -1 ) {
                patterns[i] = Pattern.compile(token.substring(0,j));
                if (j < token.length()) {
                    replace[i] = token.substring(j+1);
                } else {
                	replace[i] = " ";
                }
                logger.debug("normalize --> " + patterns[i] + " to --> " + replace[i]);
        	}
        }
    }

    /**
     * This method replaces umlauts and other special characters of languages
     * like german to normalized lowercase a-z characters.
     * 
     * @param in
     *            the String to be normalized
     * @return the normalized String in lower case.
     */
    public static String normalizeString(String in) {
    	
        if (!normalize) {
            return in;
        }
        
        if (in == null || in.trim().length() == 0) {
            return "";
        }
        
        String temp = in;
        if (useRuleFirst) {
            temp = doNormalize(temp);
        }

        if (diacriticRule) {
            temp = Normalizer.decompose(temp, false);

            String[] dia = { "\u0301", // &amp;#769; (0xcc 0x81 = 204 129)
                    // COMBINING ACUTE ACCENT
                    "\u0300", // &amp;#768; (0xcc 0x80 = 204 128)
                    // COMBINING GRAVE ACCENT
                    "\u0302", // &amp;#770; (0xcc 0x82 = 204 130)
                    // COMBINING CIRCUMFLEX ACCENT
                    "\u0307", // &amp;#775; (0xcc 0x87 = 204 135)
                    // COMBINING DOT ABOVE
                    "\u0308", // &amp;#776; (0xcc 0x88 = 204 136)
                    // COMBINING DIAERESIS
                    "\u0306", // &amp;#774; (0xcc 0x86 = 204 134)
                    // COMBINING BREVE
                    "\u030B", // &amp;#779; (0xcc 0x8b = 204 139)
                    // COMBINING DOUBLE ACUTE ACCENT
                    "\u030C", // &amp;#780; (0xcc 0x8c = 204 140)
                    // COMBINING CARON (Hacek)
                    "\u030A", // &amp;#778; (0xcc 0x8a = 204 138)
                    // COMBINING RING ABOVE
                    "\u0304", // &amp;#772; (0xcc 0x84 = 204 132)
                    // COMBINING MACRON
                    "\u032E", // &amp;#814; (0xcc 0xae = 204 174)
                    // COMBINING BREVE BELOW
                    "\u0328", // &amp;#808; (0xcc 0xa8 = 204 168)
                    // COMBINING OGONEK
                    "\u0327", // &amp;#807; (0xcc 0xa7 = 204 167)
                    // COMBINING CEDILLA
                    "\u0323", // &amp;#803; (0xcc 0xa3 = 204 163)
                    // COMBINING DOT BELOW
                    "\u0338", // &amp;#824; (0xcc 0xb8 = 204 184)
                    // COMBINING LONG SOLIDUS OVERLAY
                    "\u0336", // &amp;#822; (0xcc 0xb6 = 204 182)
                    // COMBINING LONG STROKE OVERLAY
                    "\u0332", // &amp;#818; (0xcc 0xb2 = 204 178)
                    // COMBINING LOW LINE
                    "\u0303" };// &amp;#771; (0xcc 0x83 = 204 131)
            // COMBINING TILDE

            for (String element : dia) {
                temp = MCRUtils.replaceString(temp, element, "");
            }
        }

        if (!useRuleFirst) {
            temp = doNormalize(temp);
        }

        return temp;
    }

    private static String doNormalize(String in) {
        in = in.toLowerCase().trim();
        for (int i = 0; i < patterns.length; i++) {
            in = patterns[i].matcher(in).replaceAll(replace[i]);
        }
        return in;
    }

}
