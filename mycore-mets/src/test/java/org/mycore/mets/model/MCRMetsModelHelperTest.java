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

package org.mycore.mets.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.test.MyCoReTest;

@MyCoReTest
@MCRTestConfiguration(properties = {
    @MCRTestProperty(key = MCRMetsModelHelper.ALLOWED_TRANSLATION_PROPERTY, string = "de,en")
})
public class MCRMetsModelHelperTest {

    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
            { "bild.jpg", Optional.of(MCRMetsModelHelper.MASTER_USE) },
            { "alto/datei1.xml", Optional.of(MCRMetsModelHelper.ALTO_USE) },
            { "tei/transcription/page1.xml", Optional.of(MCRMetsModelHelper.TRANSCRIPTION_USE) },
            { "tei/transcription/page2.xml", Optional.of(MCRMetsModelHelper.TRANSCRIPTION_USE) },
            { "tei/translation/page1.xml", Optional.empty() },
            { "tei/text.xml", Optional.of(MCRMetsModelHelper.MASTER_USE) },
            { "tei/translation.de/page1.xml", Optional.of(MCRMetsModelHelper.TRANSLATION_USE + ".DE") },
            { "tei/translation.en/page2.xml", Optional.of(MCRMetsModelHelper.TRANSLATION_USE + ".EN") },
            { "tei/translation.kr/page2.xml", Optional.empty() },
        });
    }

    @ParameterizedTest
    @MethodSource("data")
    public void getUseForHref(String path, Optional<String> expected) {
        assertEquals(expected, MCRMetsModelHelper.getUseForHref(path));
    }

}
