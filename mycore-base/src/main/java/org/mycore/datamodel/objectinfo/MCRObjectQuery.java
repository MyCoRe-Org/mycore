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

package org.mycore.datamodel.objectinfo;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.mycore.datamodel.metadata.MCRObjectID;

/**
 * Used to Query a collection of mycore objects.
 */
public class MCRObjectQuery {

    private MCRObjectID afterId = null;

    private int offset = -1;

    private int limit = -1;

    private int numberGreater = -1;

    private int numberLess = -1;

    private String type = null;

    private String project = null;

    private String status = null;

    private SortBy sortBy;

    private SortOrder sortOrder;

    private Instant modifiedBefore;

    private Instant modifiedAfter;

    private Instant createdBefore;

    private Instant createdAfter;

    private Instant deletedBefore;

    private Instant deletedAfter;

    private String createdBy;

    private String modifiedBy;

    private String deletedBy;

    private final List<String> includeCategories = new ArrayList<>();

    /**
     * @return the id after which the listing starts
     */
    public MCRObjectID afterId() {
        return afterId;
    }

    /**
     * modifies this query to only return object ids after lastId
     * should not be used together with the {@link #sort(SortBy, SortOrder)} method
     * @return this
     */
    public MCRObjectQuery afterId(MCRObjectID lastId) {
        this.afterId = lastId;
        return this;
    }

    /**
     * @return the amount of objects to skip from the start of the results
     */
    public int offset() {
        return offset;
    }

    /**
     * modifies this query to skip the amount offset objects at the start of the results
     * @param offset amount
     * @return this
     */
    public MCRObjectQuery offset(int offset) {
        this.offset = offset;
        return this;
    }

    /**
     * @return the maximum amount of results
     */
    public int limit() {
        return limit;
    }

    /**
     * modifies this query to limit the amount of results
     * @param limit amount
     * @return this
     */
    public MCRObjectQuery limit(int limit) {
        this.limit = limit;
        return this;
    }

    /**
     * @return the type restriction
     */
    public String type() {
        return type;
    }

    /**
     * modifies this query to restrict the type of the objects
     * @param type restriction
     * @return this
     */
    public MCRObjectQuery type(String type) {
        this.type = type;
        return this;
    }

    /**
     * @return the project restriction
     */
    public String project() {
        return project;
    }

    /**
     * modifies this query to restrict the project of the objects
     * @param project restriction
     * @return this
     */
    public MCRObjectQuery project(String project) {
        this.project = project;
        return this;
    }

    /**
     * @return the status restriction
     */
    public String status() {
        return status;
    }

    /**
     * modifies this query to restrict the status of the objects. e.G. state:submitted
     * @param status restriction
     * @return this
     */
    public MCRObjectQuery status(String status) {
        this.status = status;
        return this;
    }

    /**
     * modifies this query to change the order of the resulting list
     * @param sortBy the sort field
     * @param sortOrder the sort direction
     * @return this
     */
    public MCRObjectQuery sort(SortBy sortBy, SortOrder sortOrder) {
        this.sortOrder = sortOrder;
        this.sortBy = sortBy;
        return this;
    }

    /**
     * @return the order field of the resulting list
     */
    public SortBy sortBy() {
        return sortBy;
    }

    /**
     * @return the sort direction of the result list
     */
    public SortOrder sortAsc() {
        return sortOrder;
    }

    /**
     * @return the upper bound limit of the modified date
     */
    public Instant modifiedBefore() {
        return modifiedBefore;
    }

    /**
     * modifies this query to limit the upper bound of the modified date
     * @param modifiedBefore the upper bound limit
     * @return the same query
     */
    public MCRObjectQuery modifiedBefore(Instant modifiedBefore) {
        this.modifiedBefore = modifiedBefore;
        return this;
    }

    /**
     * @return the lower bound limit of the modifed date
     */
    public Instant modifiedAfter() {
        return modifiedAfter;
    }

    /**
     * modifies this query to limit the lower bound of the modified date
     * @param modifiedAfter the lower bound limit
     * @return this
     */
    public MCRObjectQuery modifiedAfter(Instant modifiedAfter) {
        this.modifiedAfter = modifiedAfter;
        return this;
    }

    /**
     * @return the upper bound limit of the created date
     */
    public Instant createdBefore() {
        return createdBefore;
    }

    /**
     * modifies this query to limit the upper bound of the created date
     * @param createdBefore the upper bound limit
     * @return this
     */
    public MCRObjectQuery createdBefore(Instant createdBefore) {
        this.createdBefore = createdBefore;
        return this;
    }

    /**
     * @return the lower bound limit of the created date
     */
    public Instant createdAfter() {
        return createdAfter;
    }

    /**
     * modifies this query to limit the lower bound of the created date
     * @param createdAfter the lower bound limit
     * @return this
     */
    public MCRObjectQuery createdAfter(Instant createdAfter) {
        this.createdAfter = createdAfter;
        return this;
    }

    /**
     * @return the upper bound limit of the deleted date
     */
    public Instant deletedBefore() {
        return deletedBefore;
    }

    /**
     * modifies this query to limit the upper bound of the deleted date
     * @param deletedBefore the upper bound limit
     * @return this
     */
    public MCRObjectQuery deletedBefore(Instant deletedBefore) {
        this.deletedBefore = deletedBefore;
        return this;
    }

    /**
     * @return the lower bound limit of the deleted date
     */
    public Instant deletedAfter() {
        return deletedAfter;
    }

    /**
     * modifies this query to limit the lower bound of the deleted date
     * @param deletedAfter the lower bound limit
     * @return this
     */
    public MCRObjectQuery deletedAfter(Instant deletedAfter) {
        this.deletedAfter = deletedAfter;
        return this;
    }

    /**
     * @return the created by user restriction
     */
    public String createdBy() {
        return createdBy;
    }

    /**
     * modifies this query to restrict the user which created the objects
     * @param createdBy the user restriction
     * @return this
     */
    public MCRObjectQuery createdBy(String createdBy) {
        this.createdBy = createdBy;
        return this;
    }

    /**
     * @return the modified by user restriction
     */
    public String modifiedBy() {
        return modifiedBy;
    }

    /**
     * modifies this query to restrict the user which last modified the objects
     * @param modifiedBy the user restriction
     * @return this
     */
    public MCRObjectQuery modifiedBy(String modifiedBy) {
        this.modifiedBy = modifiedBy;
        return this;
    }

    /**
     * @return the deleted by user restriction
     */
    public String deletedBy() {
        return deletedBy;
    }

    /**
     * modifies this query to restrict the user which deleted the objects
     * @param deletedBy the user restriction
     * @return this
     */
    public MCRObjectQuery deletedBy(String deletedBy) {
        this.deletedBy = deletedBy;
        return this;
    }

    /**
     * @return the lower bound limit to the object id number
     */
    public int numberGreater() {
        return numberGreater;
    }

    /**
     * modifies this query to limit the lower bound of the object id number
     * @param numberGreater the lower bound limit
     * @return this
     */
    public MCRObjectQuery numberGreater(int numberGreater) {
        this.numberGreater = numberGreater;
        return this;
    }

    /**
     * @return the upper bound limit of the object id number
     */
    public int numberLess() {
        return numberLess;
    }

    /**
     * modifies this query to limit the upper bound of the object id number
     * @param numberLess the upper bound limit
     * @return this
     */
    public MCRObjectQuery numberLess(int numberLess) {
        this.numberLess = numberLess;
        return this;
    }

    /**
     * @return a modifiable list of classification category ids. A result object need to be linked to all the
     * categories.
     */
    public List<String> getIncludeCategories() {
        return includeCategories;
    }

    public enum SortBy {
        id,
        modified,
        created
    }

    public enum SortOrder {
        asc,
        desc
    }

    @Override
    public String toString() {
        return "MCRObjectQuery{" +
            "afterId=" + afterId +
            ", offset=" + offset +
            ", limit=" + limit +
            ", numberGreater=" + numberGreater +
            ", numberLess=" + numberLess +
            ", type='" + type + '\'' +
            ", project='" + project + '\'' +
            ", status='" + status + '\'' +
            ", sortBy=" + sortBy +
            ", sortOrder=" + sortOrder +
            ", modifiedBefore=" + modifiedBefore +
            ", modifiedAfter=" + modifiedAfter +
            ", createdBefore=" + createdBefore +
            ", createdAfter=" + createdAfter +
            ", deletedBefore=" + deletedBefore +
            ", deletedAfter=" + deletedAfter +
            ", createdBy='" + createdBy + '\'' +
            ", modifiedBy='" + modifiedBy + '\'' +
            ", deletedBy='" + deletedBy + '\'' +
            ", includeCategories=" + includeCategories +
            '}';
    }
}
