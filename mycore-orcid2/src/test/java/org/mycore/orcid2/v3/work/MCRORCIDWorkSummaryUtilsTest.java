/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
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

package org.mycore.orcid2.v3.work;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.mycore.orcid2.util.MCRIdentifier;
import org.orcid.jaxb.model.common.Relationship;
import org.orcid.jaxb.model.v3.release.record.ExternalID;
import org.orcid.jaxb.model.v3.release.record.ExternalIDs;
import org.orcid.jaxb.model.v3.release.record.summary.WorkSummary;

public class MCRORCIDWorkSummaryUtilsTest {

    private static final Set<MCRIdentifier> IDENTIFIERS = Set.of(new MCRIdentifier("doi", "10.1000/ABC"),
        new MCRIdentifier("isbn", "978-3-16-148410-0"));

    @Test
    public void testFindMatchingSummariesBySelfIdentifier() {
        final WorkSummary summary = createWorkSummary(1L, createExternalID("isbn", "978-3-16-148410-0",
            Relationship.SELF));
        assertEquals(List.of(summary), findMatchingSummaries(List.of(summary)));
    }

    @Test
    public void testFindMatchingSummariesIgnoresPartOfIdentifier() {
        final WorkSummary summary = createWorkSummary(1L, createExternalID("isbn", "978-3-16-148410-0",
            Relationship.PART_OF));
        assertEquals(List.of(), findMatchingSummaries(List.of(summary)));
    }

    @Test
    public void testFindMatchingSummariesIgnoresCase() {
        final WorkSummary summary = createWorkSummary(1L, createExternalID("DOI", " 10.1000/abc ",
            Relationship.SELF));
        assertEquals(List.of(summary), findMatchingSummaries(List.of(summary)));
    }

    @Test
    public void testFindMatchingSummariesWithoutMatch() {
        final WorkSummary other = createWorkSummary(1L, createExternalID("doi", "10.1000/xyz", Relationship.SELF));
        final WorkSummary empty = createWorkSummary(2L);
        empty.setExternalIdentifiers(null);
        assertEquals(List.of(), findMatchingSummaries(List.of(other, empty)));
    }

    @Test
    public void testFindMatchingSummariesWithoutIdentifiers() {
        final WorkSummary summary = createWorkSummary(1L, createExternalID("doi", "10.1000/abc",
            Relationship.SELF));
        assertEquals(List.of(),
            MCRORCIDWorkSummaryUtils.findMatchingSummariesByIdentifiers(Set.of(), List.of(summary)).toList());
    }

    private static List<WorkSummary> findMatchingSummaries(List<WorkSummary> summaries) {
        return MCRORCIDWorkSummaryUtils.findMatchingSummariesByIdentifiers(IDENTIFIERS, summaries).toList();
    }

    private static WorkSummary createWorkSummary(long putCode, ExternalID... externalIDs) {
        final ExternalIDs ids = new ExternalIDs();
        ids.getExternalIdentifier().addAll(List.of(externalIDs));
        final WorkSummary summary = new WorkSummary();
        summary.setPutCode(putCode);
        summary.setExternalIdentifiers(ids);
        return summary;
    }

    private static ExternalID createExternalID(String type, String value, Relationship relationship) {
        final ExternalID id = new ExternalID();
        id.setType(type);
        id.setValue(value);
        id.setRelationship(relationship);
        return id;
    }
}
