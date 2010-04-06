package org.mycore.common;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

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

    private static final Logger LOGGER = Logger.getLogger(MCRTextResolver.class);

    private static Map<String, Class<? extends Term>> termList;

    /**
     * Creates the term list for the text resolver and adds
     * the default terms.
     */
    static {
        termList = new Hashtable<String, Class<? extends Term>>();
        try {
            registerTerm(Variable.class);
            registerTerm(Condition.class);
            registerTerm(EscapeCharacter.class);
        } catch(Exception exc) {
            LOGGER.error(exc);
        }
    }

    /**
     * Register a new term. The resolver invokes the term via reflection.
     * 
     * @param termClass the term class to register. 
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     * @throws InstantiationException
     */
    public static void registerTerm(Class<? extends Term> termClass) throws NoSuchMethodException, InvocationTargetException,
            IllegalAccessException, InstantiationException {
        Constructor<? extends Term> c = termClass.getConstructor(MCRTextResolver.class);
        termList.put(c.newInstance(new MCRTextResolver()).getStartEnclosingString(), termClass);
    }

    /**
     * Unregister a term.
     * 
     * @param termClass this class is unregistered
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
    public static void unregisterTerm(Class<? extends Term> termClass) throws NoSuchMethodException, InvocationTargetException,
            InstantiationException, IllegalAccessException {
        Constructor<? extends Term> c = termClass.getConstructor(MCRTextResolver.class);
        termList.remove(c.newInstance(new MCRTextResolver()).getStartEnclosingString());
    }

    /**
     * Defines how deep the text is resolved.
     * <li><b>Deep</b> - everything is resolved</li>
     * <li><b>NoVariables</b> - the value of variables is not being resolved</li>
     */
    public enum ResolveDepth {
        Deep, NoVariables
    }

    /**
     * If MyCoRe properties are resolved. For example {MCR.basedir}.
     * By default this is true.
     */
    private static boolean useMCRProperties = true;

    /**
     * This map contains all variables that can be resolved.
     */
    protected Map<String, String> variablesTable;

    protected Map<String, String> resolvedVariables;

    protected List<String> unresolvedVariables;

    protected ResolveDepth resolveDepth;

    /**
     * Creates a new text resolver. To add variables call
     * <code>addVariable</code>, otherwise only MyCoRe property
     * resolving is possible.
     */
    public MCRTextResolver() {
        this(new Hashtable<String, String>(), ResolveDepth.Deep);
    }

    public MCRTextResolver(Map<String, String> variablesTable) {
        this(variablesTable, ResolveDepth.Deep);
    }

    public MCRTextResolver(ResolveDepth depth) {
        this(new Hashtable<String, String>(), depth);
    }

    /**
     * Creates a new text resolver with a map of variables.
     * 
     * @param variablesTable a hash table of variables
     * @param depth how deep the text is resolved
     */
    public MCRTextResolver(Map<String, String> variablesTable, ResolveDepth depth) {
        this.variablesTable = variablesTable;
        this.resolvedVariables = new Hashtable<String, String>();
        this.unresolvedVariables = new ArrayList<String>();
        this.resolveDepth = depth;
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
        return variablesTable.put(name, value);
    }

    /**
     * Removes a variable from the resolver. This method does
     * nothing if no variable with the name exists.
     * 
     * @return the value of the removed variable, or null if
     * no variable with the name exists
     */
    public String removeVariable(String name) {
        return variablesTable.remove(name);
    }

    /**
     * Checks if a variable with the specified name exists.
     * 
     * @return true if a variable exists, otherwise false
     */
    public boolean containsVariable(String name) {
        return variablesTable.containsKey(name);
    }

    /**
     * Sets the resolve depth.
     * 
     * @param resolveDepth defines how deep the text is resolved.
     */
    public void setResolveDepth(ResolveDepth resolveDepth) {
        this.resolveDepth = resolveDepth;
    }

    /**
     * Returns the current resolve depth.
     * 
     * @return resolve depth enumeration
     */
    public ResolveDepth getResolveDepth() {
        return resolveDepth;
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
        resolvedVariables = new Hashtable<String, String>();
        unresolvedVariables = new ArrayList<String>();
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
     * Returns a new term in dependence of the current character (position of the text).
     * If no term is defined null is returned.
     * 
     * @param c character to check the dependency
     * @return a term or null if no one found
     */
    private Term getTerm(String text, int pos) {
        for(Entry<String, Class<? extends Term>> termEntry : termList.entrySet()) {
            if(text.startsWith(termEntry.getKey(), pos)) {
                try {
                    Constructor<? extends Term> c = termEntry.getValue().getConstructor(MCRTextResolver.class);
                    return  c.newInstance(this);
                } catch(Exception exc) {
                    LOGGER.error(exc);
                }
            }
        }
        return null;
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
     * Returns a <code>Map</code> of all variables.
     * 
     * @return a <code>Map</code> of all variables.
     */
    public Map<String, String> getVariables() {
        return variablesTable;
    }

    /**
     * Returns a <code>List</code> of all variables which couldn't be resolved.
     * 
     * @return a <code>List</code> of unresolved variables
     */
    public List<String> getUnresolvedVariables() {
        return unresolvedVariables;
    }

    /**
     * Returns a <code>Map</code> of all variables which are successfully
     * resolved. This includes only variables the incoming string
     * contains.
     * 
     * @return a <code>Map</code> of resolved variables.
     */
    public Map<String, String> getResolvedVariables() {
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
        for (String varName : unresolvedVariables) {
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
        for (Map.Entry<String, String> entry : variablesTable.entrySet()) {
            if (!usedVariables.containsKey(entry.getKey())) {
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
        return unresolvedVariables.size() == 0 ? true : false;
    }

    /**
     * Checks if MyCoRe properties are resolved if they found. For example
     * {MCR.basedir}.
     * 
     * @return true if MyCoRe properties are resolved, otherwise false
     */
    public static boolean useMCRProperties() {
        return useMCRProperties;
    }

    /**
     * Enable or disable MyCoRe property resolving.
     * 
     * @param useMCRProperties
     */
    public static void setUseMCRProperties(boolean value) {
        useMCRProperties = value;
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
     * You can write your own terms and add them to the text resolver. A sample is
     * shown in the <code>MCRTextResolverTest</code> class.
     * 
     * @author Matthias Eichner
     */
    public abstract class Term {
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
            termBuffer = new StringBuffer();
            resolved = true;
            position = 0;
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
                if (internalTerm != null) {
                    position += internalTerm.getStartEnclosingString().length();
                    internalTerm.resolve(text, position);
                    if (internalTerm.resolved == false) {
                        resolved = false;
                    }
                    position = internalTerm.position;
                    termBuffer.append(internalTerm.getValue());
                } else {
                    boolean complete = resolveInternal(text, position);
                    if (complete) {
                        int endEnclosingSize = getEndEnclosingString().length();
                        if (endEnclosingSize > 1) {
                            position += endEnclosingSize - 1;
                        }
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

        /**
         * Implement this to define the start enclosing string for
         * your term. The resolver searches in the text for this
         * string, if found, the text is processed by your term.
         * 
         * @return the start enclosing string
         */
        public abstract String getStartEnclosingString();

        /**
         * Implement this to define the end enclosing string for
         * your term. You have to check manual in the
         * <code>resolveInternal</code> method if the end of  
         * your term is reached.
         * 
         * @return the end enclosing string
         */
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
            if (text.startsWith(END_ENCLOSING_STRING, pos)) {
                // get the value from the variables table
                String value = variablesTable.get(termBuffer.toString());
                if (value == null) {
                    // variable is not in the list but maybe its a mycore property
                    if (useMCRProperties) {
                        value = MCRConfiguration.instance().getString(termBuffer.toString(), null);
                    }
                    if (value == null) {
                        unresolvedVariables.add(termBuffer.toString());
                        resolved = false;
                        return true;
                    }
                }
                // resolve the content of the variable recursive
                // to resolve all other internal variables, condition etc.
                if(resolveDepth != ResolveDepth.NoVariables) {
                    Text recursiveResolvedText = resolveText(value);
                    resolved = recursiveResolvedText.resolved;
                    value = recursiveResolvedText.getValue();
                }
                // set the value of the variable
                valueBuffer.append(value);
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

        public Condition() {
            super();
        }

        @Override
        protected boolean resolveInternal(String text, int pos) {
            if (text.startsWith(getEndEnclosingString(), pos)) {
                return true;
            }
            termBuffer.append(text.charAt(pos));
            return false;
        }

        @Override
        public String getValue() {
            if (resolved) {
                return super.getValue();
            }
            return "";
        }

        @Override
        public String getStartEnclosingString() {
            return "[";
        }

        @Override
        public String getEndEnclosingString() {
            return "]";
        }
    }

    /**
     * As escape character the backslashed is used. Only the
     * first character after the escape char is add to the term.
     */
    private class EscapeCharacter extends Term {

        public EscapeCharacter() {
            super();
        }
        @Override
        public boolean resolveInternal(String text, int pos) {
            return true;
        }

        @Override
        public String resolve(String text, int startPos) {
            position = startPos;
            char c = text.charAt(position);
            termBuffer.append(c);
            return termBuffer.toString();
        }

        @Override
        public String getStartEnclosingString() {
            return "\\";
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
        public Text() {
            super();
        }
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