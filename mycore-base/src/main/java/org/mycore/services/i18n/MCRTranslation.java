/**
 * 
 * $Revision$ $Date$
 *
 * This file is part of ** M y C o R e **
 * Visit our homepage at http://www.mycore.de/ for details.
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
 * along with this program, normally in the file license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 *
 **/
package org.mycore.services.i18n;

import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.mycore.common.MCRSessionMgr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * provides services for internationalization in mycore application.
 * 
 * You have to provide a property file named messages.properties in your
 * classpath for this class to work.
 * 
 * @author Radi Radichev
 * @author Thomas Scheffler (yagee)
 */
public class MCRTranslation {

    private static final String MESSAGES_BUNDLE = "messages";

    private static final String DEPRECATED_MESSAGES_PROPERTIES = "/deprecated-messages.properties";

    private static final Logger LOGGER = Logger.getLogger(MCRTranslation.class);

    private static final Pattern ARRAY_DETECTOR = Pattern.compile(";");

    private static boolean DEPRECATED_MESSAGES_PRESENT = false;

    private static Properties DEPRECATED_MAPPING = loadProperties();

    private static Set<String> AVAILABLE_LANGUAGES = loadAvailableLanguages();

    private static final ThreadLocal<DocumentBuilder> BUILDER_LOCAL = new ThreadLocal<DocumentBuilder>() {
        @Override
        protected DocumentBuilder initialValue() {
            try {
                return DocumentBuilderFactory.newInstance().newDocumentBuilder();
            } catch (ParserConfigurationException pce) {
                LOGGER.error("Unable to create document builder", pce);
                return null;
            }
        }
    };

    /**
     * provides translation for the given label (property key).
     * 
     * The current locale that is needed for translation is gathered by the
     * language of the current MCRSession.
     * 
     * @param label
     * @return translated String
     */
    public static String translate(String label) {
        Locale currentLocale = getCurrentLocale();
        return translate(label, currentLocale);
    }

    /**
     * provides translation for the given label (property key).
     * 
     * The current locale that is needed for translation is gathered by the
     * language of the current MCRSession.
     * 
     * @param label
     * @param baseName a fully qualified class name 
     * @return translated String
     */

    public static String translateWithBaseName(String label, String baseName) {
        Locale currentLocale = getCurrentLocale();
        return translate(label, currentLocale, baseName);
    }

    /**
     * provides translation for the given label (property key).
     * 
     * @param label
     * @param locale target locale of translation 
     * @return translated String
     */
    public static String translate(String label, Locale locale) {
        return translate(label, locale, MESSAGES_BUNDLE);
    }

    /**
     * provides translation for the given label (property key).
     * 
     * @param label
     * @param locale target locale of translation 
     * @param baseName a fully qualified class name 
     * @return translated String
     */
    public static String translate(String label, Locale locale, String baseName) {
        LOGGER.debug("Translation for current locale: " + locale.getLanguage());
        ResourceBundle message;
        try {
            message = ResourceBundle.getBundle(baseName, locale);
        } catch (MissingResourceException mre) {
            //no messages.properties at all
            LOGGER.debug(mre.getMessage());
            return "???" + label + "???";
        }
        String result = null;
        try {
            result = message.getString(label);
            LOGGER.debug("Translation for " + label + "=" + result);
        } catch (MissingResourceException mre) {
            // try to get new key if 'label' is deprecated
            if (!DEPRECATED_MESSAGES_PRESENT) {
                LOGGER.warn("Could not load resource '" + DEPRECATED_MESSAGES_PROPERTIES
                    + "' to check for depreacted I18N keys.");
            } else if (DEPRECATED_MAPPING.keySet().contains(label)) {
                String newLabel = DEPRECATED_MAPPING.getProperty(label);
                try {
                    result = message.getString(newLabel);
                } catch (java.util.MissingResourceException e) {
                }
                if (result != null) {
                    LOGGER.warn("Usage of deprected I18N key '" + label + "'. Please use '" + newLabel + "' instead.");
                    return result;
                }
            }
            result = "???" + label + "???";
            LOGGER.debug(mre.getMessage());
        }
        return result;
    }

    /**
     * Returns a map of label/value pairs which match with the given prefix.
     * 
     * The current locale that is needed for translation is gathered by the
     * language of the current MCRSession.
     * 
     * @param prefix label starts with
     * @return map of labels with translated values
     */
    public static Map<String, String> translatePrefix(String prefix) {
        Locale currentLocale = getCurrentLocale();
        return translatePrefix(prefix, currentLocale);
    }

    /**
     * Returns a map of label/value pairs which match with the given prefix.
     * 
     * @param prefix label starts with
     * @param locale target locale of translation 
     * @return map of labels with translated values
     */
    public static Map<String, String> translatePrefix(String prefix, Locale locale) {
        LOGGER.debug("Translation for locale: " + locale.getLanguage());
        HashMap<String, String> map = new HashMap<String, String>();
        ResourceBundle message = ResourceBundle.getBundle(MESSAGES_BUNDLE, locale);
        Enumeration<String> keys = message.getKeys();
        while (keys.hasMoreElements()) {
            String key = keys.nextElement();
            if (key.startsWith(prefix)) {
                map.put(key, message.getString(key));
            }
        }
        return map;
    }

    /**
     * provides translation for the given label (property key).
     * 
     * The current locale that is needed for translation is gathered by the
     * language of the current MCRSession.
     * 
     * @param label
     * @param arguments
     *            Objects that are inserted instead of placeholders in the
     *            property values
     * @return translated String
     */
    public static String translate(String label, Object... arguments) {
        Locale currentLocale = getCurrentLocale();
        String msgFormat = translate(label);
        MessageFormat formatter = new MessageFormat(msgFormat, currentLocale);
        String result = formatter.format(arguments);
        LOGGER.debug("Translation for " + label + "=" + result);
        return result;
    }

    /**
     * provides translation for the given label (property key).
     * 
     * The current locale that is needed for translation is gathered by the
     * language of the current MCRSession. Be aware that any occurence of ';'
     * and '\' in <code>argument</code> has to be masked by '\'. You can use
     * ';' to build an array of arguments: "foo;bar" would result in
     * {"foo","bar"} (the array)
     * 
     * @param label
     * @param argument
     *            String that is inserted instead of placeholders in the
     *            property values
     * @return translated String
     * @see #translate(String, Object[])
     */
    public static String translate(String label, String argument) {
        return translate(label, (Object[]) getStringArray(argument));
    }

    public static Locale getCurrentLocale() {
        String currentLanguage = MCRSessionMgr.getCurrentSession().getCurrentLanguage();
        return getLocale(currentLanguage);
    }

    public static Locale getLocale(String language) {
        if (language.equals("id")) {
            // workaround for bug with indonesian
            // INDONESIAN      ID     OCEANIC/INDONESIAN [*Changed 1989 from original ISO 639:1988, IN]
            // Java doesn't work with id
            language = "in";
            LOGGER.debug("Translation for current locale: " + language);
        }
        Locale locale = new Locale(language);
        return locale;
    }

    public static Set<String> getAvailableLanguages() {
        return AVAILABLE_LANGUAGES;
    }

    public static Document getAvailableLanguagesAsXML() {
        Document document = BUILDER_LOCAL.get().newDocument();
        Element i18nRoot = document.createElement("i18n");
        document.appendChild(i18nRoot);
        for (String lang : AVAILABLE_LANGUAGES) {
            Element langElement = document.createElement("lang");
            langElement.setTextContent(lang);
            i18nRoot.appendChild(langElement);
        }
        return document;
    }

    static String[] getStringArray(String masked) {
        List<String> a = new LinkedList<String>();
        boolean mask = false;
        StringBuilder buf = new StringBuilder();
        if (masked == null) {
            return new String[0];
        }
        if (!isArray(masked)) {
            a.add(masked);
        } else {
            for (int i = 0; i < masked.length(); i++) {
                switch (masked.charAt(i)) {
                    case ';':
                        if (mask) {
                            buf.append(';');
                            mask = false;
                        } else {
                            a.add(buf.toString());
                            buf.setLength(0);
                        }
                        break;
                    case '\\':
                        if (mask) {
                            buf.append('\\');
                            mask = false;
                        } else {
                            mask = true;
                        }
                        break;
                    default:
                        buf.append(masked.charAt(i));
                        break;
                }
            }
            a.add(buf.toString());
        }
        return a.toArray(new String[a.size()]);
    }

    static boolean isArray(String masked) {
        Matcher m = ARRAY_DETECTOR.matcher(masked);
        while (m.find()) {
            int pos = m.start();
            int count = 0;
            for (int i = pos - 1; i > 0; i--) {
                if (masked.charAt(i) == '\\') {
                    count++;
                } else {
                    break;
                }
            }
            if (count % 2 == 0) {
                return true;
            }
        }
        return false;
    }

    static Properties loadProperties() {
        Properties deprecatedMapping = new Properties();
        try {
            final InputStream propertiesStream = MCRTranslation.class
                .getResourceAsStream(DEPRECATED_MESSAGES_PROPERTIES);
            if (propertiesStream == null) {
                LOGGER.warn("Could not find resource '" + DEPRECATED_MESSAGES_PROPERTIES + "'.");
                return deprecatedMapping;
            }
            deprecatedMapping.load(propertiesStream);
            DEPRECATED_MESSAGES_PRESENT = true;
        } catch (IOException e) {
            LOGGER.warn("Could not load resource '" + DEPRECATED_MESSAGES_PROPERTIES + "'.", e);
        }
        return deprecatedMapping;
    }

    static Set<String> loadAvailableLanguages() {
        Set<String> languages = new HashSet<String>();
        for (Locale locale : Locale.getAvailableLocales()) {
            try {
                ResourceBundle bundle = ResourceBundle.getBundle(MESSAGES_BUNDLE, locale);
                languages.add(bundle.getLocale().toString());
            } catch (MissingResourceException e) {
                LOGGER.debug("Could not load " + MESSAGES_BUNDLE + " for locale: " + locale);
            }
        }
        return languages;
    }

}
