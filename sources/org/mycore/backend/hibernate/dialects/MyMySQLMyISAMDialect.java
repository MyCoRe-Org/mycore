package org.mycore.backend.hibernate.dialects;

import org.hibernate.dialect.MySQLMyISAMDialect;
import org.hibernate.dialect.function.SQLFunction;

public class MyMySQLMyISAMDialect extends MySQLMyISAMDialect {

	public MyMySQLMyISAMDialect() {
		super();
		registerFunction("match_against", new MatchAgainstFunction() );
	}

}
