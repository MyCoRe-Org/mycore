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

package org.mycore.mets.model;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.mycore.datamodel.niofs.MCRPath;

/**
 * Base implementation for a METS generator.
 *
 * @author Matthias Eichner
 */
public abstract class MCRMETSAbstractGenerator implements MCRMETSGenerator {

    private MCRPath derivatePath;

    private Set<MCRPath> ignorePaths;

    private Mets oldMets;

    public MCRMETSAbstractGenerator() {
        this.ignorePaths = new HashSet<>();
    }

    /**
     * Returns the path to the derivate.
     *
     * @return path to the derivate
     */
    public MCRPath getDerivatePath() {
        return derivatePath;
    }

    /**
     * Returns an optional of the old mets. Sometimes a generator needs to copy informations of the previous state
     * of the mets.xml.
     *
     * @return optional of the previous mets.xml
     */
    public Optional<Mets> getOldMets() {
        return oldMets != null ? Optional.of(this.oldMets) : Optional.empty();
    }

    /**
     * Returns a set of paths which should be ignore while creating the mets.xml.
     *
     * @return set of paths which should be ignored
     */
    public Set<MCRPath> getIgnorePaths() {
        return ignorePaths;
    }

    /**
     * Returns the owner of the derivate path, so the derivate id as string.
     *
     * @return the derivate id
     */
    protected String getOwner() {
        return this.derivatePath.getOwner();
    }

    public void setDerivatePath(MCRPath derivatePath) {
        this.derivatePath = derivatePath;
    }

    public void setIgnorePaths(Set<MCRPath> ignorePaths) {
        this.ignorePaths = ignorePaths;
    }

    public void setOldMets(Mets oldMets) {
        this.oldMets = oldMets;
    }

}
