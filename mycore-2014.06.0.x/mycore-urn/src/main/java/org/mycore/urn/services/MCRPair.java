/**
 * 
 */
package org.mycore.urn.services;

import org.mycore.datamodel.ifs.MCRFile;

/**
 * This class represents a pair
 * 
 * @author shermann
 * */
public class MCRPair<S, H> implements Comparable<MCRPair<String, MCRFile>> {
    private S leftComponent;
    private H rightComponent;

    public MCRPair(S leftComponent, H rightComponent) {
        this.leftComponent = leftComponent;
        this.rightComponent = rightComponent;
    }

    /**
     * @return the leftComponent
     */
    public S getLeftComponent() {
        return leftComponent;
    }

    /**
     * @param leftComponent
     *            the leftComponent to set
     */
    public void setLeftComponent(S leftComponent) {
        this.leftComponent = leftComponent;
    }

    /**
     * @return the rightComponent
     */
    public H getRightComponent() {
        return rightComponent;
    }

    /**
     * @param rightComponent
     *            the rightComponent to set
     */
    public void setRightComponent(H rightComponent) {
        this.rightComponent = rightComponent;
    }

    /**
     * Interface method. If one wants to compare other pairs than
     * Pair<String,MCRFile> one should subclass Pair and override this method
     */
    public int compareTo(MCRPair<String, MCRFile> aPair) {
        if (!(this.rightComponent instanceof MCRFile))
            return 0;

        MCRFile f1 = (MCRFile) this.rightComponent;

        if (f1.getAbsolutePath().compareTo(aPair.rightComponent.getAbsolutePath()) < 0) {
            return -1;
        }
        if (f1.getAbsolutePath().compareTo(aPair.rightComponent.getAbsolutePath()) == 0) {
            return 0;
        }
        if (f1.getAbsolutePath().compareTo(aPair.rightComponent.getAbsolutePath()) > 0) {
            return 1;
        }
        return 0;
    }
}