package org.mycore.importer.mapping.datamodel;

public interface MCRImportDatamodel {

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
     * If the datamodel doesnt support this or the flag is set to 'ignore'
     * this method will return null.
     * 
     * @param metadataName the name of the metadata element
     * @return true if heritbale, false if not and null if not set
     */
    public Boolean isHeritable(String metadataName);
    
    /**
     * Return true if the metadata element is not inherit, otherwise false.
     * If the datamodel doesnt support this or the flag is set to 'ignore'
     * this method will return null.
     * 
     * @param metadataName the name of the metadata element
     * @return false if inherit, true if not and null if not set
     */
    public Boolean isNotinherit(String metadataName);
    
    /**
     * Returns the absolute path to the datamodel document.
     * 
     * @return path to datamodel
     */
    public String getPath();

}