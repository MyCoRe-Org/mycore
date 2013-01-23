package org.mycore.importer.mapping.condition;

import org.jdom2.Element;
import org.mycore.importer.mapping.resolver.MCRImportFieldValueResolver;
import org.mycore.parsers.bool.MCRCondition;
import org.mycore.parsers.bool.MCRParseException;

public class MCRImportCondition implements MCRCondition<MCRImportFieldValueResolver> {

    private String value1;
    private String operator;
    private String value2;

    public MCRImportCondition(String value1, String operator, String value2) {
        this.value1 = value1;
        this.operator = operator;
        this.value2 = value2;
    }

    @Override
    public boolean evaluate(MCRImportFieldValueResolver res) {
        String resValue1 = res.resolveFields(this.value1);
        String resValue2 = res.resolveFields(this.value2);
        return compare(resValue1, resValue2);
    }

    private boolean compare(String v1, String v2) {
        if(this.operator.equals("=")) {
            return v1.equals(v2);
        } else if(this.operator.equals("!=")) {
            return !v1.equals(v2);
        } else if(this.operator.equals("contains")) {
            return v1.contains(v2);
        } else if(this.operator.matches(">|<|>=|<=")) {
            float floatValue1 = Float.parseFloat(v1);
            float floatValue2 = Float.parseFloat(v2);
            if(this.operator.equals(">"))
                return floatValue1 > floatValue2;
            if(this.operator.equals("<"))
                return floatValue1 < floatValue2;
            if(this.operator.equals(">="))
                return floatValue1 >= floatValue2;
            if(this.operator.equals("<="))
                return floatValue1 <= floatValue2;
        }
        throw new MCRParseException("Not a valid operator '" + operator + "'!");
    }

    @Override
    public Element toXML() {
        Element condition = new Element("condition");
        condition.setAttribute("value1", this.value1);
        condition.setAttribute("operator", this.operator);
        condition.setAttribute("value2", this.value2);
        return condition;
    }

}
