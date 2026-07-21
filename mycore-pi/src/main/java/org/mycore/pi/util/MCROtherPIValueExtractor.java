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

import org.mycore.common.MCRException;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.pi.MCRPIManager;
import org.mycore.pi.MCRPIRegistrationInfo;

public class MCROtherPIValueExtractor {

    private final String type;

    private final String service;

    private final Pattern pattern;

    public MCROtherPIValueExtractor(String type, String service, String pattern) {
        this.type = Objects.requireNonNull(type);
        this.service = Objects.requireNonNull(service);
        this.pattern = Pattern.compile(Objects.requireNonNull(pattern));
        checkPattern();
    }

    private void checkPattern() {

        Matcher matcher = pattern.matcher("");

        boolean hasExactlyOneCaptureGroup = matcher.groupCount() == 1;
        if (!hasExactlyOneCaptureGroup) {
            throw new MCRConfigurationException("Pattern doesn't have exactly one capture group: " +
                pattern.pattern());
        }

    }

    public String extractValue(MCRObjectID objectID) {

        List<MCRPIRegistrationInfo> createdIdentifiers = MCRPIManager.getInstance()
            .getCreatedIdentifiers(objectID, type, service);

        if (createdIdentifiers.isEmpty()) {
            throw new MCRException("No identifier found object " + objectID + ", type " + type +
                " and service " + service);
        }

        String identifier = createdIdentifiers.getFirst().getIdentifier();
        Matcher matcher = pattern.matcher(identifier);

        if (!matcher.find()) {
            throw new MCRException("Identifier " + identifier + " found for object " + objectID +
                ", type " + type + " and service " + service + " doesn't match pattern "
                + pattern.pattern());
        }

        String value = matcher.group(1);

        if (value.isEmpty()) {
            throw new MCRException("Identifier " + identifier + " found for object " + objectID +
                ", type " + type + " and service " + service + " contains empty value for pattern "
                + pattern.pattern());
        }

        return value;

    }

}
