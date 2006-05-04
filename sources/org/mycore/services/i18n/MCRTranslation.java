/**
 * $RCSfile$
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

import java.text.MessageFormat;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import org.apache.log4j.Logger;

import org.mycore.common.MCRSessionMgr;

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

    private static final Logger LOGGER = Logger.getLogger(MCRTranslation.class);

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
        LOGGER.debug("Translation for current locale: " + currentLocale.getLanguage());
        ResourceBundle message = ResourceBundle.getBundle("/messages", currentLocale);
        String result = message.getString(label);
        LOGGER.debug("Translation for " + label + "=" + result);
        return result;
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
    public static String translate(String label, Object[] arguments) {
        Locale currentLocale = getCurrentLocale();
        MessageFormat formatter = new MessageFormat(translate(label), currentLocale);
        String result = formatter.format(arguments);
        LOGGER.debug("Translation for " + label + "=" + result);
        return result;
    }

    /**
     * provides translation for the given label (property key).
     * 
     * The current locale that is needed for translation is gathered by the
     * language of the current MCRSession. Be aware that any occurence of '(' ,
     * ')' and '\' in <code>argument</code> has to be masked by '\'. You can
     * use '(' and ')' to build an array of arguments: "(foo)(bar)" would result
     * in {"foo","bar"} (the array)
     * 
     * @param label
     * @param argument
     *            String that is inserted instead of placeholders in the
     *            property values
     * @return translated String
     * @see #translate(String, Object[])
     */
    public static String translate(String label, String argument) {
        return translate(label, getStringArray(argument));
    }

    private static Locale getCurrentLocale() {
        return new Locale(MCRSessionMgr.getCurrentSession().getCurrentLanguage());
    }

    private static String[] getStringArray(String masked) {
        List a = new LinkedList();
        boolean mask = false, element = false;
        StringBuffer buf = new StringBuffer();
        if (masked == null) {
            return new String[0];
        }
        if (masked.charAt(0) != '(') {
            a.add(masked);
        } else {
            for (int i = 0; i < masked.length(); i++) {
                switch (masked.charAt(i)) {
                case '(':
                    if (mask) {
                        buf.append('(');
                        mask = false;
                    } else {
                        element = true;
                    }
                    break;
                case ')':
                    if (mask) {
                        buf.append(')');
                        mask = false;
                    } else {
                        element = false;
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
                    if (element) {
                        buf.append(masked.charAt(i));
                    }
                    break;
                }
            }
        }
        return (String[]) a.toArray(new String[a.size()]);
    }

}