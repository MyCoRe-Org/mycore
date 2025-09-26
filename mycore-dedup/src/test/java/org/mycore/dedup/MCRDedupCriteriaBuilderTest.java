package org.mycore.dedup;


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
import org.junit.Test;
import org.mycore.dedup.MCRDeDupCriteriaBuilder;
import org.mycore.dedup.MCRDeDupCriterion;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;

import java.io.File;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class MCRDedupCriteriaBuilderTest {
    private final Set<String> npath = Set.of(
            "mods:identifier[@type='doi']",
            "mods:title",
            "mods:namePart[@type='family']"
    );
    private MCRDeDupCriteriaBuilder builder = new MCRDeDupCriteriaBuilder(npath);

    @Test
    public void testIdentifiers(){

        MCRDeDupCriterion c1 = builder.buildFromIdentifier("duepublico", "123");
        MCRDeDupCriterion c2 = builder.buildFromIdentifier("duepublico", "123");

        assertEquals(c1, c2);

        MCRDeDupCriterion c3 = builder.buildFromIdentifier("doi", "10.1002/0470841559.ch1");
        assertNotEquals(c1, c3);

        MCRDeDupCriterion c4 = builder.buildFromIdentifier("isbn", "978-1-56619-909-4" );
        MCRDeDupCriterion c5 = builder.buildFromIdentifier("isbn", "9781566199094" );
        assertEquals(c4, c5);
    }

    @Test
    public void testTitles() {
        MCRDeDupCriterion c0 = builder.buildFromTitleAuthor("A different short title","Meier");
        MCRDeDupCriterion c1 = builder.buildFromTitleAuthor("This is a short title","Meier");
        MCRDeDupCriterion c2 = builder.buildFromTitleAuthor("THIS is a SHORT Title","Meier");
        assertNotEquals(c0, c1);
        assertEquals(c1, c2);
       MCRDeDupCriterion c3 = builder.buildFromTitleAuthor("Der Moiré-Effekt im Zusammenhang mit Hühneraugen","Meier");
        MCRDeDupCriterion c4 = builder.buildFromTitleAuthor("Der Moire-Effekt im Zusammenhang mit Huehneraugen","Meier");
        assertEquals(c3, c4);

        MCRDeDupCriterion c5 = builder.buildFromTitleAuthor("Horizon-Report 2015","Meier");
        MCRDeDupCriterion c6 = builder.buildFromTitleAuthor("Horizon-Report 2016","Meier");
        assertNotEquals(c5, c6);

        MCRDeDupCriterion c7 = builder.buildFromTitleAuthor("Hier ist Augenmaß gefragt","Meier");
        MCRDeDupCriterion c8 = builder.buildFromTitleAuthor("Hier ist Augenmass gefragt","Meier");
        assertEquals(c7, c8);
    }

}
