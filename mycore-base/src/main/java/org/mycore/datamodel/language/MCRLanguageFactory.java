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

package org.mycore.datamodel.language;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.backend.jpa.MCREntityManagerProvider;
import org.mycore.common.MCRConstants;
import org.mycore.common.MCRTransactionManager;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryDAO;
import org.mycore.datamodel.classifications2.MCRCategoryDAOFactory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.classifications2.MCRLabel;

/**
 * Returns MCRLanguage instances. The languages most commonly used, English and German,
 * are provided as constants. Other languages are read from a classification thats ID can be
 * configured using the property "MCR.LanguageClassification". That classification should use
 * ISO 639-1 code as category ID, where ISO 639-2 codes can be added by extra labels x-term and x-bibl
 * for the category. Unknown languages are created by code as required, but a warning is logged.
 *
 * @author Frank Lützenkirchen
 */
public final class MCRLanguageFactory {

    private static final Logger LOGGER = LogManager.getLogger();

    public static final MCRLanguage GERMAN = obtainInstance().getLanguage("de");

    public static final MCRLanguage ENGLISH = obtainInstance().getLanguage("en");

    /**
     * Map of languages by ISO 639-1 or -2 code
     */
    private final Map<String, MCRLanguage> languageByCode = new HashMap<>();

    /**
     * The ID of the classification containing the language codes and labels
     */
    private final MCRCategoryID classification;

    private final MCRCategoryDAO categoryDAO = MCRCategoryDAOFactory.obtainInstance();

    /**
     * Language classification may change at runtime, so we remember the time we last read the languages in.
     */
    private long classificationLastRead = Long.MIN_VALUE;

    /**
     * The default language is configured via "MCR.Metadata.DefaultLang"
     */
    private final String codeOfDefaultLanguage;

    public MCRLanguageFactory() {
        this(MCRConfiguration2.getString("MCR.Metadata.DefaultLang").orElse(MCRConstants.DEFAULT_LANG),
            MCRConfiguration2.getString("MCR.LanguageClassification").map(MCRCategoryID::new).orElse(null));
    }

    public MCRLanguageFactory(String codeOfDefaultLanguage, MCRCategoryID classification) {
        this.codeOfDefaultLanguage = codeOfDefaultLanguage;
        this.classification = classification;
        initDefaultLanguages();
    }

    public static MCRLanguageFactory obtainInstance() {
        return LazyInstanceHolder.SHARED_INSTANCE;
    }

    /**
     * Returns the default language, as configured by "MCR.Metadata.DefaultLang"
     */
    public MCRLanguage getDefaultLanguage() {
        return getLanguage(codeOfDefaultLanguage);
    }

    /**
     * Returns the language with the given ISO 639-1 or -2 code. When the given code contains a
     * subcode like "en-us", and the main language is configured, that main language is returned.
     * When the given code does not match any language configured, a language is created on-the-fly,
     * but a warning is logged.
     */
    public MCRLanguage getLanguage(String code) {
        if (classificationHasChanged()) {
            initLanguages();
        }

        return lookupLanguage(code);
    }

    private MCRLanguage lookupLanguage(String code) {
        String languageCode;
        if ((!languageByCode.containsKey(code)) && code.contains("-") && !code.startsWith("x-")) {
            languageCode = code.split("-")[0];
        } else {
            languageCode = code;
        }
        if (!languageByCode.containsKey(languageCode)) {
            LOGGER.warn("Unknown language: {}", languageCode);
            buildLanguage(languageCode, languageCode.length() > 2 ? languageCode : null, null);
        }

        return languageByCode.get(languageCode);
    }

    /**
     * This method check the language string base on RFC 1766 to the supported
     * languages in MyCoRe in a current application environment. Without appending
     * this MCRLanguageFactory only ENGLISH and GERMAN are supported.
     *
     * @param code
     *            the language string in RFC 1766 syntax
     * @return true if the language code is supported. It return true too if the code starts
     *            with x- or i-, otherwise return false;
     */
    public boolean isSupportedLanguage(String code) {
        if (code == null) {
            return false;
        }
        String codeTrimmed = code.trim();
        if (codeTrimmed.isEmpty()) {
            return false;
        }
        if (codeTrimmed.startsWith("x-") || codeTrimmed.startsWith("i-")) {
            return true;
        }
        return languageByCode.containsKey(codeTrimmed);
    }

    /**
     * Checks if any classification has changed in the persistent store, so that the languages
     * should be read again.
     */
    private boolean classificationHasChanged() {
        //TODO: remove usage of MCREntityManagerProvider
        return MCRConfiguration2.getBoolean("MCR.Persistence.Database.Enable").orElse(true)
            && MCREntityManagerProvider.getEntityManagerFactory() != null && (classification != null)
            && (categoryDAO.getLastModified() > classificationLastRead);
    }

    /**
     * Builds the default languages and reads in the languages configured by classification
     */
    private void initLanguages() {
        languageByCode.clear();
        initDefaultLanguages();
        readLanguageClassification();
    }

    /**
     * Builds the default languages
     */
    private void initDefaultLanguages() {
        MCRLanguage de = buildLanguage("de", "deu", "ger");
        MCRLanguage en = buildLanguage("en", "eng", null);
        de.setLabel(de, "Deutsch");
        de.setLabel(en, "German");
        en.setLabel(de, "Englisch");
        en.setLabel(en, "English");
    }

    /**
     * Builds a new language object with the given ISO codes.
     *
     * @param xmlCode ISO 639-1 code as used for xml:lang
     * @param termCode ISO 639-2 terminologic code, may be null
     * @param biblCode ISO 639-2 bibliographical code, may be null
     */
    private MCRLanguage buildLanguage(String xmlCode, String termCode, String biblCode) {
        MCRLanguage language = new MCRLanguage();
        addCode(language, MCRLanguageCodeType.XML_CODE, xmlCode);
        if (termCode != null) {
            addCode(language, MCRLanguageCodeType.TERM_CODE, termCode);
            addCode(language, MCRLanguageCodeType.BIBL_CODE, biblCode == null ? termCode : biblCode);
        }
        Locale locale = Arrays.stream(Locale.getAvailableLocales())
            .filter(l -> l.toString().equals(xmlCode))
            .findFirst()
            .orElseGet(() -> Locale.forLanguageTag(xmlCode));
        language.setLocale(locale);
        return language;
    }

    /**
     * Adds and registers the code for the language
     */
    private void addCode(MCRLanguage language, MCRLanguageCodeType type, String code) {
        language.setCode(type, code);
        languageByCode.put(code, language);
    }

    /**
     * Reads in the language classification and builds language objects from its categories
     */
    private void readLanguageClassification() {
        if (!MCRTransactionManager.hasActiveTransactions()) {
            MCRTransactionManager.beginTransactions();
            buildLanguagesFromClassification();
            MCRTransactionManager.commitTransactions();
        } else {
            buildLanguagesFromClassification();
        }
    }

    /**
     * Builds language objects from classification categories
     */
    private void buildLanguagesFromClassification() {
        this.classificationLastRead = categoryDAO.getLastModified();

        MCRCategory root = categoryDAO.getCategory(classification, -1);
        if (root == null) {
            LOGGER.warn("Language classification {} not found", classification::getRootID);
            return;
        }

        for (MCRCategory category : root.getChildren()) {
            buildLanguage(category);
        }
    }

    /**
     * Builds a new language object from the given category
     */
    private void buildLanguage(MCRCategory category) {
        String xmlCode = category.getId().getId();
        String termCode = category.getLabel("x-term").map(MCRLabel::getText).orElse(null);
        String biblCode = category.getLabel("x-bibl").map(MCRLabel::getText).orElse(termCode);

        MCRLanguage language = buildLanguage(xmlCode, termCode, biblCode);

        category
            .getLabels()
            .stream()
            .filter(l -> !l.getLang().startsWith("x-"))
            .sequential() //MCRLanguage is not thread safe
            .forEach(l -> {
                MCRLanguage languageOfLabel = lookupLanguage(l.getLang());
                language.setLabel(languageOfLabel, l.getText());
            });
    }

    private static final class LazyInstanceHolder {
        public static final MCRLanguageFactory SHARED_INSTANCE = new MCRLanguageFactory();
    }

}
