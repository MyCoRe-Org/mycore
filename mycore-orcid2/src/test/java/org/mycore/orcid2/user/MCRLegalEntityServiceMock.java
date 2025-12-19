package org.mycore.orcid2.user;

import org.mycore.datamodel.legalentity.MCRIdentifier;
import org.mycore.datamodel.legalentity.MCRLegalEntityService;
import org.mycore.user2.MCRUser;
import org.mycore.user2.MCRUserAttribute;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * For testing by using an internal {@link MCRUser userMock}.
 */
public class MCRLegalEntityServiceMock implements MCRLegalEntityService {

    private MCRUser userMock;

    public MCRUser getUserMock() {
        return this.userMock;
    }

    public void setUserMock(MCRUser userMock) {
        this.userMock = userMock;
    }

    @Override
    public Set<MCRIdentifier> getAllIdentifiers(MCRIdentifier identifier) {
        return userMock.getAttributes().stream()
            .map(attr -> new MCRIdentifier(attr.getName(), attr.getValue()))
            .collect(Collectors.toSet());
    }

    @Override
    public Set<MCRIdentifier> getTypedIdentifiers(MCRIdentifier primaryIdentifier, String identifierType) {
        return userMock.getAttributes().stream()
            .filter(attr -> attr.getName().substring("_id".length()).equals(identifierType))
            .map(attr -> new MCRIdentifier(attr.getName(), attr.getValue()))
            .collect(Collectors.toSet());
    }

    @Override
    public void addIdentifier(MCRIdentifier primaryIdentifier, MCRIdentifier identifierToAdd) {
        userMock.getAttributes().add(new MCRUserAttribute(
            "id_" + identifierToAdd.getType(), identifierToAdd.getValue()));
    }
}
