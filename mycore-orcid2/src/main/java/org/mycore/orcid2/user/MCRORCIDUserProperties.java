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

package org.mycore.orcid2.user;

/**
 * Model form MCRORCIDUser properties.
 */
public class MCRORCIDUserProperties {

    private boolean alwaysUpdate;

    private boolean createOwnDuplicate;

    private boolean createOwn;

    /**
     * Constructs new MCRORCIDUserProperties.
     */
    MCRORCIDUserProperties() {
        this(false, false, false);
    }

    /**
     * Constructs new MCRORCIDUserProperties.
     * 
     * @param alwaysUpdate if always update
     * @param createOwnDuplicate if create own duplicate
     * @param createOwn if create own
     */
    MCRORCIDUserProperties(boolean alwaysUpdate, boolean createOwnDuplicate, boolean createOwn) {
        this.alwaysUpdate = alwaysUpdate;
        this.createOwnDuplicate = createOwnDuplicate;
        this.createOwn = createOwn;
    }

    /**
     * Returns if always update.
     * 
     * @return true if always update
     */
    public boolean isAlwaysUpdate() {
        return alwaysUpdate;
    }

    /**
     * Sets always update.
     * 
     * @param alwaysUpdate is always update
     */
    public void setAlwaysUpdate(boolean alwaysUpdate) {
        this.alwaysUpdate = alwaysUpdate;
    }

    /**
     * Returns if create own duplicate.
     * 
     * @return true if create own duplicate
     */
    public boolean isCreateOwnDuplicate() {
        return createOwnDuplicate;
    }

    /**
     * Sets is create own duplicate.
     * 
     * @param createOwnDuplicate is create own duplicate
     */
    public void setCreateOwnDuplicate(boolean createOwnDuplicate) {
        this.createOwnDuplicate = createOwnDuplicate;
    }

    /**
     * Returns if create own.
     * 
     * @return true if create own
     */
    public boolean isCreateOwn() {
        return createOwn;
    }

    /**
     * Sets is create own.
     * 
     * @param createOwn is create own
     */
    public void setCreateOwn(boolean createOwn) {
        this.createOwn = createOwn;
    }
}
