package org.mycore.common;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

/**
 * <p>
 * This class parses and resolves strings which contains variables.
 * The constructor takes a hashtable with all possible variables. Only these could be
 * resolved. The algorithm has been optimized so that each character is touched only once.
 * </p><p>
 * To resolve a string a valid syntax is required:
 * </p><p>
 * <b>{}:</b> Use curly brackets to set variables. For example "{var1}" 
 * </p><p>
 * <b>[]:</b> Use squared brackets to define a condition. All data which
 * is set in squared brackes is only used if the internal variables are
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
 * @author Matthias Eichner
 */
public class MCRTextResolver {

    protected Hashtable<String, String> variablesTable;

    protected List<String> resolvedVariables;
    protected List<String> unresolvedVariables;

    /**
     * Creates a new variables resolver.
     * 
     * @param variablesTable a hashtable of variables
     */
    public MCRTextResolver(Hashtable<String, String> variablesTable) {
        this.variablesTable = variablesTable;
        this.resolvedVariables = new ArrayList<String>();
        this.unresolvedVariables = new ArrayList<String>();
    }

    /**
     * This method resolves all variables in the text.
     * The synatax is described at the head of the class.
     * 
     * @param text the string where the variables have to be
     * resolved
     * @return the resolved string
     */
    public String resolve(String text) {
        this.resolvedVariables = new ArrayList<String>();
        this.unresolvedVariables = new ArrayList<String>();
        return resolveText(text).getValue();
    }

    /**
     * This method resolves all variables in the text. In difference to the
     * <code>resolve(String text)</code> the <i>resolvedVariables</i>- and
     * the <i>unresolvedVariables</i> lists are not cleard.
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
     * @return an instanceof <code>Text</code>
     */
    private Text resolveText(String text) {
        Text textResolver = new Text();
        textResolver.resolve(text, 0);
        return textResolver;
    }

    /**
     * Returns a new term in dependence of c. If no term is defined
     * null is returned.
     * 
     * @param c character to check the dependency
     * @return a term or null if no one found
     */
    private Term getTerm(char c) {
        Term term = null;
        if(c == '{')
            term = new Variable();
        else if(c == '[')
            term = new Condition();
        else if(c == '\\')
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
     * Returns a hashtable of all variables.
     * 
     * @return a hashtable of all variablse.
     */
    public Hashtable<String, String> getVariables() {
        return variablesTable;
    }

    /**
     * Returns a list of all variables which couldnt be resolved.
     * 
     * @return a list of unresolved variables
     */
    public List<String> getUnresolvedVariables() {
        return unresolvedVariables;
    }

    /**
     * Returns a list of all variables which are succesfully
     * resolved. This includes only variables the incoming string
     * contains.
     * 
     * @return a list of resolved variables.
     */
    public List<String> getResolvedVariables() {
        return resolvedVariables;
    }

    /**
     * Checks if the resolved text contains unresolved
     * variables.
     * 
     * @return true if the text doesnt contains unresolved
     * variables, otherwise false
     */
    public boolean isCompletlyResolved() {
        return (unresolvedVariables.size() == 0) ? true : false;
    }

    /**
     * A term is a defined part in a text. In general, a term is defined by brackets.
     * But this is not required. Here some example terms:
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
                char c = text.charAt(position);
                Term internalTerm = getTerm(c);
                if(internalTerm != null) {
                    internalTerm.resolve(text, ++position);
                    if(internalTerm.resolved == false)
                        resolved = false;
                    position = internalTerm.position;
                    termBuffer.append(internalTerm.getValue());
                } else {
                    boolean complete = resolveInternal(c);
                    if(complete)
                        break;
                }
            }
            return getValue();
        }

        /**
         * Does term specific resolving for the current character.
         * 
         * @param c the current character from text
         * @return true if the end character is reached, otherwise false
         */
        protected abstract boolean resolveInternal(char c);

        /**
         * Returns the value of the term. Overwritte this if you
         * dont want to get the default termBuffer content as value.
         * 
         * @return the value of the term
         */
        public String getValue() {
            return termBuffer.toString();
        }
    }

    /**
     * A variable is sourrounded by curly brackets. It supports recursive
     * resolving for the content of the variable. The name of the variable
     * is set by the termBuffer and the value is equal the content of the
     * valueBuffer.
     */
    private class Variable extends Term {
        /**
         * A variable doesnt return the termBuffer, but
         * this valueBuffer.
         */
        private StringBuffer valueBuffer;

        public Variable() {
            super();
            valueBuffer = new StringBuffer();
        }

        @Override
        public boolean resolveInternal(char c) {
            if(c == '}') {
                // get the value from the variables table
                String value = variablesTable.get(termBuffer.toString());
                if (value != null) {
                    // resolve the content of the variable recursive
                    // to resolve all other internal variables, condition etc.
                    Text recursiveResolvedText = resolveText(value);
                    resolved = recursiveResolvedText.resolved;
                    // set the value of the variable
                    valueBuffer.append(recursiveResolvedText.getValue());
                    resolvedVariables.add(termBuffer.toString());
                } else {
                    unresolvedVariables.add(termBuffer.toString());
                    resolved = false;
                }
                return true;
            }
            termBuffer.append(c);
            return false;
        }

        @Override
        public String getValue() {
            return valueBuffer.toString();
        }
    }

    /**
     * A condition is defined by squared bracktes. All data which
     * is set in these brackes is only used if the internal variables are
     * not null and not empty. For example "[hello {lastName}]" is only resolved
     * if the value of "lastName" is not null and not empty. Otherwise the whole
     * content in the squared brackets are ignored.
     */
    private class Condition extends Term {
        @Override
        protected boolean resolveInternal(char c) {
            if(c == ']')
                return true;
            termBuffer.append(c);
            return false;
        }

        @Override
        public String getValue() {
            if(resolved)
                return super.getValue();
            return "";
        }
    }

    /**
     * As escape character the backslashed is used. Only the
     * first character after the escape char is add to the term.
     */
    private class EscapeCharacter extends Term {
        @Override
        public boolean resolveInternal(char c) {
            return true;
        }
        @Override
        public String resolve(String text, int startPos) {
            this.position = startPos;
            char c = text.charAt(position);
            termBuffer.append(c);
            return termBuffer.toString();
        }
    }

    /**
     * A simple text, every character is add to the term (expect its
     * a special one).
     */
    private class Text extends Term {
        @Override
        public boolean resolveInternal(char c) {
            termBuffer.append(c);
            return false;
        }
    }
}