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
