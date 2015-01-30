package org.mycore.user2.login;

import java.util.Set;

public interface MCRLDAPGroupMapper {

	public abstract Set<String> getRoles(String... eduPersonAffiliations);

}