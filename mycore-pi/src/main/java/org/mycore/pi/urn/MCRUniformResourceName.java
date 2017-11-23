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

package org.mycore.pi.urn;

import org.mycore.pi.MCRPersistentIdentifier;

public class MCRUniformResourceName implements MCRPersistentIdentifier {
    public static final String PREFIX = "urn:";

    public String getPREFIX() {
        return PREFIX;
    }

    protected MCRUniformResourceName() {
    }

    public MCRUniformResourceName(String subNamespace, String namespaceSpecificString) {
        this.subNamespace = subNamespace;
        this.namespaceSpecificString = namespaceSpecificString;
    }

    protected String subNamespace;

    protected String namespaceSpecificString;

    public String getSubNamespace() {
        return subNamespace;
    }

    public String getNamespaceSpecificString() {
        return namespaceSpecificString;
    }

    @Override
    public String asString() {
        return getPREFIX() + getSubNamespace() + getNamespaceSpecificString();
    }
}
