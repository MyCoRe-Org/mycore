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

package org.mycore.dedup.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

public class MCRDuplicateDtoTest {

    private static final List<MCRDuplicateDto.CriterionDto> CRITERIA = List
        .of(new MCRDuplicateDto.CriterionDto("identifier", "doi:10.1000/xyz"));

    @Test
    public void orderedObjectIdsAreKept() {
        MCRDuplicateDto dto = new MCRDuplicateDto("mcr_test_00000001", "first", "mcr_test_00000002", "second",
            CRITERIA);

        assertEquals("mcr_test_00000001", dto.objectId1());
        assertEquals("first", dto.title1());
        assertEquals("mcr_test_00000002", dto.objectId2());
        assertEquals("second", dto.title2());
    }

    @Test
    public void reversedObjectIdsAreSwappedTogetherWithTheirTitles() {
        MCRDuplicateDto dto = new MCRDuplicateDto("mcr_test_00000002", "second", "mcr_test_00000001", "first",
            CRITERIA);

        assertEquals("mcr_test_00000001", dto.objectId1());
        assertEquals("first", dto.title1());
        assertEquals("mcr_test_00000002", dto.objectId2());
        assertEquals("second", dto.title2());
    }

    @Test
    public void criteriaAreCopiedAndUnmodifiable() {
        List<MCRDuplicateDto.CriterionDto> criteria = new ArrayList<>(CRITERIA);
        MCRDuplicateDto dto = new MCRDuplicateDto("mcr_test_00000001", "first", "mcr_test_00000002", "second",
            criteria);

        criteria.add(new MCRDuplicateDto.CriterionDto("title-author", "meier: a title"));

        assertEquals(CRITERIA, dto.criteria());
        assertThrows(UnsupportedOperationException.class,
            () -> dto.criteria().add(new MCRDuplicateDto.CriterionDto("title-author", "meier: a title")));
    }
}
