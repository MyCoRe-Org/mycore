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

package org.mycore.common.function;

import java.nio.file.FileSystem;
import java.util.regex.PatternSyntaxException;

import org.mycore.datamodel.niofs.MCRAbstractFileSystem;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRFunctions {

    private static char END_OF_PATTERN = 0;

    private static final String GLOB_RESERVED_CHARS = "?*\\{[";

    private static final String REGEX_RESERVED_CHARS = "^$.{()+*[]|";

    /**
     * converts a Glob pattern into a regex pattern.
     * @param globPattern a pattern like <code>"/foo/*{@literal}/bar/*.txt"</code>
     * @see FileSystem#getPathMatcher(String) "glob" style
     */
    public static String convertGlobToRegex(final String globPattern) {
        boolean isInGroup = false;
        final StringBuilder regex = new StringBuilder("^");

        int nextPos = 0;
        while (nextPos < globPattern.length()) {
            final char c = globPattern.charAt(nextPos++);
            switch (c) {
                //Character handling
                //CSOFF: InnerAssignment
                case '\\' -> nextPos = escapeCharacter(regex, globPattern, nextPos);
                case '[' -> nextPos = addCharacterClass(regex, globPattern, nextPos);
                case '*' -> nextPos = handleWildcard(regex, globPattern, nextPos);
                case '?' -> regex.append("[^/]");

                //Group handling
                case '{' -> isInGroup = startGroup(regex, globPattern, nextPos, isInGroup);
                case '}' -> isInGroup = endGroup(regex, isInGroup);
                //CSON: InnerAssignment
                case ',' -> {
                    if (isInGroup) {
                        regex.append(")|(?:");//separate values
                    } else {
                        regex.append(',');
                    }
                }
                default -> {
                    if (isRegexReserved(c)) {
                        regex.append('\\');
                    }
                    regex.append(c);
                }
            }
        }

        if (isInGroup) {
            throw new PatternSyntaxException("Missing '}'.", globPattern, nextPos - 1);
        }

        return regex.append('$').toString();
    }

    private static int addCharacterClass(final StringBuilder regex, final String globPattern, int nextPos) {
        int currentIndex = getCurrentIndex(regex, globPattern, nextPos);
        boolean inRange = false;
        char rangeStartChar = 0;
        char curChar = '[';
        while (currentIndex < globPattern.length()) {
            curChar = globPattern.charAt(currentIndex++);

            switch (curChar) {
                case ']':
                    break;

                case MCRAbstractFileSystem.SEPARATOR:
                    throw new PatternSyntaxException("Character classes cannot cross directory boundaries.",
                        globPattern, currentIndex - 1);

                case '\\':
                case '[':
                    regex.append('\\');
                    regex.append(curChar);
                    break;

                case '&':
                    if (nextCharAt(globPattern, currentIndex) == '&') {
                        regex.append('\\');
                    }
                    regex.append(curChar);
                    break;

                case '-':
                    if (!inRange) {
                        throw new PatternSyntaxException("Invalid range.", globPattern, currentIndex - 1);
                    }
                    curChar = nextCharAt(globPattern, currentIndex++);
                    if (curChar == END_OF_PATTERN || curChar == ']') {
                        break;
                    }
                    if (curChar < rangeStartChar) {
                        throw new PatternSyntaxException("Invalid range.", globPattern, currentIndex - 3);
                    }
                    regex.append(curChar);
                    inRange = false;
                    break;

                default:
                    regex.append(curChar);
                    inRange = true;
                    rangeStartChar = curChar;
                    break;
            }
        }

        if (curChar != ']') {
            throw new PatternSyntaxException("Missing ']'.", globPattern, currentIndex - 1);
        }
        regex.append("]]");
        return currentIndex;
    }

    private static int getCurrentIndex(StringBuilder regex, String globPattern, int nextPos) {
        int currentIndex = nextPos;
        regex.append("[[^/]&&[");
        if (nextCharAt(globPattern, currentIndex) == '^') {
            // escape the regex negation char if it appears
            regex.append("\\^");
            currentIndex++;
        } else {
            // negation
            if (nextCharAt(globPattern, currentIndex) == '!') {
                regex.append('^');
                currentIndex++;
            }
            // hyphen allowed at start
            if (nextCharAt(globPattern, currentIndex) == '-') {
                regex.append('-');
                currentIndex++;
            }
        }
        return currentIndex;
    }

    private static boolean endGroup(final StringBuilder regex, boolean isInGroup) {
        if (isInGroup) {
            regex.append("))");
        } else {
            regex.append('}');
        }
        return false;
    }

    private static int escapeCharacter(final StringBuilder regex, final String globPattern, int nextPos) {
        if (nextPos == globPattern.length()) {
            throw new PatternSyntaxException("No character left to escape.", globPattern, nextPos - 1);
        }
        final char next = globPattern.charAt(nextPos + 1);
        if (isGlobReserved(next) || isRegexReserved(next)) {
            regex.append('\\');
        }
        regex.append(next);
        return nextPos;
    }

    private static int handleWildcard(final StringBuilder regex, final String globPattern, int nextPos) {
        if (nextCharAt(globPattern, nextPos) == '*') {
            //The ** characters matches zero or more characters crossing directory boundaries.
            regex.append(".*");
            return nextPos + 1;
        } else {
            //The * character matches zero or more characters of a name component without crossing directory boundaries
            regex.append("[^/]*");
            return nextPos;
        }
    }

    private static boolean isGlobReserved(final char c) {
        return GLOB_RESERVED_CHARS.indexOf(c) != -1;
    }

    private static boolean isRegexReserved(final char c) {
        return REGEX_RESERVED_CHARS.indexOf(c) != -1;
    }

    private static char nextCharAt(final String glob, final int i) {
        if (i < glob.length()) {
            return glob.charAt(i);
        }
        return END_OF_PATTERN;
    }

    private static boolean startGroup(final StringBuilder regex, final String globPattern, final int nextPos,
        final boolean isInGroup) {
        if (isInGroup) {
            throw new PatternSyntaxException("Nested groups are not supported.", globPattern, nextPos - 1);
        }
        regex.append("(?:(?:");
        return true;
    }

}
