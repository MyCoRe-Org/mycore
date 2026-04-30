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

import java.util.Set;

import org.mycore.common.config.MCRConfiguration2;

/**
 * Services that implement this interface should search for all identifiers of a specific legal entity (e.g. a person)
 * using a specific, identifying {@link MCRIdentifier}, or add an identifier to the legal entity. The identifier
 * can be any key-value pair that can uniquely identify a legal entity. The interface is intentionally generic to allow
 * different identifier schemes and lookup implementations.
 */
public interface MCRLegalEntityService {

    /**
     * Finds all identifiers of a legal entity determined by a specific identifier.
     * @param identifier unique identifier of legal entity, not null
     * @return a set of identifiers a legal entity owns
     */
    Set<MCRIdentifier> findAllIdentifiers(MCRIdentifier identifier);

    /**
     * Adds an identifier to a legal entity. The entity is determined by a specific, given identifier
     * @param primaryIdentifier unique identifier of legal entity, not null
     * @param identifierToAdd the identifier to add, not null
     */
    void addIdentifier(MCRIdentifier primaryIdentifier, MCRIdentifier identifierToAdd);

    /**
     * Get configured singleton service implementation.
     */
    static MCRLegalEntityService obtainInstance() {
        return InstanceHolder.SHARED_INSTANCE;
    }

    class InstanceHolder {
        private static final MCRLegalEntityService SHARED_INSTANCE = MCRConfiguration2.getInstanceOfOrThrow(
            MCRLegalEntityService.class, "MCR.LegalEntityService.Class");
    }

}
