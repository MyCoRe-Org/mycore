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

package org.mycore.datamodel.legalentity;

import jakarta.validation.constraints.NotNull;

import java.util.Set;

/**
 * Services that implement this interface should search for all identifiers of a specific legal entity (e.g. a person)
 * using a specific, identifying {@link MCRIdentifier}, or add an identifier to the legal entity. The identifier
 * can be any key-value pair that can uniquely identify a legal entity. The interface is intentionally generic to allow
 * different identifier schemes and lookup implementations.
 */
public interface MCRLegalEntityService {

    Set<MCRIdentifier> getAllIdentifiers(@NotNull MCRIdentifier identifier);

    Set<MCRIdentifier> getTypedIdentifiers(@NotNull MCRIdentifier primaryIdentifier, @NotNull String identifierType);

    void addIdentifier(@NotNull MCRIdentifier primaryIdentifier, @NotNull MCRIdentifier identifierToAdd);

}
