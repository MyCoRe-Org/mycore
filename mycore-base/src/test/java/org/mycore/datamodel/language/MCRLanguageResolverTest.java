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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

import javax.xml.transform.TransformerException;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.transform.JDOMSource;
import org.junit.Test;
import org.mycore.common.MCRTestCase;

public class MCRLanguageResolverTest extends MCRTestCase {

    @Test
    public void resolve() throws TransformerException {
        // german code
        MCRLanguageResolver languageResolver = new MCRLanguageResolver();
        JDOMSource jdomSource = (JDOMSource) languageResolver.resolve("language:de", "");
        Document document = jdomSource.getDocument();
        assertNotNull(document);
        assertNotNull(document.getRootElement());
        Element languageElement = document.getRootElement();
        assertEquals(2, languageElement.getChildren().size());

        // empty code
        assertThrows(IllegalArgumentException.class, () -> {
            languageResolver.resolve("language:", "");
        });
    }

}
