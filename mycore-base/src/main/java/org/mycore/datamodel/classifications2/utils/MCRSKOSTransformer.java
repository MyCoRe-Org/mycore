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

package org.mycore.datamodel.classifications2.utils;

import org.apache.commons.lang3.StringUtils;
import org.jdom2.Document;
import org.jdom2.Element;
import org.mycore.common.MCRConstants;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRLabel;
import org.mycore.frontend.MCRFrontendUtil;

public class MCRSKOSTransformer {

    public static Document getSKOSasRDFXML(MCRCategory categ) {
        Element eRDF = new Element("RDF", MCRConstants.RDF_NAMESPACE);
        eRDF.addNamespaceDeclaration(MCRConstants.SKOS_NAMESPACE);
        if (categ.isClassification()) {
            createSkosConceptScheme(categ, eRDF);
        }
        if (categ.isCategory()) {
            createSkosConcept(categ, eRDF);
        }

        return new Document(eRDF);
    }

    /**
     * creates a rdf element skos:Concept and its XML structure with attributes and child elements
     * @param categ - the MyCoRe category object
     * @param eRDF the RDF root elements which collects all concepts
     */
    private static void createSkosConceptScheme(MCRCategory categ, Element eRDF) {
        if (categ != null) {
            Element eConcept = new Element("ConceptScheme", MCRConstants.SKOS_NAMESPACE);
            eRDF.addContent(eConcept);

            eConcept.setAttribute("about", retrieveURI(categ), MCRConstants.RDF_NAMESPACE);
            convertLabelsAndDescriptions(categ, eConcept);

            for (MCRCategory child : categ.getChildren()) {
                eConcept.addContent(new Element("hasTopConcept", MCRConstants.SKOS_NAMESPACE)
                    .setAttribute("resource", retrieveURI(child), MCRConstants.RDF_NAMESPACE));
                createSkosConcept(child, eRDF);
            }
        }
    }

    /**
    * creates a rdf element skos:Concept and its XML structure with attributes and child elements
    * @param categ - the MyCoRe category object
    * @param eRDF the RDF root elements which collects all concepts
    */
    private static void createSkosConcept(MCRCategory categ, Element eRDF) {
        if (categ != null) {
            Element eConcept = new Element("Concept", MCRConstants.SKOS_NAMESPACE);
            eRDF.addContent(eConcept);

            eConcept.setAttribute("about", retrieveURI(categ), MCRConstants.RDF_NAMESPACE);
            convertLabelsAndDescriptions(categ, eConcept);

            if (categ.getParent() != null) {
                if (categ.getParent().isClassification()) {
                    // skos:topConceptOf is a sub-property of skos:inScheme.
                    eConcept.addContent(new Element("isTopConceptOf", MCRConstants.SKOS_NAMESPACE)
                        .setAttribute("resource", retrieveURI(categ.getParent()), MCRConstants.RDF_NAMESPACE));
                }
                if (categ.getParent().isCategory()) {
                    eConcept.addContent(new Element("broader", MCRConstants.SKOS_NAMESPACE)
                        .setAttribute("resource", retrieveURI(categ.getParent()), MCRConstants.RDF_NAMESPACE));
                    eConcept.addContent(new Element("inScheme", MCRConstants.SKOS_NAMESPACE)
                        .setAttribute("resource", retrieveURI(categ.getRoot()), MCRConstants.RDF_NAMESPACE));
                }
            }

            for (MCRCategory child : categ.getChildren()) {
                eConcept.addContent(new Element("narrower", MCRConstants.SKOS_NAMESPACE)
                    .setAttribute("resource", retrieveURI(child), MCRConstants.RDF_NAMESPACE));
                createSkosConcept(child, eRDF);
            }
        }
    }

    /**
     * converts classification labels and descriptions to skos:preferedLabels
     * @param categ
     * @param eConcept
     */
    private static void convertLabelsAndDescriptions(MCRCategory categ, Element eConcept) {
        for (MCRLabel lbl : categ.getLabels()) {
            if (!lbl.getLang().startsWith("x-")) {
                eConcept.addContent(new Element("prefLabel", MCRConstants.SKOS_NAMESPACE)
                    .setAttribute("lang", lbl.getLang(), MCRConstants.XML_NAMESPACE)
                    .setText(lbl.getText()));
            }
        }
        for (MCRLabel lbl : categ.getLabels()) {
            if (!lbl.getLang().startsWith("x-") && StringUtils.isNotEmpty(lbl.getDescription())) {
                eConcept.addContent(new Element("definition", MCRConstants.SKOS_NAMESPACE)
                    .setAttribute("lang", lbl.getLang(), MCRConstants.XML_NAMESPACE)
                    .setText(lbl.getDescription()));
            }
        }
        categ.getLabel("x-skos-exact").ifPresent(x -> {
            for (String uri : x.getText().split("\\s")) {
                eConcept.addContent(new Element("exactMatch", MCRConstants.SKOS_NAMESPACE)
                    .setAttribute("resource", uri, MCRConstants.RDF_NAMESPACE));
            }
        });
    }

    /**
     * retrieves the URI for the SKOS Concept or Concept Scheme
     * 
     * There is no consistent use of URI and x-uri attributes in MyCoRe
     * Furthermore we need to define a new endpoint /entities for Linked Data output
     * 
     * Details of the implementation needs to be discussed within the community
     * 
     * @param categ the MCRCategory object
     * 
     * @return the URI as String
     */
    private static String retrieveURI(MCRCategory categ) {
        /*
         * The use of URI or x-URI label is not standardized in MyCoRe
         * It needs to be discussed in the Community 
         * 
        if (categ.getURI() != null) {
            return categ.getURI().toString();
        }
        if (categ.getLabel("x-uri").isPresent()) {
            return categ.getLabel("x-uri").get().getText();
        }
        */
        if (categ.isClassification()) {
            return MCRFrontendUtil.getBaseURL() + "entity/classification/" + categ.getId().getRootID();
        }
        return MCRFrontendUtil.getBaseURL() + "entity/classification/" + categ.getId().getRootID() + "/"
            + categ.getId().getID();
    }

}
