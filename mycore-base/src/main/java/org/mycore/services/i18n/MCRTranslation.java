/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
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

package org.mycore.services.i18n;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.ResourceBundle.Control;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilder;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.config.MCRConfigurationDir;
import org.mycore.common.config.MCRProperties;
import org.mycore.common.xml.MCRDOMUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * provides services for internationalization in mycore application. You have to provide a property file named
 * messages.properties in your classpath for this class to work.
 *
 * @author Radi Radichev
 * @author Thomas Scheffler (yagee)
 */
public class MCRTranslation {

    private static final String MESSAGES_BUNDLE = "messages";

    private static final String DEPRECATED_MESSAGES_PROPERTIES = "/deprecated-messages.properties";

    private static final Logger LOGGER = LogManager.getLogger();

    private static final Pattern ARRAY_DETECTOR = Pattern.compile(";");

    private static final Control CONTROL = new MCRCombinedResourceBundleControl();

    private static boolean deprecatedMessagesPresent;

    private static final Properties DEPRECATED_MAPPING = loadProperties();

    private static final Set<String> AVAILABLE_LANGUAGES = loadAvailableLanguages();

    static {
        debug();
    }

    /**
     * provides translation for the given label (property key). The current locale that is needed for translation is
     * gathered by the language of the current MCRSession.
     *
     * @param label property key
     * @return translated String
     */
    public static String translate(String label) {
        return translateToLocale(label, getCurrentLocale());
    }

    /**
     * Checks whether there is a value for the given label and current locale.
     *
     * @param label property key
     * @return <code>true</code> if there is a value, <code>false</code> otherwise
     */
    public static boolean exists(String label) {
        try {
            ResourceBundle message = getResourceBundle(MESSAGES_BUNDLE, getCurrentLocale());
            message.getString(label);
        } catch (MissingResourceException mre) {
            LOGGER.debug(mre);
            return false;
        }
        return true;
    }

    /**
     * provides translation for the given label (property key). The current locale that is needed for translation is
     * gathered by the language of the current MCRSession.
     *
     * @param label property key
     * @param baseName
     *            a fully qualified class name
     * @return translated String
     */

    public static String translateWithBaseName(String label, String baseName) {
        return translateToLocale(label, getCurrentLocale(), baseName);
    }

    /**
     * provides translation for the given label (property key).
     *
     * @param label property key
     * @param locale
     *            target locale of translation
     * @return translated String
     */
    public static String translateToLocale(String label, Locale locale) {
        return translateToLocale(label, locale, MESSAGES_BUNDLE);
    }

    /**
     * Provides translation for the given label (property key) and locale.
     *
     * @param label property key
     * @param locale
     *            target locale of translation
     * @return translated String
     */
    public static String translateToLocale(String label, String locale) {
        return translateToLocale(label, getLocale(locale), MESSAGES_BUNDLE);
    }

    /**
     * provides translation for the given label (property key).
     *
     * @param label property key
     * @param locale
     *            target locale of translation
     * @param baseName
     *            a fully qualified class name
     * @return translated String
     */
    public static String translateToLocale(String label, Locale locale, String baseName) {
        LOGGER.debug("Translation for current locale: {}", locale::getLanguage);
        ResourceBundle message;
        String unresolvedQuestionMarks = "???";
        try {
            message = getResourceBundle(baseName, locale);
        } catch (MissingResourceException mre) {
            //no messages.properties at all
            LOGGER.debug(mre::getMessage);
            return unresolvedQuestionMarks + label + unresolvedQuestionMarks;
        }
        String result = null;
        try {
            result = message.getString(label);
            LOGGER.debug("Translation for {}={}", label, result);
        } catch (MissingResourceException mre) {
            // try to get new key if 'label' is deprecated
            if (!deprecatedMessagesPresent) {
                LOGGER.warn("Could not load resource '" + DEPRECATED_MESSAGES_PROPERTIES
                    + "' to check for deprecated I18N keys.");
            } else if (DEPRECATED_MAPPING.containsKey(label)) {
                String newLabel = DEPRECATED_MAPPING.getProperty(label);
                try {
                    result = message.getString(newLabel);
                } catch (MissingResourceException e) {
                }
                if (result != null) {
                    LOGGER.warn("Usage of deprecated I18N key '{}'. Please use '{}' instead.", label, newLabel);
                    return result;
                }
            }
            result = unresolvedQuestionMarks + label + unresolvedQuestionMarks;
            LOGGER.debug(mre::getMessage);
        }
        return result;
    }

    /**
     * Returns a map of label/value pairs which match with the given prefix. The current locale that is needed for
     * translation is gathered by the language of the current MCRSession.
     *
     * @param prefix
     *            label starts with
     * @return map of labels with translated values
     */
    public static Map<String, String> translatePrefix(String prefix) {
        return translatePrefixToLocale(prefix, getCurrentLocale());
    }

    /**
     * Returns a map of label/value pairs which match with the given prefix.
     *
     * @param prefix
     *            label starts with
     * @param locale
     *            target locale of translation
     * @return map of labels with translated values
     */
    public static Map<String, String> translatePrefixToLocale(String prefix, Locale locale) {
        LOGGER.debug("Translation for locale: {}", locale::getLanguage);
        @SuppressWarnings("PMD.UseConcurrentHashMap")
        Map<String, String> map = new HashMap<>();
        ResourceBundle message = getResourceBundle(MESSAGES_BUNDLE, locale);
        Enumeration<String> keys = message.getKeys();
        while (keys.hasMoreElements()) {
            String key = keys.nextElement();
            if (key.startsWith(prefix)) {
                map.put(key, message.getString(key));
            }
        }
        return Collections.unmodifiableMap(map);
    }

    /**
     * provides translation for the given label (property key). The current locale that is needed for translation is
     * gathered by the language of the current MCRSession.
     *
     * @param label property key
     * @param arguments
     *            Objects that are inserted instead of placeholders in the property values
     * @return translated String
     */
    public static String translate(String label, Object... arguments) {
        if (arguments.length > 0 && arguments[0] instanceof Locale) {
            LOGGER.warn("MCR-2970, MCR-2978: seems like you want to call translateToLocale() instead");
        }
        return translateToLocale(label, getCurrentLocale(), arguments);
    }

    /**
     * Provides translation for the given label (property key).
     *
     * @param label property key
     * @param locale target locale of translation
     * @param arguments Objects that are inserted instead of placeholders in the property values
     * @return translated String
     */
    public static String translateToLocale(String label, Locale locale, Object... arguments) {
        String msgFormat = translateToLocale(label, locale);
        MessageFormat formatter = new MessageFormat(msgFormat, locale);
        String result = formatter.format(arguments);
        LOGGER.debug("Translation for {}={}", label, result);
        return result;
    }

    /**
     * provides translation for the given label (property key). The current locale that is needed for translation is
     * gathered by the language of the current MCRSession. Be aware that any occurence of ';' and '\' in
     * <code>argument</code> has to be masked by '\'. You can use ';' to build an array of arguments: "foo;bar" would
     * result in {"foo","bar"} (the array)
     *
     * @param label property key
     * @param argument
     *            String that is inserted instead of placeholders in the property values
     * @return translated String
     * @see #translate(String, Object[])
     */
    public static String translate(String label, String argument) {
        return translate(label, (Object[]) getStringArray(argument));
    }

    /**
     * Provides translation for the given label (property key). Be aware that any occurence of ';' and '\' in
     * <code>argument</code> has to be masked by '\'. You can use ';' to build an array of arguments: "foo;bar" would
     * result in {"foo","bar"} (the array)
     *
     * @param label property key
     * @param argument
     *            String that is inserted instead of placeholders in the property values
     * @param locale target locale of translation
     * @return translated String
     * @see #translate(String, Object[])
     */
    public static String translateToLocale(String label, String argument, Locale locale) {
        return translateToLocale(label, locale, (Object[]) getStringArray(argument));
    }

    /**
     * Provides translation for the given label (property key). Be aware that any occurence of ';' and '\' in
     * <code>argument</code> has to be masked by '\'. You can use ';' to build an array of arguments: "foo;bar" would
     * result in {"foo","bar"} (the array)
     *
     * @param label property key
     * @param argument
     *            String that is inserted instead of placeholders in the property values
     * @param locale target locale of translation
     * @return translated String
     * @see #translate(String, Object[])
     */
    public static String translateToLocale(String label, String argument, String locale) {
        return translateToLocale(label, argument, getLocale(locale));
    }

    public static Locale getCurrentLocale() {
        String currentLanguage = MCRSessionMgr.getCurrentSession().getCurrentLanguage();
        return getLocale(currentLanguage);
    }

    public static Locale getLocale(String language) {
        String adjustedLanguage;
        if (language.equals("id")) {
            // workaround for bug with indonesian
            // INDONESIAN      ID     OCEANIC/INDONESIAN [*Changed 1989 from original ISO 639:1988, IN]
            // Java doesn't work with id
            adjustedLanguage = "in";
            LOGGER.debug("Translation for current locale: {}", adjustedLanguage);
        } else {
            adjustedLanguage = language;
        }
        return Locale.forLanguageTag(adjustedLanguage);
    }

    public static Set<String> getAvailableLanguages() {
        return AVAILABLE_LANGUAGES;
    }

    public static Document getAvailableLanguagesAsXML() {
        DocumentBuilder documentBuilder = MCRDOMUtils.getDocumentBuilderUnchecked();
        try {
            Document document = documentBuilder.newDocument();
            Element i18nRoot = document.createElement("i18n");
            document.appendChild(i18nRoot);
            for (String lang : AVAILABLE_LANGUAGES) {
                Element langElement = document.createElement("lang");
                langElement.setTextContent(lang);
                i18nRoot.appendChild(langElement);
            }
            return document;
        } finally {
            MCRDOMUtils.releaseDocumentBuilder(documentBuilder);
        }
    }

    static String[] getStringArray(String masked) {
        List<String> a = new ArrayList<>();
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
                    case ';' -> {
                        if (mask) {
                            buf.append(';');
                            mask = false;
                        } else {
                            a.add(buf.toString());
                            buf.setLength(0);
                        }
                    }
                    case '\\' -> {
                        if (mask) {
                            buf.append('\\');
                            mask = false;
                        } else {
                            mask = true;
                        }
                    }
                    default -> buf.append(masked.charAt(i));
                }
            }
            a.add(buf.toString());
        }
        return a.toArray(String[]::new);
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

    @SuppressWarnings("PMD.MCR.ResourceResolver")
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
            deprecatedMessagesPresent = true;
        } catch (IOException e) {
            LOGGER.warn("Could not load resource '" + DEPRECATED_MESSAGES_PROPERTIES + "'.", e);
        }
        return deprecatedMapping;
    }

    static Set<String> loadAvailableLanguages() {
        // try to load application relevant languages
        return MCRConfiguration2.getString("MCR.Metadata.Languages")
            .map(MCRConfiguration2::splitValue)
            .map(s -> s.collect(Collectors.toSet()))
            .orElseGet(MCRTranslation::loadLanguagesByMessagesBundle);//all languages by available messages_*.properties
    }

    static Set<String> loadLanguagesByMessagesBundle() {
        Set<String> languages = new HashSet<>();
        for (Locale locale : Locale.getAvailableLocales()) {
            try {
                if (!locale.getLanguage().isEmpty()) {
                    ResourceBundle bundle = getResourceBundle(MESSAGES_BUNDLE, locale);
                    languages.add(bundle.getLocale().toString());
                }
            } catch (MissingResourceException e) {
                LOGGER.debug("Could not load " + MESSAGES_BUNDLE + " for locale: {}", locale);
            }
        }
        return languages;
    }

    public static ResourceBundle getResourceBundle(String baseName, Locale locale) {
        return baseName.contains(".") ? ResourceBundle.getBundle(baseName, locale)
            : ResourceBundle.getBundle("stacked:" + baseName, locale, CONTROL);
    }

    /**
     * output the current message properties to configuration directory
     */
    private static void debug() {
        for (String lang : getAvailableLanguages()) {
            ResourceBundle rb = getResourceBundle("messages", getLocale(lang));
            Properties props = new MCRProperties();
            rb.keySet().forEach(key -> props.put(key, rb.getString(key)));
            File resolvedMsgFile = MCRConfigurationDir.getConfigFile("messages_" + lang + ".resolved.properties");
            if (resolvedMsgFile != null) {
                try (OutputStream os = Files.newOutputStream(resolvedMsgFile.toPath())) {
                    props.store(os, "MyCoRe Messages for Locale " + lang);
                } catch (IOException e) {
                    LOGGER.warn(() -> "Could not store resolved properties to " + resolvedMsgFile.getAbsolutePath(), e);
                }
            }
        }
    }

    /**
     * use with care: only required for Junit tests if properties changes.
     *
     * <pre>
     * MCR.Metadata.Languages=…
     * </pre>
     */
    static void reInit() {
        synchronized (AVAILABLE_LANGUAGES) {
            Set<String> newLanguages = loadAvailableLanguages();
            AVAILABLE_LANGUAGES.clear();
            AVAILABLE_LANGUAGES.addAll(newLanguages);
        }
    }
}
