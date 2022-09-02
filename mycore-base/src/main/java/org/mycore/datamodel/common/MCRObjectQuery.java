/*
 *  This file is part of ***  M y C o R e  ***
 *  See http://www.mycore.de/ for details.
 *
 *  MyCoRe is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  MyCoRe is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mycore.datamodel.common;

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

    public MCRObjectID afterId() {
        return afterId;
    }

    public MCRObjectQuery afterId(MCRObjectID lastId) {
        this.afterId = lastId;
        return this;
    }

    public int offset() {
        return offset;
    }

    public MCRObjectQuery offset(int offset) {
        this.offset = offset;
        return this;
    }

    public int limit() {
        return limit;
    }

    public MCRObjectQuery limit(int limit) {
        this.limit = limit;
        return this;
    }

    public String type() {
        return type;
    }

    public MCRObjectQuery type(String type) {
        this.type = type;
        return this;
    }

    public String project() {
        return project;
    }

    public MCRObjectQuery project(String project) {
        this.project = project;
        return this;
    }

    public String status() {
        return status;
    }

    public MCRObjectQuery status(String status) {
        this.status = status;
        return this;
    }

    public MCRObjectQuery sort(SortBy sortBy, SortOrder sortOrder) {
        this.sortOrder = sortOrder;
        this.sortBy = sortBy;
        return this;
    }

    public SortBy sortBy() {
        return sortBy;
    }

    public SortOrder sortAsc() {
        return sortOrder;
    }

    public Instant modifiedBefore() {
        return modifiedBefore;
    }

    public MCRObjectQuery modifiedBefore(Instant modifiedBefore) {
        this.modifiedBefore = modifiedBefore;
        return this;
    }

    public Instant modifiedAfter() {
        return modifiedAfter;
    }

    public MCRObjectQuery modifiedAfter(Instant modifiedAfter) {
        this.modifiedAfter = modifiedAfter;
        return this;
    }

    public Instant createdBefore() {
        return createdBefore;
    }

    public MCRObjectQuery createdBefore(Instant createdBefore) {
        this.createdBefore = createdBefore;
        return this;
    }

    public Instant createdAfter() {
        return createdAfter;
    }

    public MCRObjectQuery createdAfter(Instant createdAfter) {
        this.createdAfter = createdAfter;
        return this;
    }

    public Instant deletedBefore() {
        return deletedBefore;
    }

    public MCRObjectQuery deletedBefore(Instant deletedBefore) {
        this.deletedBefore = deletedBefore;
        return this;
    }

    public Instant deletedAfter() {
        return deletedAfter;
    }

    public MCRObjectQuery deletedAfter(Instant deletedAfter) {
        this.deletedAfter = deletedAfter;
        return this;
    }

    public String createdBy() {
        return createdBy;
    }

    public MCRObjectQuery createdBy(String createdBy) {
        this.createdBy = createdBy;
        return this;
    }

    public String modifiedBy() {
        return modifiedBy;
    }

    public MCRObjectQuery modifiedBy(String modifiedBy) {
        this.modifiedBy = modifiedBy;
        return this;
    }

    public String deletedBy() {
        return deletedBy;
    }

    public MCRObjectQuery deletedBy(String deletedBy) {
        this.deletedBy = deletedBy;
        return this;
    }

    public int numberGreater() {
        return numberGreater;
    }

    public MCRObjectQuery numberGreater(int numberGreater) {
        this.numberGreater = numberGreater;
        return this;
    }

    public int numberLess() {
        return numberLess;
    }

    public MCRObjectQuery numberLess(int numberLess) {
        this.numberLess = numberLess;
        return this;
    }

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
}
