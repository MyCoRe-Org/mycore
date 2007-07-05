package org.mycore.services.acl;

import org.mycore.backend.hibernate.tables.MCRACCESS;
import org.mycore.backend.hibernate.tables.MCRACCESSRULE;

public class MCRACLAccessView extends MCRACCESS{
	private MCRACCESSRULE rule;

	public MCRACCESSRULE getRule() {
		return rule;
	}

	public void setRule(MCRACCESSRULE rule) {
		this.rule = rule;
	}
	
}
