/**
 * 
 */
package org.mycore.user2.login;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.mycore.common.config.MCRConfiguration;
import org.mycore.user2.MCRUser2Constants;

/**
 * @author Daniel Kirst (mcrdkirs)
 *
 */
public class MCRLDAPGroupPropertyMapper implements MCRLDAPGroupMapper {

	static final String CONFIG_GROUP_PREFIX = MCRUser2Constants.CONFIG_PREFIX
			+ "Shibboleth.eduPersonAffiliation.";

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mycore.user2.login.MCRLDAPGroupMapper#getRoles(java.lang.String)
	 */
	@Override
	public Set<String> getRoles(String... eduPersonAffiliations) {
		HashSet<String> roles = new HashSet<String>();
		for (String eduPersonAffiliation : eduPersonAffiliations) {
			roles.addAll(MCRConfiguration.instance().getStrings(
					CONFIG_GROUP_PREFIX + eduPersonAffiliation,
					Collections.<String> emptyList()));
		}
		return roles;
	}

}
