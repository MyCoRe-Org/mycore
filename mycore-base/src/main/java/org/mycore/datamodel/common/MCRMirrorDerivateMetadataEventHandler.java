package org.mycore.datamodel.common;

import java.util.ArrayList;

import org.apache.xerces.util.XMLChar;
import org.jdom2.Content;
import org.jdom2.Element;
import org.mycore.access.MCRAccessException;
import org.mycore.common.MCRConstants;
import org.mycore.common.MCRException;
import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventHandlerBase;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetaDerivateLinkID;
import org.mycore.datamodel.metadata.MCRMetaLangText;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;

/**
 * Mirrors derivate metadata to the mycore object.
 */
public class MCRMirrorDerivateMetadataEventHandler extends MCREventHandlerBase {

    @Override
    protected void handleDerivateCreated(MCREvent evt, MCRDerivate der) {
        updateObject(der);
    }

    @Override
    protected void handleDerivateUpdated(MCREvent evt, MCRDerivate der) {
        updateObject(der);
    }

    @Override
    protected void handleDerivateRepaired(MCREvent evt, MCRDerivate der) {
        updateObject(der);
    }

    private void updateObject(MCRDerivate der) {
        final MCRObjectID ownerID = der.getOwnerID();
        final MCRObject mcrObject = MCRMetadataManager.retrieveMCRObject(ownerID);
        final MCRMetaDerivateLinkID derivateLink = mcrObject.getStructure().getDerivateLink(der.getId());

        if (derivateLink != null) {
            final String mainDoc = der.getDerivate().getInternals().getMainDoc();
            final String label = der.getLabel();

            derivateLink.setMainDoc(mainDoc);

            if (XMLChar.isValidNCName(label)) {
                derivateLink.setXLinkLabel(label);
            }

            final int titleSize = der.getDerivate().getTitleSize();
            final ArrayList<Content> contentList = new ArrayList<>();
            for (int i = 0; i < titleSize; i++) {
                final MCRMetaLangText title = der.getDerivate().getTitle(i);
                final String lang = title.getLang();
                final String text = title.getText();
                final Element titleElement = new Element("title");
                titleElement.setAttribute(lang, lang, MCRConstants.XML_NAMESPACE);
                titleElement.setText(text);
                contentList.add(titleElement);
            }
            derivateLink.setContentList(contentList);
            try {
                MCRMetadataManager.update(mcrObject);
            } catch (MCRAccessException e) {
                throw new MCRException("Eventhandler has no access to modify the MyCoRe-Object!", e);
            }
        }
    }
}
