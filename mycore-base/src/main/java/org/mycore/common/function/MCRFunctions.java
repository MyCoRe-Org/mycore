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
     * @return 
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
        regex.append("[[^/]&&[");
        if (nextCharAt(globPattern, nextPos) == '^') {
            // escape the regex negation char if it appears
            regex.append("\\^");
            nextPos++;
        } else {
            // negation
            if (nextCharAt(globPattern, nextPos) == '!') {
                regex.append('^');
                nextPos++;
            }
            // hyphen allowed at start
            if (nextCharAt(globPattern, nextPos) == '-') {
                regex.append('-');
                nextPos++;
            }
        }
        boolean inRange = false;
        char rangeStartChar = 0;
        char curChar = '[';
        while (nextPos < globPattern.length()) {
            curChar = globPattern.charAt(nextPos++);
            if (curChar == ']') {
                break;
            }
            if (curChar == MCRAbstractFileSystem.SEPARATOR) {
                throw new PatternSyntaxException("Chracter classes cannot cross directory boundaries.", globPattern,
                    nextPos - 1);
            }
            if (curChar == '\\' || curChar == '[' || curChar == '&' && nextCharAt(globPattern, nextPos) == '&') {
                // escape '\', '[' or "&&"
                regex.append('\\');
            }
            regex.append(curChar);

            if (curChar == '-') {
                if (!inRange) {
                    throw new PatternSyntaxException("Invalid range.", globPattern, nextPos - 1);
                }
                curChar = nextCharAt(globPattern, nextPos++);
                if (curChar == END_OF_PATTERN || curChar == ']') {
                    break;
                }
                if (curChar < rangeStartChar) {
                    throw new PatternSyntaxException("Invalid range.", globPattern, nextPos - 3);
                }
                regex.append(curChar);
                inRange = false;
            } else {
                inRange = true;
                rangeStartChar = curChar;
            }
        }
        if (curChar != ']') {
            throw new PatternSyntaxException("Missing ']'.", globPattern, nextPos - 1);
        }
        regex.append("]]");
        return nextPos;
    }

    private static boolean endGroup(final StringBuilder regex, boolean isInGroup) {
        if (isInGroup) {
            isInGroup = false;
            regex.append("))");
        } else {
            regex.append('}');
        }
        return isInGroup;
    }

    private static int escapeCharacter(final StringBuilder regex, final String globPattern, int nextPos) {
        if (nextPos == globPattern.length()) {
            throw new PatternSyntaxException("No character left to escape.", globPattern, nextPos - 1);
        }
        final char next = globPattern.charAt(nextPos++);
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
            nextPos++;
        } else {
            //The * character matches zero or more characters of a name component without crossing directory boundaries
            regex.append("[^/]*");
        }
        return nextPos;
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
