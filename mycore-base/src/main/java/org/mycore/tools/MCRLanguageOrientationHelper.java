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
package org.mycore.tools;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.mycore.common.config.MCRConfiguration2;

public class MCRLanguageOrientationHelper {

    static Set<String> RTL_LANGUAGES = getRTLLanguages();

    private static Set<String> getRTLLanguages() {
        return MCRConfiguration2.getString("MCR.I18N.RtlLanguageList")
            .map(MCRConfiguration2::splitValue)
            .orElse(Stream.empty())
            .collect(Collectors.toSet());
    }

    public static boolean isRTL(String language) {
        return RTL_LANGUAGES.contains(language);
    }

}
