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

package org.mycore.orcid2.user;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents the properties associated with an ORCID user, controlling various settings
 * related to works management.
 */
public class MCRORCIDUserProperties {

    private Boolean alwaysUpdateWork;

    private Boolean createDuplicateWork;

    private Boolean createFirstWork;

    private Boolean recreateDeletedWork;

    /**
     * Constructs a new instance of {@code MCRORCIDUserProperties} with default values.
     */
    public MCRORCIDUserProperties() {
    }

    /**
     * Constructs a new instance of {@code MCRORCIDUserProperties} with specified values.
     *
     * @param alwaysUpdateWork if always update
     * @param createDuplicateWork if create own duplicate
     * @param createFirstWork if create own
     * @param recreateDeletedWork if recreate deleted
     */
    public MCRORCIDUserProperties(Boolean alwaysUpdateWork, Boolean createDuplicateWork, Boolean createFirstWork,
        Boolean recreateDeletedWork) {
        this.alwaysUpdateWork = alwaysUpdateWork;
        this.createDuplicateWork = createDuplicateWork;
        this.createFirstWork = createFirstWork;
        this.recreateDeletedWork = recreateDeletedWork;
    }

    /**
     * Returns always update work property.
     *
     * @return true if always update
     */
    @JsonProperty("alwaysUpdateWork")
    public Boolean isAlwaysUpdateWork() {
        return alwaysUpdateWork;
    }

    /**
     * Sets always update work property.
     *
     * @param alwaysUpdateWork is always update work
     */
    public void setAlwaysUpdateWork(Boolean alwaysUpdateWork) {
        this.alwaysUpdateWork = alwaysUpdateWork;
    }

    /**
     * Returns create duplicate work property.
     *
     * @return true if create duplicate work
     */
    @JsonProperty("createDuplicateWork")
    public Boolean isCreateDuplicateWork() {
        return createDuplicateWork;
    }

    /**
     * Sets create duplicate work property.
     *
     * @param createDuplicateWork is create duplicate work
     */
    public void setCreateDuplicateWork(Boolean createDuplicateWork) {
        this.createDuplicateWork = createDuplicateWork;
    }

    /**
     * Returns create first work property.
     *
     * @return true if create first work
     */
    @JsonProperty("createFirstWork")
    public Boolean isCreateFirstWork() {
        return createFirstWork;
    }

    /**
     * Sets create first work property.
     *
     * @param createFirstWork is create first work
     */
    public void setCreateFirstWork(Boolean createFirstWork) {
        this.createFirstWork = createFirstWork;
    }

    /**
     * Returns recreate deleted work property.
     *
     * @return true if recreate deleted work
     */
    @JsonProperty("recreateDeletedWork")
    public Boolean isRecreateDeletedWork() {
        return recreateDeletedWork;
    }

    /**
     * Sets recreate deleted work property.
     *
     * @param recreateDeletedWork recreate deleted work
     */
    public void setRecreateDeleted(Boolean recreateDeletedWork) {
        this.recreateDeletedWork = recreateDeletedWork;
    }
}
