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

package org.mycore.orcid2.v3;

import org.mycore.orcid2.client.MCRORCIDSection;

/**
 * @see org.mycore.orcid2.client.MCRORCIDSection
 */
public enum MCRORCIDSectionImpl implements MCRORCIDSection {
    ALL(""), ACTIVITIES("activities"), ADDRESS("address"), BIOGRAPHY("biography"), DISTINCTION("distinction"),
    DISTINCTION_SUMMARY("distinction/summaray"), DISTINCTIONS("distinctions"), EDUCATION("education"),
    EDUCATION_SUMMARY("education/summary"), EDUCATIONS("educations"), EMAIL("email"), EMPLOYMENT("employment"),
    EMPLOYMENT_SUMMARY("employment/summary"), EMPLOYMENTS("employments"), EXTERNAL_IDENTIFIERS("external-identifiers"),
    FUNDING("funding"), FUNDING_SUMMARY("funding/summary"), FUNDINGS("fundings"), INVITED_POSITION("invited-position"),
    INVITED_POSITION_SUMMARY("invited-position/summary"), INVITED_POSITIONS("invited-positions"), KEYWORDS("keywords"),
    MEMBERSHIP("membership"), MEMBERSHIP_SUMMARY("membership/summary"), MEMBERSHIPS("memberships"),
    NOTIFICATION_PERMISSION("notification-permission"), OTHER_NAMES("other-names"), PEER_REVIEW("peer-review"),
    PEER_REVIEW_SUMMARY("peer-review/summary"), PEER_REVIEWS("peer-reviews"), PERSON("person"),
    PERSON_DETAILS("person-details"), QUALIFICATION("qualification"), QUALIFICATION_SUMMARY("qualification/summary"),
    QUALIFICATIONS("qualifications"), RESEARCH_RESOURCE("research-resource"),
    RESEARCH_RESOURCE_SUMMARY("research-resource/summary"), RESEARCH_RESOURCES("research_resources"),
    RESEARCHER_URLS("researcher-urls"), SERVICE("service"), SERVICE_SUMMARY("service/summary"), SERVICES("services"),
    WORK("work"), WORK_SUMMARY("work/summary"), WORKS("works");

    private final String path;

    MCRORCIDSectionImpl(String path) {
        this.path = path;
    }

    @Override
    public String getPath() {
        return path;
    }
}
