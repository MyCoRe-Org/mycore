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

package org.mycore.mods.dedup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.jdom2.Element;
import org.junit.jupiter.api.Test;
import org.mycore.common.MCRConstants;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.dedup.MCRDeDupCriteriaProvider;
import org.mycore.dedup.MCRDeDupCriterion;
import org.mycore.mods.MCRMODSWrapper;
import org.mycore.test.MyCoReTest;

@MyCoReTest
public class MCRMODSDeDupCriterionBuilderTest {

    private static final MCRMODSIdentifierDeDupCriterionBuilder IDENTIFIER_BUILDER =
        new MCRMODSIdentifierDeDupCriterionBuilder();

    private static final MCRMODSTitleAuthorDeDupCriterionBuilder TITLE_AUTHOR_BUILDER =
        new MCRMODSTitleAuthorDeDupCriterionBuilder();

    @Test
    public void identifierHyphensAreNormalized() {
        assertEquals(IDENTIFIER_BUILDER.buildFromIdentifier("isbn", "978-1-56619-909-4"),
            IDENTIFIER_BUILDER.buildFromIdentifier("isbn", "9781566199094"));
    }

    @Test
    public void identifierTypeIsRelevant() {
        assertNotEquals(IDENTIFIER_BUILDER.buildFromIdentifier("doi", "123"),
            IDENTIFIER_BUILDER.buildFromIdentifier("duepublico", "123"));
    }

    @Test
    public void titleAuthorIsCaseInsensitive() {
        assertEquals(TITLE_AUTHOR_BUILDER.buildFromTitleAuthor("THIS is a SHORT Title", "Meier"),
            TITLE_AUTHOR_BUILDER.buildFromTitleAuthor("This is a short title", "Meier"));
    }

    @Test
    public void titleAuthorNormalizesAccentsAndSharpS() {
        assertEquals(TITLE_AUTHOR_BUILDER.buildFromTitleAuthor("Der Moiré-Effekt", "Meier"),
            TITLE_AUTHOR_BUILDER.buildFromTitleAuthor("Der Moire-Effekt", "Meier"));
        assertEquals(TITLE_AUTHOR_BUILDER.buildFromTitleAuthor("Hier ist Augenmaß gefragt", "Meier"),
            TITLE_AUTHOR_BUILDER.buildFromTitleAuthor("Hier ist Augenmass gefragt", "Meier"));
    }

    @Test
    public void differentTitlesDoNotMatch() {
        assertNotEquals(TITLE_AUTHOR_BUILDER.buildFromTitleAuthor("A different short title", "Meier"),
            TITLE_AUTHOR_BUILDER.buildFromTitleAuthor("This is a short title", "Meier"));
    }

    @Test
    public void providerAggregatesConfiguredBuildersForModsType() {
        Element mods = new Element("mods", MCRConstants.MODS_NAMESPACE);

        mods.addContent(new Element("identifier", MCRConstants.MODS_NAMESPACE)
            .setAttribute("type", "doi").setText("10.1000/xyz"));

        Element titleInfo = new Element("titleInfo", MCRConstants.MODS_NAMESPACE);
        titleInfo.addContent(new Element("title", MCRConstants.MODS_NAMESPACE).setText("This is a Title"));
        mods.addContent(titleInfo);

        Element name = new Element("name", MCRConstants.MODS_NAMESPACE).setAttribute("type", "personal");
        name.addContent(new Element("namePart", MCRConstants.MODS_NAMESPACE)
            .setAttribute("type", "family").setText("Meier"));
        mods.addContent(name);

        Element location = new Element("location", MCRConstants.MODS_NAMESPACE);
        location.addContent(new Element("shelfLocator", MCRConstants.MODS_NAMESPACE).setText("ABC 123"));
        mods.addContent(location);

        MCRObject object = MCRMODSWrapper.wrapMODSDocument(mods, "junit");

        Set<MCRDeDupCriterion> criteria = MCRDeDupCriteriaProvider.obtainInstance().getCriteria(object);

        assertEquals(3, criteria.size());
        assertTrue(criteria.contains(MCRDeDupCriterion.of("identifier", "doi:10.1000/xyz")));
        assertTrue(criteria.contains(MCRDeDupCriterion.of("title-author", "meier: this is a title")));
        assertTrue(criteria.contains(MCRDeDupCriterion.of("shelfmark", "ABC 123")));
    }
}
