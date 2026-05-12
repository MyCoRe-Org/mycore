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

package org.mycore.common.xsl.uriresolver;

import java.util.HashMap;
import java.util.Map;

import javax.xml.transform.Source;
import javax.xml.transform.URIResolver;

import org.jdom2.Element;
import org.jdom2.transform.JDOMSource;
import org.mycore.services.i18n.MCRTranslation;

public class MCRI18NResolver implements URIResolver {

    /**
     * Resolves the I18N String value for the given property.<br><br>
     * <br>
     * Syntax: <code>i18n:{i18n-code},{i18n-prefix}*,{i18n-prefix}*...</code> or <br>
     *         <code>i18n:{i18n-code}[:param1:param2:…paramN]</code>
     * <br>
     * Result: <code> <br>
     *     &lt;i18n&gt; <br>
     *   &lt;translation key=&quot;key1&quot;&gt;translation1&lt;/translation&gt; <br>
     *   &lt;translation key=&quot;key2&quot;&gt;translation2&lt;/translation&gt; <br>
     *   &lt;translation key=&quot;key3&quot;&gt;translation3&lt;/translation&gt; <br>
     * &lt;/i18n&gt; <br>
     * </code>
     * <br/>
     * If just one i18n-code is passed, then the translation element is skipped.
     * <code>
     *     &lt;i18n&gt; <br>translation&lt;/i18n&gt;<br>
     * </code>
     * Additionally, if the singular i18n-code is followed by a ":"-separated list of values,
     * the translation result is interpreted to be in Java MessageFormat and will be formatted with those values.
     * E.g.
     * <code>i18n:module.dptbase.common.results.nResults:15</code> (<code>{0} objects found</code>)
     *  -> "<code>15 objects found</code>"
     * @param href
     *            URI in the syntax above
     * @param base
     *            not used
     * @return the element with result format above
     * @see javax.xml.transform.URIResolver
     */
    @Override
    public Source resolve(String href, String base) {
        String target = href.substring(href.indexOf(':') + 1);

        final Element i18nElement = new Element("i18n");
        if (!target.contains("*") && !target.contains(",")) {
            String translation;
            if (target.contains(":")) {
                final int i = target.indexOf(':');
                translation = MCRTranslation.translate(target.substring(0, i),
                    (Object[]) target.substring(i + 1).split(":"));
            } else {
                translation = MCRTranslation.translate(target);
            }
            i18nElement.addContent(translation);
            return new JDOMSource(i18nElement);
        }

        final String[] translationKeys = target.split(",");

        // Combine translations to prevent duplicates
        Map<String, String> translations = new HashMap<>();
        for (String translationKey : translationKeys) {
            if (translationKey.endsWith("*")) {
                final String prefix = translationKey.substring(0, translationKey.length() - 1);
                translations.putAll(MCRTranslation.translatePrefix(prefix));
            } else {
                translations.put(translationKey,
                    MCRTranslation.translate(translationKey));
            }
        }

        translations.forEach((key, value) -> {
            final Element translation = new Element("translation");
            translation.setAttribute("key", key);
            translation.setText(value);
            i18nElement.addContent(translation);
        });

        return new JDOMSource(i18nElement);
    }

}
