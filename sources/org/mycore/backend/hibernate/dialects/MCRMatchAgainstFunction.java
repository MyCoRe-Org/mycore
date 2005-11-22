package org.mycore.backend.hibernate.dialects;

import java.util.List;

import org.hibernate.QueryException;
import org.hibernate.dialect.function.SQLFunction;
import org.hibernate.engine.Mapping;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.type.Type;

public class MCRMatchAgainstFunction implements SQLFunction{

	public String render(List args, SessionFactoryImplementor sfi) throws QueryException {
        StringBuffer buf = new StringBuffer();

        buf.append("MATCH(");
        for ( int i=0; i<args.size()-1; i++) {
            buf.append(args.get(i));
            if (i<args.size()-2) buf.append(",");
        }
        buf.append(")");
       
        buf.append(" AGAINST(");
        buf.append(args.get(args.size()-1));
        buf.append(")");
       
        return buf.toString(); 
	}

	public Type getReturnType(Type type, Mapping mapping) throws QueryException {
		return type;
	}

	public boolean hasArguments() {
		return true;
	}

	public boolean hasParenthesesIfNoArguments() {
		return true;
	}

}
