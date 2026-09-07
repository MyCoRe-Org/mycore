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

package org.mycore.pi.util;

import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.datamodel.metadata.MCRBase;
import org.mycore.pi.MCRPIService;
import org.mycore.pi.MCRPIServiceManager;
import org.mycore.pi.MCRPersistentIdentifier;
import org.mycore.pi.backend.MCRPI;
import org.mycore.pi.exceptions.MCRPersistentIdentifierException;

public final class MCROtherPIValueExtractor {

    private static final Logger LOGGER = LogManager.getLogger();

    private final String service;

    private final Pattern pattern;

    public MCROtherPIValueExtractor(String service, String pattern) {
        this.service = Objects.requireNonNull(service);
        this.pattern = checkPattern(Pattern.compile(Objects.requireNonNull(pattern)));
    }

    private static Pattern checkPattern(Pattern pattern) {
        Matcher matcher = pattern.matcher("");
        if (matcher.groupCount() != 1) {
            throw new MCRConfigurationException("Pattern doesn't have exactly one capture group: " + pattern.pattern());
        }
        return pattern;
    }

    public String extractValue(MCRBase base) throws MCRPersistentIdentifierException {

        MCRPIServiceManager serviceManager = MCRPIServiceManager.getInstance();
        MCRPIService<MCRPersistentIdentifier> service = serviceManager.getRegistrationService(this.service);
        List<MCRPI> pis = MCRPIService.getFlags(base, "", service);

        if (pis.isEmpty()) {
            throw new MCRPersistentIdentifierException("No identifier found for " + base
                + " and service " + this.service);
        }

        if (pis.size() != 1 && LOGGER.isWarnEnabled()) {
            LOGGER.warn("Service flags for {} contain more than one flag for service {}", base, this.service);
        }

        String identifier = pis.getFirst().getIdentifier();
        Matcher matcher = pattern.matcher(identifier);

        if (!matcher.find()) {
            throw new MCRPersistentIdentifierException("Identifier " + identifier + ", found for " + base
                + " and service " + this.service + ", doesn't match pattern " + pattern.pattern());
        }

        String value = matcher.group(1);

        if (value.isEmpty()) {
            throw new MCRPersistentIdentifierException("Identifier " + identifier + ", found for " + base
                + " and service " + this.service + ", contains empty value for pattern " + pattern.pattern());
        }

        return value;

    }

}
