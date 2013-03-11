package org.mycore.importer.mapping.resolver;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.mycore.common.MCRTextResolver;
import org.mycore.common.MCRTextResolver.ResolveDepth;
import org.mycore.importer.MCRImportField;

/**
 * <p>
 * The field value resolver tries to parse and resolve an incoming string
 * which contains field variables. At the constructor a list of all possible
 * fields has to be set. Only these fields could be resolved. If a field
 * is used it will be added to the usedFields list. To resolving a string
 * a valid syntax is required (see <code>MCRTextResolver</code>).
 * </p>
 * 
 * @see MCRTextResolver
 * @author Matthias Eichner
 */
public class MCRImportFieldValueResolver {

    private MCRTextResolver textResolver;

    /**
     * Create a new field value resolver with a list of all fields which
     * can be resolved.
     * 
     * @param fieldList a list of fields which can be resolved
     */
    public MCRImportFieldValueResolver(List<MCRImportField> fieldList) {
        Hashtable<String, String> varTable = new Hashtable<String, String>();
        for(MCRImportField field : fieldList) {
            String baseId = field.getBaseId();
            if(field.getValue() != null)
                varTable.put(baseId, field.getValue());
            parseSubFields(field, varTable, baseId);
        }
        this.textResolver = new MCRTextResolver(varTable, ResolveDepth.NoVariables, false);
    }

    private void parseSubFields(MCRImportField parentField, Map<String, String> varTable, String base) {
        for(MCRImportField childField : parentField.getSubFieldList()) {
            StringBuilder id = new StringBuilder(base);
            id.append(parentField.getSeperator());
            id.append(childField.getId());
            if(childField.getValue() != null)
                varTable.put(id.toString(), childField.getValue());
            parseSubFields(childField, varTable, id.toString());
        }
    }
    
    /**
     * This method resolves field identifiers at an incoming string.
     * 
     * @param incomingString the string which have to be resolved.
     */
    public String resolveFields(String incomingString) {
        if(incomingString == null)
            throw new NullPointerException();
        return textResolver.resolveNext(incomingString);
    }

    /**
     * Checks if all fields in the last incoming string are
     * fully resolved.
     * 
     * @return true if the string is successfully parsed and all
     * fields are resolved, otherwise false
     */
    public boolean isCompletelyResolved() {
        return textResolver.isCompletelyResolved();
    }
    
    /**
     * Returns a list of all fields which are not used.
     * 
     * @return a list of not used fields
     */
    public List<MCRImportField> getNotUsedFields() {
        List<MCRImportField> notUsedFields = new ArrayList<MCRImportField>();
        for(Map.Entry<String, String> entry : textResolver.getNotUsedVariables().entrySet()) {
            notUsedFields.add(new MCRImportField(entry.getKey(), entry.getValue()));
        }
        return notUsedFields;
    }
}