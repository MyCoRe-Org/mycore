package org.mycore.common;

import org.mycore.common.content.MCRJDOMContent;
import org.mycore.common.xml.MCRXMLParserFactory;
import org.mycore.datamodel.metadata.MCRMetaElement;
import org.mycore.datamodel.metadata.MCRMetaInterface;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectMetadata;

/**
 * Helper class to merge mycore objects. Only metadata merging is
 * currently supported.
 *
 * @author Matthias Eichner
 */
public class MCRObjectMerger {

    protected MCRObject target;

    /**
     * Creates a new instance of the object merger. The target will be cloned for
     * further processing. You will receive a copy when calling {@link #get()}.
     *
     * @param target the target mycore object
     */
    public MCRObjectMerger(MCRObject target) {
        this.target = new MCRObject(target.createXML());
    }

    /**
     * Merges the metadata of the given source into the target object. Be aware that
     * performance isn't that good when validation is activated, due checking against
     * the schema each time a change is made.
     *
     * @param source the source which is merged into the target
     * @param validate If true, every change is tracked and validated against the
     *          xml schema of the mycore object. When a change is invalid it will be
     *          canceled and the merging continues.
     *          When set to false the mycore object will be merged without validation.
     *          This can result in an invalid object.
     *
     * @return true if something was merged
     */
    public boolean mergeMetadata(MCRObject source, boolean validate) {
        MCRObjectMetadata targetMetadata = this.target.getMetadata();
        boolean merged = false;
        for (MCRMetaElement metaElementSource : source.getMetadata()) {
            MCRMetaElement metaElementTarget = targetMetadata.getMetadataElement(metaElementSource.getTag());
            if (metaElementTarget == null) {
                targetMetadata.setMetadataElement(metaElementSource.clone());
                if (validate && !validate(this.target)) {
                    targetMetadata.removeMetadataElement(metaElementSource.getTag());
                } else {
                    merged = true;
                }
            } else {
                for (MCRMetaInterface metaInterfaceSource : metaElementSource) {
                    boolean equal = false;
                    for (MCRMetaInterface metaInterfaceTarget : metaElementTarget) {
                        if (metaInterfaceSource.equals(metaInterfaceTarget)) {
                            equal = true;
                            break;
                        }
                    }
                    if (!equal) {
                        metaElementTarget.addMetaObject(metaInterfaceSource.clone());
                        if (validate && !validate(this.target)) {
                            metaElementTarget.removeMetaObject(metaInterfaceSource);
                        } else {
                            merged = true;
                        }
                    }
                }
            }
        }
        return merged;
    }

    /**
     * Validates the given mcr object against its own schema.
     *
     * @param mcrobj the object to validate
     * @return true if the object is valid, otherwise false
     */
    protected boolean validate(MCRObject mcrobj) {
        try {
            MCRXMLParserFactory.getParser(true, true).parseXML(new MCRJDOMContent(mcrobj.createXML()));
            return true;
        } catch (Exception exc) {
            return false;
        }
    }

    /**
     * Returns a copy of the merged target object.
     *
     */
    public MCRObject get() {
        return this.target;
    }

}
