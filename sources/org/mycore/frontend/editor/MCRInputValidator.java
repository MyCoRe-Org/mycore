/*
 * $RCSfile$
 * $Revision$ $Date$
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */

package org.mycore.frontend.editor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.*;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Pattern;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.transform.JDOMSource;
import org.mycore.common.MCRCache;
import org.mycore.common.MCRConfigurationException;
import org.mycore.common.MCRException;

/**
 * This class provides input validation methods for editor data.
 * 
 * @author Frank Lützenkirchen
 */
public class MCRInputValidator {
    /** Template stylesheet for checking XSL conditions * */
    private Document stylesheet = null;

    /** XSL transformer factory * */
    private TransformerFactory factory = null;

    /** Creates a new, reusable input validator * */
    private MCRInputValidator() {
        stylesheet = prepareStylesheet();
        factory = TransformerFactory.newInstance();
    }

    private static MCRInputValidator singleton;

    public static synchronized MCRInputValidator instance() {
        if (singleton == null) {
            singleton = new MCRInputValidator();
        }

        return singleton;
    }

    /** Cache of reusable stylesheets for checking XSL conditions * */
    private MCRCache xslcondCache = new MCRCache(20);

    /**
     * Checks the input string against an XSL condition. The syntax of the
     * condition string is same as it would be usable in a xsl:if condition. The
     * input string can be referenced by "." or "text()" in the condition, for
     * example a condition could be "starts-with(.,'http://')". If input string
     * is null, false is returned.
     * 
     * @param input
     *            the string that should be validated
     * @param condition
     *            the XSL condition as it would be used in xsl:when or xsl:if
     * @return false if input is null, otherwise the result of the test is
     *         returned
     * @throws MCRConfigurationException
     *             if XSL condition has syntax errors
     */
    public boolean validateXSLCondition(String input, String condition) {
        if (input == null) {
            input = "";
        }

        Document xml = new Document(new Element("input").addContent(input));
        Source xmlsrc = new JDOMSource(xml);

        Document xsl = (Document) (xslcondCache.get(condition));

        if (xsl == null) {
            xsl = (Document) (stylesheet.clone());

            Element when = xsl.getRootElement().getChild("template", xslns).getChild("choose", xslns).getChild("when", xslns);
            when.setAttribute("test", condition);
            xslcondCache.put(condition, xsl);
        }

        try {
            Transformer transformer = factory.newTransformer(new JDOMSource(xsl));
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            transformer.transform(xmlsrc, new StreamResult(out));
            out.close();

            return "t".equals(out.toString("UTF-8"));
        } catch (TransformerConfigurationException e) {
            String msg = "Could not build XSL transformer";
            throw new org.mycore.common.MCRConfigurationException(msg, e);
        } catch (UnsupportedEncodingException e) {
            String msg = "UTF-8 encoding seems not to be supported?";
            throw new org.mycore.common.MCRConfigurationException(msg, e);
        } catch (TransformerException e) {
            String msg = "Probably syntax error in this XSL condition: " + condition;
            throw new org.mycore.common.MCRConfigurationException(msg, e);
        } catch (IOException e) {
            String msg = "IOException in memory, this should never happen";
            throw new org.mycore.common.MCRConfigurationException(msg, e);
        }
    }

    private Namespace xslns = Namespace.getNamespace("xsl", "http://www.w3.org/1999/XSL/Transform");

    /** Prepares a template stylesheet that is used for checking XSL conditions * */
    private synchronized Document prepareStylesheet() {
        Element stylesheet = new Element("stylesheet").setAttribute("version", "1.0");
        stylesheet.setNamespace(xslns);

        Element output = new Element("output", xslns);
        output.setAttribute("method", "text");
        stylesheet.addContent(output);

        Element template = new Element("template", xslns).setAttribute("match", "/input");
        stylesheet.addContent(template);

        Element choose = new Element("choose", xslns);
        template.addContent(choose);

        Element when = new Element("when", xslns);
        when.addContent("t");

        Element otherwise = new Element("otherwise", xslns);
        otherwise.addContent("f");
        choose.addContent(when).addContent(otherwise);

        return new Document(stylesheet);
    }

    /** Cache of reusable compiled regular expressions * */
    private MCRCache regexpCache = new MCRCache(20);

    /**
     * Checks the input string against a regular expression.
     * 
     * @see java.util.regex.Pattern#compile(java.lang.String)
     * 
     * @param input
     *            the string that should be validated
     * @param regexp
     *            the regular expression using the syntax of the
     *            java.util.regex.Pattern class
     * @return false if input is null, otherwise the result of the test is
     *         returned
     */
    public boolean validateRegularExpression(String input, String regexp) {
        if (input == null) {
            input = "";
        }

        Pattern p = (Pattern) (regexpCache.get(regexp));

        if (p == null) {
            p = Pattern.compile(regexp);
            regexpCache.put(regexp, p);
        }

        return p.matcher(input).matches();
    }

    /**
     * Checks an input string for minimum and/or maximum length. The minimum and
     * maximum length must be given as a string that contains the actual int
     * number, both arguments are optional if one of the limits should not be
     * checked.
     * 
     * @param input
     *            the input string thats length should be checked
     * @param smin
     *            minimum length as a string, or null if min lenght should not
     *            be checked
     * @param smax
     *            maximum length as a string, or null if max length should not
     *            be checked
     * @return true, if the string matches the given min and max lengths
     */
    public boolean validateLength(String input, String smin, String smax) {
        if (input == null) {
            input = "";
        }

        int min = ((smin == null) ? Integer.MIN_VALUE : Integer.parseInt(smin));
        int max = ((smax == null) ? Integer.MAX_VALUE : Integer.parseInt(smax));

        return (input.length() >= min) && (input.length() <= max);
    }

    /** Cache of reusable DateFormat objects * */
    private MCRCache formatCache = new MCRCache(20);

    /**
     * Returns a reusable DateFormat object for the given format string. That
     * object may come from a cache.
     */
    private DateFormat getDateTimeFormat(String format) {
        DateFormat df = (DateFormat) (formatCache.get(format));

        if (df == null) {
            df = new SimpleDateFormat(format);
            df.setLenient(false);
            formatCache.put(format, df);
        }

        return df;
    }

    /**
     * Checks if input is null or empty or just contains whitespace.
     * 
     * @param input
     *            the string to be checked
     * @return false if input is null or empty or just blanks
     */
    public boolean validateRequired(String input) {
        return ((input != null) && (input.trim().length() > 0));
    }

    /**
     * Checks input for correct data type and minimum/maximum value. Possible
     * data types are string, integer, decimal or datetime. The min and max
     * arguments are optional and must be expressed as strings. The min and max
     * value are used inclusive in the allowed range of values. If no check for
     * min or max value should be performed, null can be given for that
     * argument. For datetime input, the format of the string must be given as
     * defined in SimpleDateFormat. For decimal input, the format argument
     * should contain a two-character, lowercase language code as defined by ISO
     * 639. This code determines the locale that is used to parse decimal
     * values. If null is given, the default locale will be used. Ffor other
     * data types null should be used as the format argument.
     * 
     * Usage examples:
     * <ul>
     * <li>validateMinMaxType( input, "integer", "15", "20", null )</li>
     * <li>validateMinMaxType( input, "datetime", "01.01.2000", null,
     * "dd.MM.yyyy" )</li>
     * <li>validateMinMaxType( input, "decimal", "3,1", "4,0", "de" )</li>
     * </ul>
     * 
     * @see java.text.SimpleDateFormat
     * @see java.util.Locale
     * @see java.text.NumberFormat#getInstance(java.util.Locale)
     * 
     * @param input
     *            the input string to check
     * @param type
     *            one of "string", "integer", "decimal" or "datetime"
     * @param min
     *            the minimum value as a string, or null if min should not be
     *            tested
     * @param max
     *            the maximum value as a string, or null if max should not be
     *            tested
     * @param format
     *            for datetime input, a java.text.SimpleDateFormat pattern; for
     *            decimal input, a ISO-639 language code
     * @return true if input matches the given data type, min, max value and
     *         date time format
     */
    public boolean validateMinMaxType(String input, String type, String min, String max, String format) {
        if (input == null) {
            input = "";
        }

        if (type.equals("string")) {
            boolean ok = true;

            if (min != null) {
                ok = (min.compareTo(input) <= 0);
            }

            if (max != null) {
                ok = ok && (max.compareTo(input) >= 0);
            }

            return ok;
        } else if (type.equals("integer")) {
            long lmin = Long.MIN_VALUE;
            long lmax = Long.MAX_VALUE;
            long lval = 0;

            try {
                if (min != null) {
                    lmin = Long.parseLong(min);
                }

                if (max != null) {
                    lmax = Long.parseLong(max);
                }
            } catch (NumberFormatException ex) {
                String msg = "Could not parse min/max value for input validation";
                throw new MCRConfigurationException(msg, ex);
            }

            try {
                lval = Long.parseLong(input);
            } catch (NumberFormatException ex) {
                return false;
            }

            return (lmin <= lval) && (lmax >= lval);
        } else if (type.equals("decimal")) {
            Locale locale = ((format == null) ? Locale.getDefault() : new Locale(format));
            NumberFormat nf = NumberFormat.getNumberInstance(locale);

            double dmin = Double.MIN_VALUE;
            double dval = Double.MAX_VALUE;
            double dmax = 0.0;

            try {
                if (min != null) {
                    dmin = nf.parse(min).doubleValue();
                }

                if (max != null) {
                    dmax = nf.parse(max).doubleValue();
                }
            } catch (ParseException ex) {
                String msg = "Could not parse min/max value for input validation";
                throw new MCRConfigurationException(msg, ex);
            }

            try {
                dval = nf.parse(input).doubleValue();
            } catch (ParseException e) {
                return false;
            }

            return (dmin <= dval) && (dmax >= dval);
        } else if (type.equals("datetime")) {
            DateFormat df = getDateTimeFormat(format);

            Date dmin = null;
            Date dmax = null;
            Date dval = null;

            try {
                dval = df.parse(input);
            } catch (ParseException ex) {
                return false;
            }

            try {
                if (min != null) {
                    dmin = df.parse(min);
                }

                if (max != null) {
                    dmax = df.parse(max);
                }
            } catch (ParseException ex) {
                String msg = "Could not parse min/max value for input validation";
                throw new MCRConfigurationException(msg, ex);
            }

            if ((dmin != null) && (dmin.after(dval))) {
                return false;
            }

            if ((dmax != null) && (dmax.before(dval))) {
                return false;
            }

            return true;
        } else {
            throw new MCRConfigurationException("Unknown input data type: " + type);
        }
    }

    /**
     * Compares two input fields using a comparison operator.
     * 
     * @param valueA
     *            the first input string to check
     * @param valueB
     *            the second input string to check
     * @param type
     *            one of "string", "integer", "decimal" or "datetime"
     * @param operator
     *            One of =, <, >, <=, >=, !=
     * @param format
     *            for datetime input, a java.text.SimpleDateFormat pattern; for
     *            decimal input, a ISO-639 language code
     * 
     * @return true if the compare result is true OR one of the input fields is
     *         empty OR one of the input fields is in wrong format.
     */
    public boolean compare(String valueA, String valueB, String operator, String type, String format) {
        try {
            if ((valueA == null) || (valueA.trim().length() == 0)) {
                return true;
            }

            if ((valueB == null) || (valueB.trim().length() == 0)) {
                return true;
            }

            if (type.equals("string")) {
                int res = valueA.compareTo(valueB);

                if ("=".equals(operator)) {
                    return (res == 0);
                } else if ("<".equals(operator)) {
                    return (res < 0);
                } else if (">".equals(operator)) {
                    return (res > 0);
                } else if ("<=".equals(operator)) {
                    return (res <= 0);
                } else if (">=".equals(operator)) {
                    return (res >= 0);
                } else if ("!=".equals(operator)) {
                    return !(res == 0);
                } else {
                    throw new MCRConfigurationException("Unknown compare operator: " + operator);
                }
            } else if (type.equals("integer")) {
                long vA = Long.parseLong(valueA);
                long vB = Long.parseLong(valueB);

                if ("=".equals(operator)) {
                    return (vA == vB);
                } else if ("<".equals(operator)) {
                    return (vA < vB);
                } else if (">".equals(operator)) {
                    return (vA > vB);
                } else if ("<=".equals(operator)) {
                    return (vA <= vB);
                } else if (">=".equals(operator)) {
                    return (vA >= vB);
                } else if ("!=".equals(operator)) {
                    return !(vA == vB);
                } else {
                    throw new MCRConfigurationException("Unknown compare operator: " + operator);
                }
            } else if (type.equals("decimal")) {
                Locale locale = ((format == null) ? Locale.getDefault() : new Locale(format));
                NumberFormat nf = NumberFormat.getNumberInstance(locale);
                double vA = nf.parse(valueA).doubleValue();
                double vB = nf.parse(valueB).doubleValue();

                if ("=".equals(operator)) {
                    return (vA == vB);
                } else if ("<".equals(operator)) {
                    return (vA < vB);
                } else if (">".equals(operator)) {
                    return (vA > vB);
                } else if ("<=".equals(operator)) {
                    return (vA <= vB);
                } else if (">=".equals(operator)) {
                    return (vA >= vB);
                } else if ("!=".equals(operator)) {
                    return !(vA == vB);
                } else {
                    throw new MCRConfigurationException("Unknown compare operator: " + operator);
                }
            } else if (type.equals("datetime")) {
                DateFormat df = getDateTimeFormat(format);
                Date vA = df.parse(valueA);
                Date vB = df.parse(valueB);

                if ("=".equals(operator)) {
                    return (vA.equals(vB));
                } else if ("<".equals(operator)) {
                    return (vA.before(vB));
                } else if (">".equals(operator)) {
                    return (vA.after(vB));
                } else if ("<=".equals(operator)) {
                    return (vA.before(vB) || vA.equals(vB));
                } else if (">=".equals(operator)) {
                    return (vA.after(vB) || vA.equals(vB));
                } else if ("!=".equals(operator)) {
                    return !(vA.equals(vB));
                } else {
                    throw new MCRConfigurationException("Unknown compare operator: " + operator);
                }
            } else {
                throw new MCRConfigurationException("Unknown input data type: " + type);
            }
        } catch (ParseException ex) {
            return true;
        }
    }

    /**
     * Calls a "public static boolean" method in the given class and validates
     * the value externally using the given method in that class.
     * 
     * @param clazz
     *            the name of the class that contains the validation method
     * @param method
     *            the name of the public static boolean method that should be
     *            called
     * @param value
     *            the value to validate
     * 
     * @return true, if the value validates
     */
    public boolean validateExternally(String clazz, String method, String value) {
        Class[] argTypes = new Class[1];
        argTypes[0] = String.class;
        Object[] args = new Object[1];
        args[0] = value;
        Object result = new Boolean(false);
        try {
            Method m = Class.forName(clazz).getMethod(method, argTypes);
            result = m.invoke(null, args);
        } catch (Exception ex) {
            String msg = "Exception while validating input using external method";
            throw new MCRException(msg, ex);
        }
        return ((Boolean) result).booleanValue();
    }

    /**
     * Calls a "public static boolean" method in the given class and validates
     * the two values externally using the given method in that class.
     * 
     * @param clazz
     *            the name of the class that contains the validation method
     * @param method
     *            the name of the public static boolean method that should be
     *            called
     * @param value1
     *            the first value to validate
     * @param value2
     *            the second value to validate
     * 
     * @return true, if the two values validate
     */
    public boolean validateExternally(String clazz, String method, String value1, String value2) {
        Class[] argTypes = new Class[2];
        argTypes[0] = String.class;
        argTypes[1] = String.class;
        Object[] args = new Object[2];
        args[0] = value1;
        args[1] = value2;
        Object result = new Boolean(false);
        try {
            Method m = Class.forName(clazz).getMethod(method, argTypes);
            result = m.invoke(null, args);
        } catch (Exception ex) {
            String msg = "Exception while validating input using external method";
            throw new MCRException(msg, ex);
        }
        return ((Boolean) result).booleanValue();
    }

    public static void main(String[] args) {
        MCRInputValidator iv = MCRInputValidator.instance();
        System.out.println(true == iv.validateXSLCondition("bingo@bongo.com", "contains(.,'@')"));
        System.out.println(false == iv.validateLength("john doe", "20", null));
        System.out.println(false == iv.validateRequired(" \t"));
        System.out.println(false == iv.validateRegularExpression("aacab", "a*b"));
        System.out.println(true == iv.validateMinMaxType("4711", "integer", "100", null, null));
        System.out.println(true == iv.validateMinMaxType("Frank", "string", "AAAAA", "zzzzz", null));
        System.out.println(true == iv.validateMinMaxType("13:58", "datetime", null, "14:00", "HH:mm"));
        System.out.println(false == iv.validateMinMaxType("27:58", "datetime", null, null, "HH:mm"));
        System.out.println(false == iv.validateMinMaxType("30.02.2005", "datetime", null, null, "dd.MM.yyyy"));
        System.out.println(true == iv.validateMinMaxType("26.02.2005", "datetime", null, null, "dd.MM.yyyy"));
        System.out.println(true == iv.validateMinMaxType("3,5", "decimal", "1", "4", "de"));
        System.out.println(true == iv.validateMinMaxType("3.5", "decimal", "1", "4", "en"));
    }
}
