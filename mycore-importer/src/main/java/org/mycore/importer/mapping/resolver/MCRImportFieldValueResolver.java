package org.mycore.importer.mapping.resolver;

import java.util.ArrayList;
import java.util.List;

import org.mycore.importer.MCRImportField;

/**
 * <p>
 * The field value resolver tries to parse and resolve an incoming string
 * which contains field variables. At the constructor a list of all possible
 * fields has to be set. Only these fields could be resolved. If a field
 * is used it will be added to the usedFields list. To resolving a string
 * a valid syntax is required.
 * </p>
 * <p>
 * <b>{}:</b> Use curly brackets to set field variables. For example "{var1}" 
 * </p>
 * <p>
 * <b>[]:</b> Use squared brackets to define a field condition. All data which
 * is set in squared brackes is only used if the internal fields are
 * not null and not empty. For example "[hello {lastName}]" is only resolved
 * if the value of "lastName" is not null and not empty. 
 * </p>
 * <p>
 * <b>\:</b> Use the escaping char to use all predefined characters.
 * </p>
 * 
 * @author Matthias Eichner
 */
public class MCRImportFieldValueResolver {

    protected List<MCRImportField> fieldList;
    protected List<MCRImportField> usedFields;

    /**
     * Create a new field value resolver with a list of all fields which
     * can be resolved.
     * 
     * @param fieldList a list of fields which can be resolved
     */
    public MCRImportFieldValueResolver(List<MCRImportField> fieldList) {
        this.fieldList = fieldList;
        this.usedFields = new ArrayList<MCRImportField>(); 
    }

    /**
     * This method resolves field identifiers at an incoming string.
     * The synatax is described at the head of the class.
     * 
     * @param incomingString the string which have to be resolved.
     */
    public String resolveFields(String incomingString) {
        StringBuffer returnBuffer = new StringBuffer();
        resolveFields(incomingString, returnBuffer, 0);
        return returnBuffer.toString();
    }

    private int resolveFields(String incomingString, StringBuffer returnBuffer, int startPos) {
        StringBuffer currentVar = new StringBuffer();
        boolean isInVar = false;
        boolean varResolved = true;
        for(int i = startPos; i < incomingString.length(); i++) {
            char c = incomingString.charAt(i);
            if(isInVar == false) {
                if(c == '{')
                    isInVar = true;
                else if(c == '[') {
                    // call this methode recursive for resolving []-brackets
                    StringBuffer tempBuffer = new StringBuffer();
                    i = resolveFields(incomingString, tempBuffer, i + 1);
                    if(tempBuffer.length() != 0)
                        returnBuffer.append(tempBuffer);
                } else if(c == ']') {
                    if(varResolved == false)
                        returnBuffer.setLength(0);
                    return i;
                } else if(c == '\\') {
                    // escape char
                    if(i + 1 < incomingString.length()) {
                        returnBuffer.append(incomingString.charAt(i + 1));
                        i++;
                    }
                } else
                    returnBuffer.append(c);
            } else {
                if(c != '}')
                    currentVar.append(c);
                else {
                    // resolve var
                    MCRImportField field = getField(currentVar.toString());
                    if(field != null && field.getValue() != null && !field.getValue().equals("")) {
                        returnBuffer.append( field.getValue() );
                        // set the field as used
                        usedFields.add(field);
                    } else
                        varResolved = false;
                    // go out of the var and reset the currentVar string buffer
                    isInVar = false;
                    currentVar = new StringBuffer();
                }
            }
        }
        return -1;
    }

    /**
     * Returns a field from the field list by the 
     * given field id.
     * 
     * @param fieldId the id of the field
     * @return the field from the field list, or null
     * 
     */
    public MCRImportField getField(String fieldId) {
        for(MCRImportField field : fieldList) {
            if(fieldId.equals(field.getId()))
                return field;
        }
        return null;
    }

    /**
     * Returns a list of all fields which are not used.
     * 
     * @return a list of not used fields
     */
    public List<MCRImportField> getNotUsedFields() {
        List<MCRImportField> notUsedFields =  new ArrayList<MCRImportField>(fieldList.size());
        notUsedFields.addAll(fieldList);
        notUsedFields.removeAll(usedFields);
        return notUsedFields;
    }

    /**
     * Returns a list of all fields which are used.
     * 
     * @return a list of used fields.
     */
    public List<MCRImportField> getUsedFields() {
        return usedFields;
    }
}
