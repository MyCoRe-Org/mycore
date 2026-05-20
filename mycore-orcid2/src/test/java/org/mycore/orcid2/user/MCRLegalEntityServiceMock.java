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

    private static final String ATTR_ID_PREFIX = "id_";

    private MCRUser userMock;

    public void setUserMock(MCRUser userMock) {
        this.userMock = userMock;
    }

    @Override
    public Set<MCRIdentifier> getAllIdentifiers(MCRIdentifier identifier) {
        return userMock.getAttributes().stream()
            .map(a -> new MCRIdentifier(a.getName().startsWith(ATTR_ID_PREFIX)
                                        ? a.getName().substring(ATTR_ID_PREFIX.length()) : a.getName(), a.getValue()))
            .collect(Collectors.toSet());
    }

    @Override
    public void addIdentifier(MCRIdentifier primaryIdentifier, MCRIdentifier identifierToAdd) {
        userMock.getAttributes().add(new MCRUserAttribute(
            "id_" + identifierToAdd.getType(), identifierToAdd.getValue()));
    }
}
