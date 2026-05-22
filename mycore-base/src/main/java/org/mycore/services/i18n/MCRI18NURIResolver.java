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

import java.util.HashMap;
import java.util.Map;

import javax.xml.transform.Source;
import javax.xml.transform.URIResolver;

import org.jdom2.Element;
import org.jdom2.transform.JDOMSource;

/**
 * {@link URIResolver} that resolves i18n translation keys and returns the results as XML.
 */
public class MCRI18NURIResolver implements URIResolver {

    /**
     * Resolves one or more i18n translation keys and returns the translations as an XML source.
     * <p>For a single key, the translation is returned as the text content of the {@code <i18n>}
     * element directly. For multiple keys or prefix wildcards, each translation is wrapped in a
     * {@code <translation>} child element. A single key may be followed by {@code :}-separated
     * {@link java.text.MessageFormat} arguments.
     * <p>URI Syntax:
     * <pre>
     *   &lt;scheme&gt;:{i18n-key}[:{arg}...]
     *   &lt;scheme&gt;:{i18n-key},{i18n-key},...
     *   &lt;scheme&gt;:{i18n-prefix}*[,{i18n-prefix}*,...]
     * </pre>
     * <p>Example request:
     * <pre>
     *   i18n:module.results.nResults:15
     *   i18n:module.title,module.description
     *   i18n:module.*
     * </pre>
     * <p>Example response for a single key:
     * <pre>{@code
     *   <i18n>15 objects found</i18n>
     * }</pre>
     * <p>Example response for multiple keys:
     * <pre>{@code
     *   <i18n>
     *     <translation key="module.title">My Title</translation>
     *     <translation key="module.description">My Description</translation>
     *   </i18n>
     * }</pre>
     *
     * @param href the URI in the syntax above to resolve
     * @param base the base URI of the calling stylesheet (unused)
     * @return a {@link JDOMSource} wrapping the {@code <i18n>} element
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
