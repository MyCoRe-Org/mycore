package org.mycore.mods.csl;

import static org.mycore.common.MCRConstants.MODS_NAMESPACE;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.mycore.common.content.MCRContent;
import org.mycore.csl.MCRItemDataProvider;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.mods.MCRMODSWrapper;
import org.mycore.mods.classification.MCRClassMapper;
import org.xml.sax.SAXException;

import de.undercouch.citeproc.csl.CSLDateBuilder;
import de.undercouch.citeproc.csl.CSLItemData;
import de.undercouch.citeproc.csl.CSLItemDataBuilder;
import de.undercouch.citeproc.csl.CSLName;
import de.undercouch.citeproc.csl.CSLNameBuilder;
import de.undercouch.citeproc.csl.CSLType;

public class MCRModsItemDataProvider extends MCRItemDataProvider {

    private MCRMODSWrapper wrapper;

    private String id;

    @Override
    public CSLItemData retrieveItem(String id) {
        final CSLItemDataBuilder idb = new CSLItemDataBuilder().id(id);

        processGenre(idb);
        processTitles(idb);
        processNames(idb);
        processIdentifier(idb);
        processPublicationData(idb);
        processAbstract(idb);
        processModsPart(idb);

        return idb.build();
    }

    private void processModsPart(CSLItemDataBuilder idb) {
        final Element modsPartElement = wrapper.getElement("mods:relatedItem[@type='host']/mods:part");
        if (modsPartElement != null) {
            final List<Element> detailElements = modsPartElement.getChildren("detail", MODS_NAMESPACE);
            for (Element detailElement : detailElements) {
                final String type = detailElement.getAttributeValue("type");
                final Element num = detailElement.getChild("number", MODS_NAMESPACE);
                if (num != null) {
                    final int number = Integer.parseInt(num.getTextNormalize());
                    switch (type) {
                        case "issue":
                            idb.issue(number);
                            break;
                        case "volume":
                            idb.volume(number);
                            break;
                    }
                }
            }
            final Element modsExtentElement = wrapper
                .getElement("mods:relatedItem[@type='host']/mods:part/mods:extent[@unit='pages']");
            if (modsExtentElement != null) {
                final String start = modsExtentElement.getChildTextNormalize("start", MODS_NAMESPACE);
                final String end = modsExtentElement.getChildTextNormalize("end", MODS_NAMESPACE);
                if (start != null) {
                    idb.pageFirst(start);
                }
                if (end != null) {
                    idb.page(end);
                }
            }
        }
    }

    private void processGenre(CSLItemDataBuilder idb) {
        final List<Element> elements = wrapper.getElements("mods:genre");
        final Set<String> genres = elements.stream()
            .map(MCRClassMapper::getCategoryID)
            .filter(Objects::nonNull)
            .map(MCRCategoryID::getID)
            .collect(Collectors.toSet());

        final List<Element> parentElements = wrapper.getElements("mods:relatedItem[@type='host']/mods:genre");
        final Set<String> parentGenres = parentElements.stream()
            .map(MCRClassMapper::getCategoryID)
            .filter(Objects::nonNull)
            .map(MCRCategoryID::getID)
            .collect(Collectors.toSet());

        if (genres.contains("article")) {
            if (parentGenres.contains("journal")) {
                idb.type(CSLType.ARTICLE_JOURNAL);
            } else if (parentGenres.contains("newspaper")) {
                idb.type(CSLType.ARTICLE_NEWSPAPER);
            } else {
                idb.type(CSLType.ARTICLE);
            }
        } else if (genres.contains("book")) {
            idb.type(CSLType.BOOK);
        } else if (genres.contains("interview")) {
            idb.type(CSLType.INTERVIEW);
        } else if (genres.contains("research_data")) {
            idb.type(CSLType.DATASET);
        } else if (genres.contains("patent")) {
            idb.type(CSLType.PATENT);
        } else if (genres.contains("chapter")) {
            idb.type(CSLType.CHAPTER);
        } else if (genres.contains("entry")) {
            idb.type(CSLType.ENTRY_ENCYCLOPEDIA);
        } else if (genres.contains("preface")) {
            idb.type(CSLType.ARTICLE);
        } else if (genres.contains("speech")) {
            idb.type(CSLType.SPEECH);
        } else if (genres.contains("video")) {
            idb.type(CSLType.MOTION_PICTURE);
        } else if (genres.contains("broadcasting")) {
            idb.type(CSLType.BROADCAST);
        } else if (genres.contains("review")) {
            idb.type(CSLType.REVIEW);
            if (parentGenres.contains("book")) {
                idb.type(CSLType.REVIEW_BOOK);
            }
        } else if (genres.contains("thesis") || genres.contains("exam") || genres.contains("dissertation")
            || genres.contains("habilitation") || genres.contains("diploma_thesis") || genres.contains("master_thesis")
            || genres.contains("bachelor_thesis") || genres.contains("student_resarch_project")
            || genres.contains("magister_thesis")) {
            idb.type(CSLType.THESIS);
        } else if (genres.contains("report") || genres.contains("research_results") || genres.contains("in_house")
            || genres.contains("press_release") || genres.contains("declaration")) {
            idb.type(CSLType.REPORT);
        } /* else if (genres.contains("teaching_material") || genres.contains("lecture_resource")
               || genres.contains("course_resources")) {
           // TODO: find right mapping
          } else if (genres.contains("lexicon")) {
           // TODO: find right mapping
          } else if (genres.contains("collection")) {
           // TODO: find right mapping
          } else if(genres.contains("lecure")){
           // TODO: find right mapping
          } else if (genres.contains("series")) {
           // TODO: find right mapping
          } else if (genres.contains("journal")) {
           // TODO: find right mapping
          } else if (genres.contains("newspaper")) {
           // TODO: find right mapping
          } else if (genres.contains("picture")) {
           // TODO: find right mapping
          } else if (genres.contains("poster")) {
           // TODO: find right mapping
          } else {
          
          } */
    }

    private void processAbstract(CSLItemDataBuilder idb) {
        Optional.ofNullable(wrapper.getElement("mods:abstract[not(@altFormat)]")).ifPresent(abstr -> {
            idb.abstrct(abstr.getTextNormalize());
        });
    }

    private void processPublicationData(CSLItemDataBuilder idb) {
        Optional.ofNullable(wrapper.getElement("mods:originInfo[@eventType='publication']/mods:place/mods:placeTerm"))
            .ifPresent(el -> {
                idb.publisherPlace(el.getTextNormalize());
            });

        Optional.ofNullable(wrapper.getElement("mods:originInfo[@eventType='publication']/mods:publisher"))
            .ifPresent(el -> {
                idb.publisher(el.getTextNormalize());
            });

        Optional.ofNullable(wrapper.getElement("mods:originInfo[@eventType='publication']/mods:edition"))
            .ifPresent(el -> {
                idb.edition(el.getTextNormalize());
            });

        Optional.ofNullable(wrapper.getElement("mods:originInfo[@eventType='publication']/mods:dateIssued"))
            .ifPresent(el -> {
                idb.issued(new CSLDateBuilder().raw(el.getTextNormalize()).build());
            });
    }

    private void processIdentifier(CSLItemDataBuilder idb) {
        final List<Element> identifiers = wrapper.getElements("mods:identifier");

        identifiers.forEach(identifierElement -> {
            final String type = identifierElement.getAttributeValue("type");
            final String identifier = identifierElement.getTextNormalize();
            switch (type) {
                case "doi":
                    idb.DOI(identifier);
                    break;
                case "isbn":
                    idb.ISBN(identifier);
                    break;
                case "issn":
                    idb.ISSN(identifier);
                    break;
                case "pmid":
                    idb.PMID(identifier);
                    break;
                case "pmcid":
                    idb.PMCID(identifier);
                    break;
            }
        });

    }

    private void processTitles(CSLItemDataBuilder idb) {
        final Element titleInfoElement = wrapper.getElement("mods:titleInfo[not(@xlink:type) or @xlink:type='simple']");
        if (titleInfoElement != null) {
            idb.titleShort(buildShortTitle(titleInfoElement));
            idb.title(buildTitle(titleInfoElement));
        }

        final Element parentTitleInfoElement = wrapper
            .getElement("mods:relatedItem[@type='host']/mods:titleInfo[not(@xlink:type)" +
                " or @xlink:type='simple']");
        if (parentTitleInfoElement != null) {
            idb.containerTitleShort(buildShortTitle(parentTitleInfoElement));
            idb.containerTitle(buildTitle(parentTitleInfoElement));
        }

    }

    private void processNames(CSLItemDataBuilder idb) {
        final List<Element> modsNameElements = wrapper.getElements("mods:name");
        HashMap<String, List<CSLName>> roleNameMap = new HashMap<>();
        for (Element modsName : modsNameElements) {
            final CSLNameBuilder nameBuilder = new CSLNameBuilder();

            nameBuilder.isInstitution("false".equals(modsName.getAttributeValue("personal")));

            //todo: maybe better mapping here
            HashMap<String, List<String>> typeContentsMap = new HashMap<>();
            modsName.getChildren("namePart", MODS_NAMESPACE).forEach(namePart -> {
                final String type = namePart.getAttributeValue("type");
                final String content = namePart.getTextNormalize();
                final List<String> contents = typeContentsMap.computeIfAbsent(type, (t) -> new LinkedList<>());
                contents.add(content);
            });

            if (typeContentsMap.containsKey("family")) {
                nameBuilder.family(String.join(" ", typeContentsMap.get("family")));
            }

            if (typeContentsMap.containsKey("given")) {
                nameBuilder.given(String.join(" ", typeContentsMap.get("given")));
            }

            final CSLName cslName = nameBuilder.build();
            final Element roleElement = modsName.getChild("role", MODS_NAMESPACE);
            if (roleElement != null) {
                final List<Element> roleTerms = roleElement.getChildren("roleTerm", MODS_NAMESPACE);
                for (Element roleTermElement : roleTerms) {
                    final String role = roleTermElement.getTextNormalize();
                    final List<CSLName> cslNames = roleNameMap.computeIfAbsent(role, (s) -> new LinkedList<>());
                    cslNames.add(cslName);
                }
            }
        }

        roleNameMap.forEach((role, list) -> {
            final CSLName[] cslNames = list.toArray(list.toArray(new CSLName[0]));
            switch (role) {
                case "aut":
                    idb.author(cslNames);
                    break;
                case "col":
                    idb.collectionEditor(cslNames);
                    break;
                case "edt":
                    idb.editor(cslNames);
                    break;
                case "fmd":
                    idb.director(cslNames);
                    break;
                case "ivr":
                    idb.interviewer(cslNames);
                    break;
                case "ill":
                    idb.illustrator(cslNames);
                    break;
                case "trl":
                    idb.translator(cslNames);
                    break;
                case "cmp":
                    idb.composer(cslNames);
                    break;
            }
        });

    }

    private String buildShortTitle(Element titleInfoElement) {
        return titleInfoElement.getChild("title", MODS_NAMESPACE).getText();
    }

    private String buildTitle(Element titleInfoElement) {
        StringBuilder titleBuilder = new StringBuilder();
        titleBuilder.append(getModsElementTextStream(titleInfoElement, "nonSort").collect(Collectors.joining(" ")));
        titleBuilder.append(getModsElementTextStream(titleInfoElement, "title").collect(Collectors.joining(" ")));
        final String subTitle = getModsElementTextStream(titleInfoElement, "subTitle").collect(Collectors.joining(" "));
        if (subTitle.length() > 0) {
            titleBuilder.append(":").append(subTitle);
        }

        titleBuilder.append(Stream.of("partNumber", "partName")
            .flatMap(n -> getModsElementTextStream(titleInfoElement, n))
            .collect(Collectors.joining(",")));
        return titleBuilder.toString();
    }

    private static Stream<String> getModsElementTextStream(Element element, String elementName) {
        return element.getChildren(elementName, MODS_NAMESPACE)
            .stream()
            .map(Element::getTextNormalize);
    }

    @Override
    public String[] getIds() {
        return new String[] { id };
    }

    @Override
    public void addContent(MCRContent content) throws IOException, JDOMException, SAXException {
        final Document document = content.asXML();
        final MCRObject object = new MCRObject(document);
        wrapper = new MCRMODSWrapper(object);
        this.id = object.getId().toString();
    }

    @Override
    public void reset() {
        this.id = null;
        this.wrapper = null;
    }
}
