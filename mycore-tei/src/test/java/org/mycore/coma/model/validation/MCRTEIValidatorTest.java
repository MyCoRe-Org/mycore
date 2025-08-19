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

package org.mycore.coma.model.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URL;

import javax.xml.transform.stream.StreamSource;

import org.junit.jupiter.api.Test;
import org.mycore.tei.MCRTEIValidator;
import org.mycore.test.MyCoReTest;
import org.xml.sax.SAXException;

@MyCoReTest
public class MCRTEIValidatorTest {

    @Test
    void validate() throws IOException, SAXException {
        MCRTEIValidator teiValidator = getTeiValidator("xml/validTei.xml");
        teiValidator.validate();

        assertEquals(0, teiValidator.getErrors().size() + teiValidator.getFatals().size());
    }

    @Test
    void validateFail() throws IOException, SAXException {
        MCRTEIValidator teiValidator = getTeiValidator("xml/invalidTei.xml");
        teiValidator.validate();

        assertTrue(teiValidator.getErrors().size() + teiValidator.getFatals().size() > 0);
    }

    private MCRTEIValidator getTeiValidator(String path) throws IOException {
        URL resource = MCRTEIValidatorTest.class.getClassLoader().getResource(path);
        return new MCRTEIValidator(new StreamSource(resource.openStream()));
    }

}
