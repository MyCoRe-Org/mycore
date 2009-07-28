package org.mycore.importer.mapping.resolver.metadata;

import org.jdom.Namespace;


public class MCRImportTextResolver extends MCRImportAbstractMetadataResolver {

    @Override
    protected void resolveAdditional() {
        setDefaultAttributes();
    }

    protected void setDefaultAttributes() {
        // default lang is de
        String lang = metadataChild.getAttributeValue("lang", Namespace.XML_NAMESPACE); 
        if(lang == null || lang.equals(""))
            metadataChild.setAttribute("lang", "de", Namespace.XML_NAMESPACE);

        // default form is "plain"
        String form = metadataChild.getAttributeValue("form"); 
        if(form == null || form.equals(""))
            metadataChild.setAttribute("form", "plain");       
    }

    protected boolean checkValidation() {
        if(metadataChild.getText() == null || metadataChild.getText().equals(""))
            return false;
        return true;
    }
}