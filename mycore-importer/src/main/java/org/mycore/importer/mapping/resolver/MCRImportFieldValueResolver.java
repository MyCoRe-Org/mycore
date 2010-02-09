package org.mycore.importer.mapping.resolver;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.mycore.common.MCRTextResolver;
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

    private MCRTextResolver variableResolver;

    /**
     * Create a new field value resolver with a list of all fields which
     * can be resolved.
     * 
     * @param fieldList a list of fields which can be resolved
     */
    public MCRImportFieldValueResolver(List<MCRImportField> fieldList) {
        Hashtable<String, String> variablesTable = new Hashtable<String, String>();
        for(MCRImportField field : fieldList) {
            variablesTable.put(field.getId(), field.getValue());
        }
        variableResolver = new MCRTextResolver(variablesTable);
    }

    /**
     * This method resolves field identifiers at an incoming string.
     * 
     * @param incomingString the string which have to be resolved.
     */
    public String resolveFields(String incomingString) {
        return variableResolver.resolve(incomingString);
    }

    /**
     * Checks if all fields in the last incoming string are
     * fully resolved.
     * 
     * @return true if the string is successfully parsed and all
     * fields are resolved, otherwise false
     */
    public boolean isCompletelyResolved() {
        return variableResolver.isCompletelyResolved();
    }
    
    /**
     * Returns a list of all fields which are not used.
     * 
     * @return a list of not used fields
     */
    public List<MCRImportField> getNotUsedFields() {
        List<MCRImportField> notUsedFields = new ArrayList<MCRImportField>();
        for(Map.Entry<String, String> entry : variableResolver.getNotUsedVariables().entrySet()) {
            notUsedFields.add(new MCRImportField(entry.getKey(), entry.getValue()));
        }
        return notUsedFields;
    }
}