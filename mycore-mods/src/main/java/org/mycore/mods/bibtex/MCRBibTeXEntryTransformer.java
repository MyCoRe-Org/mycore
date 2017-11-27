/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mycore.mods.bibtex;

import java.util.ArrayList;
import java.util.List;

import org.jdom2.Element;
import org.mycore.common.MCRConstants;

import bibtex.dom.BibtexEntry;

/**
 * Transforms a single BibTeX entry to a JDOM mods:mods element.
 *
 * @author Frank L\u00FCtzenkirchen
 */
class MCRBibTeXEntryTransformer {

    private static final String XPATH_HOST = "mods:relatedItem[@type='host']";

    private List<MCRFieldTransformer> fieldTransformers = new ArrayList<>();

    MCRBibTeXEntryTransformer() {
        fieldTransformers
            .add(new MCRField2XPathTransformer("document_type", "mods:genre" + MCRFieldTransformer.AS_NEW_ELEMENT));
        fieldTransformers.add(new MCRField2XPathTransformer("title",
            "mods:titleInfo" + MCRFieldTransformer.AS_NEW_ELEMENT + "/mods:title"));
        fieldTransformers.add(new MCRPersonListTransformer("author", "aut"));
        fieldTransformers.add(new MCRField2XPathTransformer("journal",
            XPATH_HOST + "[mods:genre='journal']/mods:titleInfo/mods:title"));
        fieldTransformers.add(new MCRField2XPathTransformer("booktitle",
            XPATH_HOST + "[mods:genre='collection']/mods:titleInfo/mods:title"));
        fieldTransformers
            .add(new MCRMoveToRelatedItemIfExists(XPATH_HOST, new MCRPersonListTransformer("editor", "edt")));
        fieldTransformers.add(new MCRMoveToRelatedItemIfExists(XPATH_HOST,
            new MCRField2XPathTransformer("edition", "mods:originInfo/mods:edition")));
        fieldTransformers.add(new MCRMoveToRelatedItemIfExists(XPATH_HOST,
            new MCRField2XPathTransformer("howpublished", "mods:originInfo/mods:edition")));
        fieldTransformers.add(new MCRMoveToRelatedItemIfExists(XPATH_HOST,
            new MCRField2XPathTransformer("publisher", "mods:originInfo/mods:publisher")));
        fieldTransformers.add(new MCRMoveToRelatedItemIfExists(XPATH_HOST,
            new MCRField2XPathTransformer("address", "mods:originInfo/mods:place/mods:placeTerm[@type='text']")));
        fieldTransformers.add(
            new MCRMoveToRelatedItemIfExists(XPATH_HOST + "[mods:genre='collection']", new MCRYearTransformer()));
        fieldTransformers.add(new MCRMoveToRelatedItemIfExists(
            XPATH_HOST + "[mods:genre='journal']|descendant::mods:relatedItem[@type='series']",
            new MCRField2XPathTransformer("volume", "mods:part/mods:detail[@type='volume']/mods:number")));
        fieldTransformers.add(new MCRField2XPathTransformer("number",
            XPATH_HOST + "/mods:part/mods:detail[@type='issue']/mods:number"));
        fieldTransformers.add(new MCRField2XPathTransformer("issue",
            XPATH_HOST + "/mods:part/mods:detail[@type='issue']/mods:number"));
        fieldTransformers.add(new MCRPagesTransformer());
        fieldTransformers.add(new MCRMoveToRelatedItemIfExists(XPATH_HOST, new MCRField2XPathTransformer("isbn",
            "mods:identifier[@type='isbn']" + MCRFieldTransformer.AS_NEW_ELEMENT)));
        fieldTransformers.add(new MCRMoveToRelatedItemIfExists(XPATH_HOST,
            new MCRField2XPathTransformer("series", "mods:relatedItem[@type='series']/mods:titleInfo/mods:title")));
        fieldTransformers.add(new MCRMoveToRelatedItemIfExists(
            XPATH_HOST + "[mods:genre='journal']|descendant::mods:relatedItem[@type='series']",
            new MCRField2XPathTransformer("issn",
                "mods:identifier[@type='issn']" + MCRFieldTransformer.AS_NEW_ELEMENT)));
        fieldTransformers.add(new MCRField2XPathTransformer("doi",
            "mods:identifier[@type='doi']" + MCRFieldTransformer.AS_NEW_ELEMENT));
        fieldTransformers.add(new MCRField2XPathTransformer("urn",
            "mods:identifier[@type='urn']" + MCRFieldTransformer.AS_NEW_ELEMENT));
        fieldTransformers.add(
            new MCRField2XPathTransformer("url", "mods:location/mods:url" + MCRFieldTransformer.AS_NEW_ELEMENT));
        fieldTransformers.add(new MCRField2XPathTransformer("keywords",
            "mods:subject" + MCRFieldTransformer.AS_NEW_ELEMENT + "/mods:topic"));
        fieldTransformers.add(new MCRField2XPathTransformer("author_keywords",
            "mods:subject" + MCRFieldTransformer.AS_NEW_ELEMENT + "/mods:topic"));
        fieldTransformers
            .add(new MCRField2XPathTransformer("abstract", "mods:abstract" + MCRFieldTransformer.AS_NEW_ELEMENT));
        fieldTransformers.add(new MCRField2XPathTransformer("note", "mods:note" + MCRFieldTransformer.AS_NEW_ELEMENT));
        fieldTransformers.add(new MCRField2XPathTransformer("type", "mods:note" + MCRFieldTransformer.AS_NEW_ELEMENT));
        fieldTransformers.add(new MCRField2XPathTransformer("source", "mods:recordInfo/mods:recordOrigin"));
        fieldTransformers.add(new MCRUnsupportedFieldTransformer(fieldTransformers));
    }

    Element transform(BibtexEntry entry) {
        Element mods = new Element("mods", MCRConstants.MODS_NAMESPACE);
        MCRGenreTransformer.setGenre(entry, mods);
        transformFields(entry, mods);
        MCRGenreTransformer.fixHostGenre(entry, mods);
        Element extension = getExtension(mods);
        extension.addContent(buildSourceExtension(entry));
        return mods;
    }

    private Element getExtension(Element mods) {
        Element extension = mods.getChild("extension", MCRConstants.MODS_NAMESPACE);
        if (extension == null) {
            extension = new Element("extension", MCRConstants.MODS_NAMESPACE);
            mods.addContent(extension);
        }
        return extension;
    }

    private Element buildSourceExtension(BibtexEntry entry) {
        Element source = new Element("source");
        source.setAttribute("format", "bibtex");
        source.setText(entry.toString());
        return source;
    }

    private void transformFields(BibtexEntry entry, Element mods) {
        for (MCRFieldTransformer transformer : fieldTransformers) {
            transformer.transformField(entry, mods);
        }
    }
}
