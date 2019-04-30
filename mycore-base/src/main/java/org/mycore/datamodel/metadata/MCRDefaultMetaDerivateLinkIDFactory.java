package org.mycore.datamodel.metadata;

import java.util.ArrayList;

import org.jdom2.Content;
import org.jdom2.Element;
import org.mycore.common.MCRConstants;

public class MCRDefaultMetaDerivateLinkIDFactory extends MCRMetaDerivateLinkIDFactory{

    @Override
    public MCRMetaDerivateLinkID getDerivateLink(MCRDerivate der) {

        final MCRMetaDerivateLinkID derivateLinkID = new MCRMetaDerivateLinkID();
        final String mainDoc = der.getDerivate().getInternals().getMainDoc();
        final String label = der.getLabel();

        derivateLinkID.setReference(der.getId().toString(), null, label);
        derivateLinkID.setSubTag("derobject");
        final ArrayList<Content> contentList = new ArrayList<>();

        final int order = der.getOrder();
        final Element orderElement = new Element("order");
        orderElement.setText(String.valueOf(order));
        contentList.add(orderElement);

        if (mainDoc != null) {
            final Element mainDocElement = new Element("mainDoc");
            mainDocElement.setText(mainDoc);
            contentList.add(mainDocElement);
        }

        der.getDerivate().getTitles().forEach(title -> {
            final Element titleElement = new Element("title");
            titleElement.setAttribute("lang", title.getLang(), MCRConstants.XML_NAMESPACE);
            titleElement.setText(title.getText());
            contentList.add(titleElement);
        });

        der.getDerivate().getClassifications().forEach(clazz -> {
            final Element classElement = new Element("title");
            classElement.setAttribute("classid", clazz.getClassId());
            classElement.setAttribute("categid", clazz.getCategId());
            contentList.add(classElement);
        });

        derivateLinkID.setContentList(contentList);
        return derivateLinkID;
    }
}
