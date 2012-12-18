package org.mycore.importer.mapping.datamodel;

import java.util.List;

public interface MCRImportDatamodel {
    
    public enum Inheritance {
        TRUE, FALSE, IGNORE;

        public boolean getBoolean() {
            return this.equals(Inheritance.TRUE);
        }
    }

    /**
     * <p>
     * Returns the enclosing name of the given child name.
     * </p>
     * <p>
     * Sample:</br>
     *  &lt;element name="<b>def.unittitle</b>"&gt;</br>
     *  &nbsp;&nbsp;&lt;mcrmetalangtext name="<b>unittitle</b>" class="MCRMetaLangText" /&gt;</br>
     *  &lt;/element&gt;</br>
     * </p> 
     * <p>
     * In this case 'unittitle' is the given metadata name and 'def.unititle' the returned
     * enclosing name.
     * </p>
     * @param innerTag
     * @return
     */
    public String getEnclosingName(String metadataName);

    /**
     * Returns the classname from the metadata element. This is something like
     * MCRMetaLangText or MCRMetaLinkID.
     * 
     * @param metadataName the name of the metadata element
     * @return the classname of the metadata element
     */
    public String getClassname(String metadataName);
    
    /**
     * Return true if the metadata element is heritable, otherwise false.
     * If the data model doesn't support this or the flag is set to 'ignore'
     * this method will return <code>Inheritance.IGNORE</code> is returned.
     * 
     * @param metadataName the name of the metadata element
     * @return true if heritable, false if not and null if not set
     */
    public Inheritance isHeritable(String metadataName);

    /**
     * Return true if the meta data element is not inherit, otherwise false.
     * If the data model doesn't support this or the flag is set to 'ignore'
     * this method will return <code>Inheritance.IGNORE</code> is returned.
     * 
     * @param metadataName the name of the metadata element
     * @return false if inherit, true if not and null if not set
     */
    public Inheritance isNotinherit(String metadataName);

    /**
     * Returns the absolute path to the data model document.
     * 
     * @return path to data model
     */
    public String getPath();

    /**
     * Checks if the minOccurs value of a meta data element is > 0.
     * If so, true is returned, otherwise false.
     * 
     * @param metdataName name of the meta data element
     * @return true if meta data element is required (minOccurs > 0)
     */
    public boolean isRequired(String metdataName);

    /**
     * Returns a list of all meta data elements which are defined
     * in this datamodel.
     * 
     * @return list of meta data elements
     */
    public List<String> getMetadataNames();

}