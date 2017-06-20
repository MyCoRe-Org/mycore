package org.mycore.mets.iiif;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jdom2.Element;
import org.jdom2.Text;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.mycore.common.MCRConstants;
import org.mycore.iiif.presentation.model.attributes.MCRIIIFMetadata;

public class MCRMetsIIIFModsMetadataExtractor implements MCRMetsIIIFMetadataExtractor {

    @Override
    public List<MCRIIIFMetadata> extractModsMetadata(Element xmlData) {
        Map<String, String> elementLabelMap = new HashMap<>();

        elementLabelMap.put("title", "mods:mods/mods:titleInfo/mods:title/text()");
        elementLabelMap.put("genre", "mods:mods/mods:genre/text()");
        // TODO: add some more metadata

        return elementLabelMap.entrySet().stream().map(entry -> {
            XPathExpression<Text> pathExpression = XPathFactory.instance().compile(entry.getValue(), Filters.text(),
                null, MCRConstants.MODS_NAMESPACE);
            List<Text> texts = pathExpression.evaluate(xmlData);
            if (texts.size() == 0) {
                return null;
            }
            return new MCRIIIFMetadata(entry.getKey(),
                texts.stream().map(Text::getText).collect(Collectors.joining(", ")));
        }).filter(e -> e != null)
            .collect(Collectors.toList());
    }

}
