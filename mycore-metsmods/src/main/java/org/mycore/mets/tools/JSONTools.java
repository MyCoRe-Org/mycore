/* $Revision: 3033 $ 
 * $Date: 2010-10-22 13:41:12 +0200 (Fri, 22 Oct 2010) $ 
 * $LastChangedBy: thosch $
 * Copyright 2010 - Thüringer Universitäts- und Landesbibliothek Jena
 *  
 * Mets-Editor is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Mets-Editor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mets-Editor.  If not, see http://www.gnu.org/licenses/.
 */
package org.mycore.mets.tools;

/**
 * @author Silvio Hermann (shermann)
 * 
 */
public class JSONTools {
    /**
     * Removes brackets ('[' at the beginning and ']' at the end) from the given
     * string
     * 
     * @return the string without brackets
     */
    public static String stripBrackets(String input) {
        if (input.length() < 2) {
            return input;
        }
        if (input.startsWith("[") && input.endsWith("]")) {
            return input.substring(1, input.length() - 1);
        }
        return input;
    }

    public static String stripBracketsAndQuotes(String input) {
        input = stripBrackets(input);
        if (input.length() < 2) {
            return input;
        }
        if (input.startsWith("\"") && input.endsWith("\"") || input.startsWith("'") && input.endsWith("'")) {
            return input.substring(1, input.length() - 1);
        }
        return input;
    }
}
