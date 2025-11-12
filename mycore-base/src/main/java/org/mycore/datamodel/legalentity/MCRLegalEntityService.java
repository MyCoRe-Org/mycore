package org.mycore.datamodel.legalentity;

import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Services that implement this interface should search for all identifiers of a specific legal entity (e.g. a person)
 * using a specific, identifying {@link MCRIdentifier}, or add an identifier to the legal entity. The identifier
 * can be any key-value pair that can uniquely identify a legal entity. The interface is intentionally generic to allow
 * different identifier schemes and lookup implementations.
 */
public interface MCRLegalEntityService {

    public List<MCRIdentifier> getAllIdentifiers(@NotNull MCRIdentifier identifier);

    public void addIdentifier(@NotNull MCRIdentifier primaryIdentifier, @NotNull MCRIdentifier identifierToAdd);

}
