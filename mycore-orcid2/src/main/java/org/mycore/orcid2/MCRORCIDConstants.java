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

package org.mycore.orcid2;

import java.util.List;

import org.mycore.common.config.MCRConfiguration2;

/**
 * Provides general constants.
 */
public class MCRORCIDConstants {

    /**
     * Config prefix of mycore-orcid2 properties.
     */
    public static final String CONFIG_PREFIX = "MCR.ORCID2.";

    /**
     * ORCID Base URL.
     */
    public static final String ORCID_BASE_URL = MCRConfiguration2.getStringOrThrow(CONFIG_PREFIX + "BaseURL");

    /**
     * List of all language codes supported by ORCID.
     */
    public static final List<String> SUPPORTED_LANGUAGE_CODES
        = MCRConfiguration2.getString(CONFIG_PREFIX + "SupportedLanguageCodes").stream()
        .flatMap(MCRConfiguration2::splitValue).toList();
}
