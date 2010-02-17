package org.mycore.common;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * This class parses and resolve strings which contains variables and
 * MyCoRe properties. To add variables call <code>addVariable</code>.
 * To disable MyCoRe property resolving call <code>setUseMCRProperties(false)</code>.
 * </p><p>
 * The algorithm has been optimized so that each character is touched only once.
 * </p><p>
 * To resolve a string a valid syntax is required:
 * </p><p>
 * <b>{}:</b> Use curly brackets for variables or properties. For example "{var1}"
 * or "{MCR.basedir}" 
 * </p><p>
 * <b>[]:</b> Use squared brackets to define a condition. All data which
 * is set in squared brackets is only used if the internal variables are
 * not null and not empty. For example "[hello {lastName}]" is only resolved
 * if the value of "lastName" is not null and not empty. Otherwise the whole
 * content in the squared brackets are ignored.
 * </p><p>
 * <b>\:</b> Use the escape character to use all predefined characters.
 * </p>
 * <p>
 * Sample:</br>
 * "Lastname: {lastName}[, Firstname: {firstName}]"<br/>
 * </p>
 * 
 * @author Matthias Eichner
 */
public class MCRTextResolver {

    /**
     * If MyCoRe properties are resolved. For example {MCR.basedir}.
     * By default this is true.
     */
    protected boolean useMCRProperties;

    /**
     * This hash table contains all variables that can be resolved.
     */
    protected Hashtable<String, String> variablesTable;

    protected Hashtable<String, String> resolvedVariables;
    protected List<String> unresolvedVariables;

    /**
     * Creates a new text resolver. To add variables call
     * <code>addVariable</code>, otherwise only MyCoRe property
     * resolving is possible.
     */
    public MCRTextResolver() {
        this(new Hashtable<String, String>());
    }

    /**
     * Creates a new text resolver with a hash table of variables.
     * 
     * @param variablesTable a hash table of variables
     */
    public MCRTextResolver(Hashtable<String, String> variablesTable) {
        this.variablesTable = variablesTable;
        this.resolvedVariables = new Hashtable<String, String>();
        this.unresolvedVariables = new ArrayList<String>();
        this.useMCRProperties = true;
    }

    /**
     * Adds a new variable to the resolver. This overwrites a
     * existing variable with the same name.
     * 
     * @param name name of the variable
     * @param value value of the variable
     * @return the previous value of the specified name, or null
     * if it did not have one
     */
    public String addVariable(String name, String value) {
        return this.variablesTable.put(name, value);
    }

    /**
     * Removes a variable from the resolver. This method does
     * nothing if no variable with the name exists.
     * 
     * @return the value of the removed variable, or null if
     * no variable with the name exists
     */
    public String removeVariable(String name) {
        return this.variablesTable.remove(name);
    }

    /**
     * Checks if a variable with the specified name exists.
     * 
     * @return true if a variable exists, otherwise false
     */
    public boolean containsVariable(String name) {
        return this.variablesTable.containsKey(name);
    }

    /**
     * This method resolves all variables in the text.
     * The syntax is described at the head of the class.
     * 
     * @param text the string where the variables have to be
     * resolved
     * @return the resolved string
     */
    public String resolve(String text) {
        this.resolvedVariables = new Hashtable<String, String>();
        this.unresolvedVariables = new ArrayList<String>();
        return resolveText(text).getValue();
    }

    /**
     * This method resolves all variables in the text. In difference to the
     * <code>resolve(String text)</code> the <i>resolvedVariables</i>- and
     * the <i>unresolvedVariables</i> lists are not cleared.
     * 
     * @param text the text to resolve
     * @return the resolved string
     */
    public String resolveNext(String text) {
        return resolveText(text).getValue();
    }

    /**
     * Resolves a text and returns it as <code>Text</code> object.
     * 
     * @param text the text to resolve
     * @return an instance of <code>Text</code>
     */
    private Text resolveText(String text) {
        Text textResolver = new Text();
        textResolver.resolve(text, 0);
        return textResolver;
    }

    /**
     * Returns a new term in dependence of the . If no term is defined
     * null is returned.
     * 
     * @param c character to check the dependency
     * @return a term or null if no one found
     */
    private Term getTerm(String text, int pos) {
        Term term = null;
        if(text.startsWith(Variable.START_ENCLOSING_STRING, pos))
            term = new Variable();
        else if(text.startsWith(Condition.START_ENCLOSING_STRING, pos))
            term = new Condition();
        else if(text.startsWith(EscapeCharacter.START_ENCLOSING_STRING, pos))
            term = new EscapeCharacter();
        return term;
    }

    /**
     * Returns the value of a variable.
     * 
     * @param varName the name of the variable
     * @return the value
     */
    public String getValue(String varName) {
        return variablesTable.get(varName);
    }

    /**
     * Returns a hash table of all variables.
     * 
     * @return a hash table of all variables.
     */
    public Hashtable<String, String> getVariables() {
        return variablesTable;
    }

    /**
     * Returns a list of all variables which couldn't be resolved.
     * 
     * @return a list of unresolved variables
     */
    public List<String> getUnresolvedVariables() {
        return unresolvedVariables;
    }

    /**
     * Returns a hash table of all variables which are successfully
     * resolved. This includes only variables the incoming string
     * contains.
     * 
     * @return a hash table of resolved variables.
     */
    public Hashtable<String, String> getResolvedVariables() {
        return resolvedVariables;
    }

    /**
     * Returns a hash table of variables that are used in the last <code>
     * resolve</code> and all last <code>resolveNext</code> calls.
     * This contains also variables that are not in the original
     * variables table. The value string of these variables is empty ("").
     * 
     * @return a hash table of all variables that are used
     */
    public Hashtable<String, String> getUsedVariables() {
        Hashtable<String, String> usedVars = new Hashtable<String, String>();
        usedVars.putAll(resolvedVariables);
        for(String varName : unresolvedVariables) {
            usedVars.put(varName, "");
        }
        return usedVars;
    }

    /**
     * Returns a hash table of variables which are not used. Not used means
     * that in the last <code>resolve</code> and <code>resolveNext</code>
     * calls the variable doesn't occur.
     * 
     * @return a hash table of not used fields
     */
    public Hashtable<String, String> getNotUsedVariables() {
        Hashtable<String, String> usedVariables = getUsedVariables();
        Hashtable<String, String> notUsedVariables = new Hashtable<String, String>();
        for(Map.Entry<String, String> entry : variablesTable.entrySet()) {
            if(!usedVariables.containsKey(entry.getKey())) {
                notUsedVariables.put(entry.getKey(), entry.getValue());
            }
        }
        return notUsedVariables;
    }

    /**
     * Checks if the resolved text contains unresolved
     * variables.
     * 
     * @return true if the text doesn't contains unresolved
     * variables, otherwise false
     */
    public boolean isCompletelyResolved() {
        return (unresolvedVariables.size() == 0) ? true : false;
    }

    /**
     * Checks if MyCoRe properties are resolved if they are found. For example
     * {MCR.basedir}.
     * 
     * @return true if MyCoRe properties are resolved, otherwise false
     */
    public boolean useMCRProperties() {
        return useMCRProperties;
    }

    /**
     * Enables or disables MyCoRe property resolving.
     * 
     * @param useMCRProperties
     */
    public void setUseMCRProperties(boolean useMCRProperties) {
        this.useMCRProperties = useMCRProperties;
    }

    /**
     * A term is a defined part in a text. In general, a term is defined by brackets,
     * but this is not required. Here are some example terms:
     * <ul>
     * <li>Variable: {term1}</li>
     * <li>Condition: [term2]</li>
     * <li>EscapeChar: \[</li>
     * </ul>
     * 
     * @author Matthias Eichner
     */
    private abstract class Term {
        /**
         * The string buffer within the term. For example: {<b>var</b>}. 
         */
        protected StringBuffer termBuffer;
        /**
         * If the term is successfully resolved. By default this
         * is true.
         */
        protected boolean resolved;
        /**
         * The current character position in the term.
         */
        protected int position;

        public Term() {
            this.termBuffer = new StringBuffer();
            this.resolved = true;
            this.position = 0;
        }

        /**
         * Resolves the text from the startPosition to the end of the text
         * or if a term specific end character is found.
         * 
         * @param text the term to resolve
         * @param startPosition the current character position
         * @return the value of the term after resolving
         */
        public String resolve(String text, int startPosition) {
            for (position = startPosition; position < text.length(); position++) {
                Term internalTerm = getTerm(text, position);
                if(internalTerm != null) {
                    position += internalTerm.getStartEnclosingString().length();
                    internalTerm.resolve(text, position);
                    if(internalTerm.resolved == false)
                        resolved = false;
                    position = internalTerm.position;
                    termBuffer.append(internalTerm.getValue());
                } else {
                    boolean complete = resolveInternal(text, position);
                    if(complete) {
                        int endEnclosingSize = getEndEnclosingString().length();
                        if(endEnclosingSize > 1)
                            position += endEnclosingSize - 1;
                        break;
                    }
                }
            }
            return getValue();
        }

        /**
         * Does term specific resolving for the current character.
         * 
         * @param c the current character from text
         * @return true if the end string is reached, otherwise false
         */
        protected abstract boolean resolveInternal(String text, int pos);

        /**
         * Returns the value of the term. Overwrite this if you
         * don't want to get the default termBuffer content as value.
         * 
         * @return the value of the term
         */
        public String getValue() {
            return termBuffer.toString();
        }

        public abstract String getStartEnclosingString();
        public abstract String getEndEnclosingString();
    }

    /**
     * A variable is surrounded by curly brackets. It supports recursive
     * resolving for the content of the variable. The name of the variable
     * is set by the termBuffer and the value is equal the content of the
     * valueBuffer.
     */
    private class Variable extends Term {
        public static final String START_ENCLOSING_STRING = "{";
        public static final String END_ENCLOSING_STRING = "}";

        /**
         * A variable doesn't return the termBuffer, but
         * this valueBuffer.
         */
        private StringBuffer valueBuffer;

        public Variable() {
            super();
            valueBuffer = new StringBuffer();
        }

        @Override
        public boolean resolveInternal(String text, int pos) {
            if(text.startsWith(END_ENCLOSING_STRING, pos)) {
                // get the value from the variables table
                String value = variablesTable.get(termBuffer.toString());
                if(value == null) {
                    // variable is not in the list but maybe its a mycore property
                    if(useMCRProperties)
                        value = MCRConfiguration.instance().getString(termBuffer.toString(), null);
                    if(value == null) {
                        unresolvedVariables.add(termBuffer.toString());
                        resolved = false;
                        return true;
                    }
                }
                // resolve the content of the variable recursive
                // to resolve all other internal variables, condition etc.
                Text recursiveResolvedText = resolveText(value);
                resolved = recursiveResolvedText.resolved;
                // set the value of the variable
                valueBuffer.append(recursiveResolvedText.getValue());
                resolvedVariables.put(termBuffer.toString(), valueBuffer.toString());
                return true;
            }
            termBuffer.append(text.charAt(pos));
            return false;
        }

        @Override
        public String getValue() {
            return valueBuffer.toString();
        }

        @Override
        public String getStartEnclosingString() {
            return START_ENCLOSING_STRING;
        }
        @Override
        public String getEndEnclosingString() {
            return END_ENCLOSING_STRING;
        }
    }

    /**
     * A condition is defined by squared brackets. All data which
     * is set in these brackets is only used if the internal variables are
     * not null and not empty. For example "[hello {lastName}]" is only resolved
     * if the value of "lastName" is not null and not empty. Otherwise the whole
     * content in the squared brackets are ignored.
     */
    private class Condition extends Term {
        public static final String START_ENCLOSING_STRING = "[";
        public static final String END_ENCLOSING_STRING = "]";
        
        @Override
        protected boolean resolveInternal(String text, int pos) {
            if(text.startsWith(END_ENCLOSING_STRING, pos))
                return true;
            termBuffer.append(text.charAt(pos));
            return false;
        }

        @Override
        public String getValue() {
            if(resolved)
                return super.getValue();
            return "";
        }
        @Override
        public String getStartEnclosingString() {
            return START_ENCLOSING_STRING;
        }
        @Override
        public String getEndEnclosingString() {
            return END_ENCLOSING_STRING;
        }
    }

    /**
     * As escape character the backslashed is used. Only the
     * first character after the escape char is add to the term.
     */
    private class EscapeCharacter extends Term {
        public static final String START_ENCLOSING_STRING = "\\";

        @Override
        public boolean resolveInternal(String text, int pos) {
            return true;
        }
        @Override
        public String resolve(String text, int startPos) {
            this.position = startPos;
            char c = text.charAt(position);
            termBuffer.append(c);
            return termBuffer.toString();
        }
        @Override
        public String getStartEnclosingString() {
            return START_ENCLOSING_STRING;
        }
        @Override
        public String getEndEnclosingString() {
            return "";
        }
    }

    /**
     * A simple text, every character is added to the term (except its
     * a special one).
     */
    private class Text extends Term {
        @Override
        public boolean resolveInternal(String text, int pos) {
            termBuffer.append(text.charAt(pos));
            return false;
        }
        @Override
        public String getStartEnclosingString() {
            return "";
        }
        @Override
        public String getEndEnclosingString() {
            return "";
        }
    }
}