/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mycore.frontend.cli;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.text.Format;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;

import org.apache.commons.lang.ClassUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRException;
import org.mycore.common.config.MCRConfigurationException;

/**
 * Represents a command understood by the command line interface. A command has an external input syntax that the user
 * uses to invoke the command and points to a method in a class that implements the command.
 * 
 * @see MCRCommandLineInterface
 * @author Frank Lützenkirchen
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 */
public class MCRCommand {

    private static final Logger LOGGER = LogManager.getLogger(MCRCommand.class);

    /** The input format used for invoking this command */
    protected MessageFormat messageFormat;

    /** The java method that implements this command */
    private Method method;

    /** The types of the invocation parameters */
    protected Class<?>[] parameterTypes;

    /** The class providing the implementation method */
    protected String className;

    /** The method implementing this command */
    protected String methodName;

    /** The beginning of the message format up to the first parameter */
    protected String suffix;

    /** The help text String */
    protected String help;

    /**
     * use this to overwrite this class.
     */
    protected MCRCommand() {
    }

    /**
     * Creates a new MCRCommand.
     * 
     * @param format
     *            the command syntax, e.g. "save document {0} to directory {1}"
     * @param methodSignature
     *            the method to invoke, e.g. "miless.commandline.DocumentCommands.saveDoc int String"
     * @param helpText
     *            the helpt text for this command
     */
    public MCRCommand(String format, String methodSignature, String helpText) {
        StringTokenizer st = new StringTokenizer(methodSignature, " ");

        String token = st.nextToken();
        int point = token.lastIndexOf(".");

        className = token.substring(0, point);
        methodName = token.substring(point + 1);
        int numParameters = st.countTokens();
        parameterTypes = new Class<?>[numParameters];
        messageFormat = new MessageFormat(format, Locale.ROOT);

        for (int i = 0; i < numParameters; i++) {
            token = st.nextToken();

            Format f = null;
            switch (token) {
                case "int":
                    parameterTypes[i] = Integer.TYPE;
                    f = NumberFormat.getIntegerInstance(Locale.ROOT);
                    break;
                case "long":
                    parameterTypes[i] = Long.TYPE;
                    f = NumberFormat.getIntegerInstance(Locale.ROOT);
                    break;
                case "String":
                    parameterTypes[i] = String.class;
                    break;
                default:
                    unsupportedArgException(methodSignature, token);
            }
            messageFormat.setFormat(i, f);
        }

        int pos = format.indexOf("{");
        suffix = pos == -1 ? format : format.substring(0, pos);

        if (helpText != null) {
            help = helpText;
        } else {
            help = "No help text available for this command";
        }
    }

    private void unsupportedArgException(String methodSignature, String token) {
        throw new MCRConfigurationException("Error while parsing command definitions for command line interface:\n"
            + "Unsupported argument type '" + token + "' in command " + methodSignature);
    }

    public MCRCommand(Method cmd) {
        className = cmd.getDeclaringClass().getName();
        methodName = cmd.getName();
        parameterTypes = cmd.getParameterTypes();
        org.mycore.frontend.cli.annotation.MCRCommand cmdAnnotation = cmd
            .getAnnotation(org.mycore.frontend.cli.annotation.MCRCommand.class);
        help = cmdAnnotation.help();
        messageFormat = new MessageFormat(cmdAnnotation.syntax(), Locale.ROOT);
        setMethod(cmd);

        for (int i = 0; i < parameterTypes.length; i++) {
            Class<?> paramtype = parameterTypes[i];
            if (ClassUtils.isAssignable(paramtype, Integer.class, true)
                || ClassUtils.isAssignable(paramtype, Long.class, true)) {
                messageFormat.setFormat(i, NumberFormat.getIntegerInstance(Locale.ROOT));
            } else if (!String.class.isAssignableFrom(paramtype)) {
                unsupportedArgException(className + "." + methodName, paramtype.getName());
            }
        }

        int pos = cmdAnnotation.syntax().indexOf("{");
        suffix = pos == -1 ? cmdAnnotation.syntax() : cmdAnnotation.syntax().substring(0, pos);
    }

    private void initMethod(ClassLoader classLoader) throws ClassNotFoundException, NoSuchMethodException {
        if (method == null) {
            setMethod(Class.forName(className, true, classLoader).getMethod(methodName, parameterTypes));
        }
    }

    /**
     * The method return the helpt text of this command.
     * 
     * @return the help text as String
     */
    public String getHelpText() {
        return help;
    }

    /**
     * Parses an input string and tries to match it with the message format used to invoke this command.
     * 
     * @param commandLine
     *            The input from the command line
     * @return null, if the input does not match the message format; otherwise an array holding the parameter values
     *         from the command line
     */
    protected Object[] parseCommandLine(String commandLine) {
        try {
            return messageFormat.parse(commandLine);
        } catch (ParseException ex) {
            return null;
        }
    }

    /**
     * Transforms the parameters found by the MessageFormat parse method into such that can be used to invoke the method
     * implementing this command
     * 
     * @param commandParameters
     *            The parameters as returned by the <code>parseCommandLine</code> method
     */
    private void prepareInvocationParameters(Object[] commandParameters) {

        for (int i = 0; i < commandParameters.length; i++) {
            if (parameterTypes[i] == Integer.TYPE) {
                commandParameters[i] = ((Number) commandParameters[i]).intValue();
            }
        }
    }

    /**
     * Tries to invoke the method that implements the behavior of this command given the user input from the command
     * line. This is only done when the command line syntax matches the syntax used by this command.
     * 
     * @return null, if the command syntax did not match and the command was not invoked, otherwise a List of commands
     *         is returned which may be empty or otherwise contains commands that should be processed next
     * @param input
     *            The command entered by the user at the command prompt
     * @throws IllegalAccessException
     *             when the method can not be invoked
     * @throws InvocationTargetException
     *             when an exception is thrown by the invoked method
     * @throws ClassNotFoundException
     *             when the class providing the method could not be found
     * @throws NoSuchMethodException
     *             when the method specified does not exist
     */
    public List<String> invoke(String input) throws IllegalAccessException, InvocationTargetException,
        ClassNotFoundException, NoSuchMethodException {
        return invoke(input, MCRCommand.class.getClassLoader());
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public List<String> invoke(String input, ClassLoader classLoader) throws IllegalAccessException,
        InvocationTargetException, ClassNotFoundException, NoSuchMethodException {
        if (!input.startsWith(suffix)) {
            return null;
        }

        Object[] commandParameters = parseCommandLine(input);

        if (commandParameters == null) {
            LOGGER.info("No match for syntax: {}", getSyntax());
            return null;
        }
        LOGGER.info("Syntax matched (executed): {}", getSyntax());

        initMethod(classLoader);
        prepareInvocationParameters(commandParameters);
        Object result = method.invoke(null, commandParameters);
        if (result instanceof List && !((List) result).isEmpty() && ((List) result).get(0) instanceof String) {
            return (List<String>) result;
        } else {
            return new ArrayList<>();
        }
    }

    /**
     * Returns the input syntax to be used for invoking this command from the command prompt.
     * 
     * @return the input syntax for this command
     */
    public final String getSyntax() {
        return messageFormat.toPattern();
    }

    public void outputHelp() {
        MCRCommandLineInterface.output(getSyntax());
        MCRCommandLineInterface.output("    " + getHelpText());
        MCRCommandLineInterface.output("");
    }

    /**
     * @param method
     *            the method to set
     */
    public void setMethod(Method method) {
        if (!Modifier.isStatic(method.getModifiers())) {
            throw new MCRException("MCRCommand method needs to be static: " + method);
        }
        this.method = method;
    }
}
