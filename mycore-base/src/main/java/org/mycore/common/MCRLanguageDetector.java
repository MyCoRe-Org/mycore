/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
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

package org.mycore.common;

import java.lang.Character.UnicodeScript;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Detects the language of a given text string by
 * looking for typical words and word endings and used characters for each language.
 * German, english, french, arabic, chinese, japanese, greek and hebrew are currently supported.
 *
 * @author Frank Lützenkirchen
 */
public class MCRLanguageDetector {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final Properties WORDS = new Properties();

    private static final Properties ENDINGS = new Properties();

    private static final Map<UnicodeScript, String> CODE_2_LANGUAGE_CODES = new EnumMap<>(UnicodeScript.class);

    static {
        CODE_2_LANGUAGE_CODES.put(UnicodeScript.ARABIC, "ar");
        CODE_2_LANGUAGE_CODES.put(UnicodeScript.GREEK, "el");
        CODE_2_LANGUAGE_CODES.put(UnicodeScript.HAN, "zh");
        CODE_2_LANGUAGE_CODES.put(UnicodeScript.HEBREW, "he");
        CODE_2_LANGUAGE_CODES.put(UnicodeScript.HIRAGANA, "ja");
        CODE_2_LANGUAGE_CODES.put(UnicodeScript.KATAKANA, "ja");

        WORDS.put("de", "als am auch auf aus bei bis das dem den der deren derer des dessen"
            + " die dies diese dieser dieses ein eine einer eines einem für"
            + " hat im ist mit sich sie über und vom von vor wie zu zum zur");
        WORDS.put("en",
            "a and are as at do for from has have how its like new of on or the their through to with you your");
        WORDS.put("fr", "la le les un une des, à aux de pour par sur comme aussi jusqu'à"
            + " jusqu'aux quel quels quelles laquelle lequel lesquelles"
            + " lesquelles auxquels auxquelles avec sans ont sont duquel desquels desquelles quand");

        ENDINGS.put("en", "ar ble cal ce ced ed ent ic ies ing ive ness our ous ons ral th ure y");
        ENDINGS.put("de", "ag chen gen ger iche icht ig ige isch ische ischen kar ker"
            + " keit ler mus nen ner rie rer ter ten trie tz ung yse");
        ENDINGS.put("fr", "é, és, ée, ées, euse, euses, ème, euil, asme, isme, aux");
    }

    private static int buildScore(String text, String lang, String wordList, String endings) {
        String cleanedText = text.toLowerCase(Locale.ROOT).trim();
        cleanedText = cleanedText
            .replace(',', ' ')
            .replace('-', ' ')
            .replace('/', ' ');
        cleanedText = " " + cleanedText + " ";

        int score = 0;

        StringTokenizer st = new StringTokenizer(wordList, " ");
        while (st.hasMoreTokens()) {
            String word = st.nextToken();
            int pos = cleanedText.indexOf(" " + word + " ");
            while (pos >= 0) {
                score += 2;
                int fromIndex = Math.min(pos + word.length() + 1, cleanedText.length());
                pos = cleanedText.indexOf(" " + word + " ", fromIndex);
            }
        }

        st = new StringTokenizer(endings, " ");
        while (st.hasMoreTokens()) {
            String ending = st.nextToken();
            if (cleanedText.contains(ending + " ")) {
                score += 1;
            }
            int pos = cleanedText.indexOf(ending + " ");
            while (pos >= 0) {
                score += 1;
                int fromIndex = Math.min(pos + ending.length() + 1, cleanedText.length());
                pos = cleanedText.indexOf(ending + " ", fromIndex);
            }
        }

        LOGGER.debug("Score {} = {}", lang, score);
        return score;
    }

    public static String detectLanguageByCharacter(String text) {
        if (text == null || text.isEmpty()) {
            LOGGER.warn("The text for language detection is null or empty");
            return null;
        }
        LOGGER.debug("Detecting language of [{}]", text);

        Map<UnicodeScript, AtomicInteger> scores = new EnumMap<>(UnicodeScript.class);
        buildScores(text, scores);
        UnicodeScript code = getCodeWithMaxScore(scores);

        return CODE_2_LANGUAGE_CODES.getOrDefault(code, null);
    }

    private static void buildScores(String text, Map<UnicodeScript, AtomicInteger> scores) {
        try {
            char[] chararray = text.toCharArray();
            for (int i = 0; i < text.length(); i++) {
                UnicodeScript code = UnicodeScript.of(Character.codePointAt(chararray, i));
                increaseScoreFor(scores, code);
            }
        } catch (Exception ignored) {
        }
    }

    private static void increaseScoreFor(Map<UnicodeScript, AtomicInteger> scores, UnicodeScript code) {
        scores.computeIfAbsent(code, k -> new AtomicInteger()).incrementAndGet();
    }

    private static UnicodeScript getCodeWithMaxScore(Map<UnicodeScript, AtomicInteger> scores) {
        UnicodeScript maxCode = null;
        int maxScore = 0;
        for (UnicodeScript code : scores.keySet()) {
            int score = scores.get(code).get();
            if (score > maxScore) {
                maxScore = score;
                maxCode = code;
            }
        }
        return maxCode;
    }

    /**
     * Detects the language of a given text string.
     *
     * @param text the text string
     * @return the language code: de, en, fr, ar ,el, zh, he, jp or null
     */
    public static String detectLanguage(String text) {
        LOGGER.debug("Detecting language of [{}]", text);

        String bestLanguage = detectLanguageByCharacter(text);

        if (bestLanguage == null) {
            int bestScore = 0;
            for (String language : WORDS.stringPropertyNames()) {
                String wordList = WORDS.getProperty(language);
                String endingList = ENDINGS.getProperty(language);

                int score = buildScore(text, language, wordList, endingList);
                if (score > bestScore) {
                    bestLanguage = language;
                    bestScore = score;
                }
            }
        }

        LOGGER.debug("Detected language = {}", bestLanguage);
        return bestLanguage;
    }
}
