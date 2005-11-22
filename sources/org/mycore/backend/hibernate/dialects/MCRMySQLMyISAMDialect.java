package org.mycore.backend.hibernate.dialects;

import org.hibernate.dialect.MySQLMyISAMDialect;
import org.hibernate.dialect.function.SQLFunction;

public class MCRMySQLMyISAMDialect extends MySQLMyISAMDialect {

	public MCRMySQLMyISAMDialect() {
		super();
		registerFunction("match_against", new MCRMatchAgainstFunction() );
	}

}
