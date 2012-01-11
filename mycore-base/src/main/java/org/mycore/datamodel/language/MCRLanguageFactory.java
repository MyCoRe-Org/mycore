/*
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

package org.mycore.datamodel.language;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryDAO;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.classifications2.MCRLabel;
import org.mycore.datamodel.classifications2.impl.MCRCategoryDAOImpl;

/**
 * Returns MCRLanguage instances. The languages most commonly used, English and German,
 * are provided as constants. Other languages are read from a classification thats ID can be
 * configured using the property "MCR.LanguageClassification". That classification should use
 * ISO 639-1 code as category ID, where ISO 639-2 codes can be added by extra labels x-term and x-bibl
 * for the category. Unknown languages are created by code as required, but a warning is logged. 
 * 
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRLanguageFactory {

    private static Logger LOGGER = Logger.getLogger(MCRLanguageFactory.class);

    private static MCRLanguageFactory singleton = new MCRLanguageFactory();

    /**
     * Returns the MCRLanguageFactory singleton
     */
    public static MCRLanguageFactory instance() {
        return singleton;
    }

    /**
     * Map of languages by ISO 639-1 or -2 code
     */
    private Map<String, MCRLanguage> languageByCode = new HashMap<String, MCRLanguage>();

    /**
     * The ID of the classification containing the language codes and labels
     */
    private MCRCategoryID classification = null;

    private MCRCategoryDAO DAO = new MCRCategoryDAOImpl();

    /**
     * Language classification may change at runtime, so we remember the time we last read the languages in.
     */
    private long classificationLastRead = Long.MIN_VALUE;

    /**
     * The default language is configured via "MCR.Metadata.DefaultLang"
     */
    private String codeOfDefaultLanguage;

    private MCRLanguageFactory() {
        MCRConfiguration config = MCRConfiguration.instance();
        codeOfDefaultLanguage = config.getString("MCR.Metadata.DefaultLang");
        buildDefaultLanguages();

        String classificationID = config.getString("MCR.LanguageClassification", null);
        if (classificationID != null)
            classification = new MCRCategoryID(classificationID, null);
    }

    /**
     * Returns the default langauge, as configured by "MCR.Metadata.DefaultLang"
     */
    public MCRLanguage getDefaultLanguage() {
        return getLanguage(codeOfDefaultLanguage);
    }

    public final static MCRLanguage GERMAN = MCRLanguageFactory.instance().getLanguage("de");

    public final static MCRLanguage ENGLISH = MCRLanguageFactory.instance().getLanguage("en");

    /**
     * Returns the language with the given ISO 639-1 or -2 code. When the given code contains a
     * subcode like "en-us", and the main language is configured, that main language is returned.
     * When the given code does not match any language configured, a language is created on-the-fly,
     * but a warning is logged. 
     */
    public MCRLanguage getLanguage(String code) {
        if (classificationHasChanged())
            buildLanguages();

        if ((!languageByCode.containsKey(code)) && code.contains("-") && !code.startsWith("x-"))
            code = code.split("-")[0];

        if (!languageByCode.containsKey(code)) {
            LOGGER.warn("Unknown language: " + code);
            buildLanguage(code, code, null);
        }

        return languageByCode.get(code);
    }

    /**
     * Checks if any classification has changed in the persistent store, so that the languages
     * should be read again.
     */
    private boolean classificationHasChanged() {
        return (classification != null) && (DAO.getLastModified() > classificationLastRead);
    }

    /**
     * Builds the default languages and reads in the languages configured by classification
     */
    private void buildLanguages() {
        languageByCode.clear();
        buildDefaultLanguages();
        readLanguageClassification();
    }

    /**
     * Builds the default languages 
     */
    private void buildDefaultLanguages() {
        buildLanguage("de", "deu", "ger");
        buildLanguage("en", "eng", null);
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
        addCode(language, MCRLanguageCodeType.xmlCode, xmlCode);
        if (termCode != null) {
            addCode(language, MCRLanguageCodeType.termCode, termCode);
            addCode(language, MCRLanguageCodeType.biblCode, biblCode == null ? termCode : biblCode);
        }
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
        MCRSession session = MCRSessionMgr.getCurrentSession();
        if (!session.isTransactionActive()) {
            session.beginTransaction();
            buildLanguagesFromClassification();
            session.commitTransaction();
        } else
            buildLanguagesFromClassification();
    }

    /**
     * Builds language objects from classification categories
     */
    private void buildLanguagesFromClassification() {
        this.classificationLastRead = DAO.getLastModified();

        MCRCategory root = DAO.getCategory(classification, -1);
        if (root == null) {
            LOGGER.warn("Language classification " + classification.getRootID() + " not found");
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
        String xmlCode = category.getId().getID();
        MCRLabel lTermCode = category.getLabel("x-term");
        MCRLabel lBiblCode = category.getLabel("x-bibl");
        String termCode = lTermCode == null ? null : lTermCode.getText();
        String biblCode = lBiblCode == null ? termCode : lBiblCode.getText();
        buildLanguage(xmlCode, termCode, biblCode);
    }
}