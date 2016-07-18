package org.mycore.mods.identifier;

import org.jdom2.Element;
import org.mycore.common.MCRException;
import org.mycore.datamodel.metadata.MCRBase;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.mods.MCRMODSWrapper;
import org.mycore.pi.MCRPersistentIdentifierInscriber;
import org.mycore.pi.exceptions.MCRPersistentIdentifierException;
import org.mycore.pi.urn.MCRUniformResourceName;


public class MCRMODSURNPersistentIdentifierInscriber extends MCRPersistentIdentifierInscriber<MCRUniformResourceName> {

    public MCRMODSURNPersistentIdentifierInscriber(String inscriberID) {
        super(inscriberID);
    }

    @Override
    public void insertIdentifier(MCRUniformResourceName identifier, MCRBase obj, String additional) throws MCRPersistentIdentifierException {
        MCRObject object = checkObject(obj);
        MCRMODSWrapper wrapper = new MCRMODSWrapper(object);
        wrapper.setElement("identifier", "type", "urn", identifier.asString()).orElseThrow(() -> new MCRException("Could not insert urn into mods document!"));
    }

    @Override
    public void removeIdentifier(MCRUniformResourceName identifier, MCRBase obj, String additional) {

    }

    @Override
    public boolean hasIdentifier(MCRBase obj, String additional) throws MCRPersistentIdentifierException {
        MCRObject object = checkObject(obj);
        MCRMODSWrapper wrapper = new MCRMODSWrapper(object);
        Element element = wrapper.getElement("mods:identifier[@type='urn']");
        return element != null;
    }

    private MCRObject checkObject(MCRBase base) throws MCRPersistentIdentifierException {
        if(!(base instanceof MCRObject)){
            throw new MCRPersistentIdentifierException(getClass().getName() + " does only support MyCoReObjects!");
        }
        return (MCRObject) base;
    }
}
