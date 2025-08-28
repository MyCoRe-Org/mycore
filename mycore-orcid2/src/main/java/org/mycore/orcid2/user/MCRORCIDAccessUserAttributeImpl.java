package org.mycore.orcid2.user;

import org.mycore.orcid2.util.MCRIdentifier;
import org.mycore.user2.MCRUser;
import org.mycore.user2.MCRUserAttribute;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static org.mycore.orcid2.user.MCRORCIDUser.ATTR_ID_PREFIX;
import static org.mycore.orcid2.user.MCRORCIDUser.ATTR_ORCID_ID;

public class MCRORCIDAccessUserAttributeImpl implements MCRORCIDAccess {

    @Override
    public void addORCID(String orcid, MCRUser user) {
        final MCRUserAttribute attribute = new MCRUserAttribute(ATTR_ORCID_ID, orcid);
        // allow more than one ORCID iD per user
        if (!user.getAttributes().contains(attribute)) {
            user.getAttributes().add(new MCRUserAttribute(ATTR_ORCID_ID, orcid));
        }
    }

    @Override
    public Set<String> getORCIDs(MCRUser user) {
        return user.getAttributes().stream()
            .filter(a -> Objects.equals(a.getName(), ATTR_ORCID_ID))
            .map(MCRUserAttribute::getValue).collect(Collectors.toSet());
    }

    @Override
    public Set<MCRIdentifier> getIdentifiers(MCRUser user) {
        return user.getAttributes().stream().filter(a -> a.getName().startsWith(ATTR_ID_PREFIX))
            .map(a -> new MCRIdentifier(a.getName().substring(ATTR_ID_PREFIX.length()), a.getValue()))
            .collect(Collectors.toSet());
    }
}
