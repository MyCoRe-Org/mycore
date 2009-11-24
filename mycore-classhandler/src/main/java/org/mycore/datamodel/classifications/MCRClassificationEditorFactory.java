package org.mycore.datamodel.classifications;

public class MCRClassificationEditorFactory {
    private static MCRClassificationEditor classificationEditor = new MCRClassificationEditor();
    public static MCRClassificationEditor getInstance() {
        return classificationEditor;
    }

}
