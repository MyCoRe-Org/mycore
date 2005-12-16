/*
 * $RCSfile$
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

import java.util.*;

import org.apache.log4j.Logger;

/**
 * Detects the language of a given text string by 
 * looking for typical words and word endings for each language.
 * German, englisch and french are currently supported.
 * 
 * @author Frank Lützenkirchen
 * @version $Revision$ $Date$
 */
public class MCRLanguageDetector {
    private static Logger LOGGER = Logger.getLogger(MCRLanguageDetector.class);

    private static Properties words = new Properties();

    private static Properties endings = new Properties();

    static {
        words.put("de", "als am auch auf aus bei bis das dem den der deren derer des dessen die dies diese dieser dieses ein eine einer eines einem für hat im ist mit sich sie über und vom von vor wie zu zum zur");
        words.put("en", "a and are as at do for from has have how its like new of on or the their through to with you your");
        words.put("fr", "la le les un une des, à aux de pour par sur comme aussi jusqu'à jusqu'aux quel quels quelles laquelle lequel lesquelles lesquelles auxquels auxquelles avec sans ont sont duquel desquels desquelles quand");

        endings.put("en", "ar ble cal ce ced ed ent ic ies ing ive ness our ous ons ral th ure y");
        endings.put("de", "ag chen gen ger iche icht ig ige isch ische ischen kar ker keit ler mus nen ner rie rer ter ten trie tz ung yse");
        endings.put("fr", "é, és, ée, ées, euse, euses, ème, euil, asme, isme, aux");
    }

    private static int buildScore(String text, String lang, String wordList, String endings) {
        text = text.toLowerCase().trim();
        text = text.replace(',', ' ').replace('-', ' ').replace('/', ' ');
        text = " " + text + " ";

        int score = 0;

        StringTokenizer st = new StringTokenizer(wordList, " ");
        while (st.hasMoreTokens()) {
            String word = st.nextToken();
            int pos = 0;
            while ((pos = text.indexOf(" " + word + " ", pos)) >= 0) {
                score += 2;
                pos = Math.min(pos + word.length() + 1, text.length());
            }
        }

        st = new StringTokenizer(endings, " ");
        while (st.hasMoreTokens()) {
            String ending = st.nextToken();

            if (text.indexOf(ending + " ") >= 0)
                score += 1;
            int pos = 0;
            while ((pos = text.indexOf(ending + " ", pos)) >= 0) {
                score += 1;
                pos = Math.min(pos + ending.length() + 1, text.length());
            }
        }

        LOGGER.debug("Score " + lang + " = " + score);
        return score;
    }

    /**
     * Detects the language of a given text string.
     * 
     * @param text the text string
     * @return the language code: de, en, fr or null
     */
    public static String detectLanguage(String text) {
        LOGGER.debug("Detecting language of [" + text + "]");

        String bestLanguage = null;
        int bestScore = 0;

        Enumeration languages = words.keys();
        while (languages.hasMoreElements()) {
            String language = (String) (languages.nextElement());
            String wordList = words.getProperty(language);
            String endingList = endings.getProperty(language);

            int score = buildScore(text, language, wordList, endingList);
            if (score > bestScore) {
                bestLanguage = language;
                bestScore = score;
            }
        }

        LOGGER.debug("Detected language = " + bestLanguage);
        return bestLanguage;
    }
}
