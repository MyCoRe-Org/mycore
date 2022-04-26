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

package org.mycore.mods.csl;

import static org.mycore.common.MCRConstants.MODS_NAMESPACE;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import de.undercouch.citeproc.helper.json.JsonBuilder;
import de.undercouch.citeproc.helper.json.StringJsonBuilderFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.common.content.MCRContent;
import org.mycore.csl.MCRItemDataProvider;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.frontend.MCRFrontendUtil;
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

    private static final Logger LOGGER = LogManager.getLogger();

    private static final String NON_DROPPING_PARTICLE = "nonDroppingParticle";

    private static final String DROPPING_PARTICLE = "droppingParticle";

    public static final String USABLE_TITLE_XPATH = "mods:titleInfo[not(@altFormat) and (not(@xlink:type)" +
        " or @xlink:type='simple')]";

    public static final String SHORT_TITLE_XPATH = "mods:titleInfo[not(@altFormat) and (not(@xlink:type)"  +
            " or @xlink:type='simple') and @type='abbreviated']";

    public static final String MODS_RELATED_ITEM_XPATH = "mods:relatedItem/";

    public static final String MODS_ORIGIN_INFO_PUBLICATION = "mods:originInfo[@eventType='publication' or not" +
            "(@eventType)]";
    public static final String NONE_TYPE = "none";

    public static final String URN_RESOLVER_LINK = "https://nbn-resolving.org/";

    private MCRMODSWrapper wrapper;

    private String id;

    private final Set<String> nonDroppingParticles = MCRConfiguration2.getString("MCR.CSL.NonDroppingParticles")
        .stream()
        .flatMap(str -> Stream.of(str.split(",")))
        .collect(Collectors.toSet());

    private final Set<String> droppingParticles = MCRConfiguration2.getString("MCR.CSL.DroppingParticles")
            .stream()
            .flatMap(str -> Stream.of(str.split(",")))
            .collect(Collectors.toSet());

    private static Stream<String> getModsElementTextStream(Element element, String elementName) {
        return element.getChildren(elementName, MODS_NAMESPACE)
            .stream()
            .map(Element::getTextNormalize);
    }

    @Override
    public CSLItemData retrieveItem(String id) {
        final CSLItemDataBuilder idb = new CSLItemDataBuilder().id(id);

        processURL(id, idb);
        processGenre(idb);
        processTitles(idb);
        processLanguage(idb);
        processNames(idb);
        processIdentifier(idb);
        processPublicationData(idb);
        processAbstract(idb);
        processModsPart(idb);
        processSubject(idb);

        CSLItemData build = idb.build();
        if(LOGGER.isDebugEnabled()){
            JsonBuilder jsonBuilder = new StringJsonBuilderFactory().createJsonBuilder();
            String str = (String) build.toJson(jsonBuilder);
            LOGGER.debug("Created json object: {}", str);
        }

        return build;
    }

    private void processSubject(CSLItemDataBuilder idb) {
        final String keyword = wrapper.getElements("mods:subject/mods:topic")
            .stream()
            .map(Element::getTextNormalize)
            .collect(Collectors.joining(", "));
        if (keyword.length() > 0) {
            idb.keyword(keyword);
        }
    }

    protected void processLanguage(CSLItemDataBuilder idb) {
        Optional.ofNullable(
                wrapper.getElement("mods:language/mods:languageTerm[@authority='rfc5646' or @authority='rfc4646']"))
                .or(() -> Optional.ofNullable(wrapper.getElement(MODS_RELATED_ITEM_XPATH +
                "mods:language/mods:languageTerm[@authority='rfc5646' or @authority='rfc4646']")))
                .ifPresent(el -> {
                    idb.language(el.getTextNormalize());
                });
    }


    protected void processURL(String id, CSLItemDataBuilder idb) {
        // use 1. urn 2. mods:location/mods:url  3. receive if there is a fulltext  4. url of parent
      Optional.ofNullable(wrapper.getElement("mods:identifier[@type='urn']"))
                .map(Element::getTextNormalize)
                .map((urn)-> URN_RESOLVER_LINK + urn)
            .or(() ->  Optional.ofNullable(wrapper.getElement("mods:location/mods:url"))
                    .map(Element::getTextNormalize))
            .or(() -> Optional.of(MCRFrontendUtil.getBaseURL() + "receive/" + id)
                .filter(url -> this.wrapper.getMCRObject().getStructure().getDerivates().size() > 0))
            .or(()-> Optional.ofNullable(wrapper.getElement("mods:relatedItem[@type='host']/mods:location/mods:url"))
                        .map(Element::getTextNormalize))
            .ifPresent(idb::URL);
    }

    protected void processModsPart(CSLItemDataBuilder idb) {
        String issueVolumeXP = "mods:relatedItem/mods:part[count(mods:detail[@type='issue' or @type='volume'])>0]";
        final Optional<Element> parentPartOpt = Optional.ofNullable(wrapper.getElement(issueVolumeXP))
            .or(() -> Optional.ofNullable(wrapper.getElement(".//" + issueVolumeXP)));
        parentPartOpt.ifPresent((modsPartElement) -> {
            final List<Element> detailElements = modsPartElement.getChildren("detail", MODS_NAMESPACE);
            for (Element detailElement : detailElements) {
                final String type = detailElement.getAttributeValue("type");
                final Element num = detailElement.getChild("number", MODS_NAMESPACE);
                if (num != null) {

                    Consumer<String> strFN = null;
                    Consumer<Integer> intFn = null;
                    switch (type) {
                        case "issue":
                            strFN = idb::issue;
                            intFn = idb::issue;
                            break;
                        case "volume":
                            strFN = idb::volume;
                            intFn = idb::volume;
                            break;
                        case "article_number":
                            intFn = idb::number;
                            strFN = idb::number;
                            break;
                        default:
                            LOGGER.warn("Unknown type " + type + " in mods:detail in " + this.id);
                            break;
                    }

                    try {
                        if (intFn != null) {
                            intFn.accept(Integer.parseInt(num.getTextNormalize()));
                        }
                    } catch (NumberFormatException nfe) {
                        /* if(strFN!=null){ java compiler: always true :O */
                        strFN.accept(num.getTextNormalize());
                        //}
                    }

                }
            }
        });

        final Element modsExtentElement = wrapper
                .getElement("mods:relatedItem[@type='host']/mods:part/mods:extent[@unit='pages']");
        if (modsExtentElement != null) {
            final String start = modsExtentElement.getChildTextNormalize("start", MODS_NAMESPACE);
            final String end = modsExtentElement.getChildTextNormalize("end", MODS_NAMESPACE);
            final String list = modsExtentElement.getChildTextNormalize("list", MODS_NAMESPACE);
            final String total = modsExtentElement.getChildTextNormalize("total", MODS_NAMESPACE);

            if (list != null) {
                idb.page(list);
            } else if (start != null && end != null) {
                idb.pageFirst(start);
                idb.page(start + "-" + end);
            } else if (start != null && total != null) {
                idb.pageFirst(start);

                try {
                    final int totalI = Integer.parseInt(total);
                    final int startI = Integer.parseInt(start);
                    idb.page(start + "-" + (totalI - startI));
                } catch (NumberFormatException e) {
                    idb.page(start);
                }

                idb.numberOfPages(total);
            } else if (start != null) {
                idb.pageFirst(start);
                idb.page(start);
            } else if (end != null) {
                idb.pageFirst(end);
                idb.page(end);
            }
        }

    }


    protected void processGenre(CSLItemDataBuilder idb) {
        final List<Element> elements = wrapper.getElements("mods:genre");
        final Set<String> genres = elements.stream()
            .map(this::getGenreStringFromElement)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        final List<Element> parentElements = wrapper.getElements("mods:relatedItem[@type='host']/mods:genre");
        final Set<String> parentGenres = parentElements.stream()
            .map(this::getGenreStringFromElement)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        if (genres.contains("article")) {
            if (parentGenres.contains("journal")) {
                idb.type(CSLType.ARTICLE_JOURNAL);
            } else if (parentGenres.contains("newspaper")) {
                idb.type(CSLType.ARTICLE_NEWSPAPER);
            } else {
                idb.type(CSLType.ARTICLE);
            }
        } else if (genres.contains("book") || genres.contains("proceedings") || genres.contains("collection")
            || genres.contains("festschrift") || genres.contains("lexicon") || genres.contains("monograph")
            || genres.contains("lecture")) {
            idb.type(CSLType.BOOK);
        } else if (genres.contains("interview")) {
            idb.type(CSLType.INTERVIEW);
        } else if (genres.contains("research_data")) {
            idb.type(CSLType.DATASET);
        } else if (genres.contains("patent")) {
            idb.type(CSLType.PATENT);
        } else if (genres.contains("chapter") || genres.contains("contribution")) {
            idb.type(CSLType.CHAPTER);
        } else if (genres.contains("entry")) {
            idb.type(CSLType.ENTRY_ENCYCLOPEDIA);
        } else if (genres.contains("preface")) {
            idb.type(CSLType.ARTICLE);
        } else if (genres.contains("speech") || genres.contains("poster")) {
            idb.type(CSLType.SPEECH);
        } else if (genres.contains("video")) {
            idb.type(CSLType.MOTION_PICTURE);
        } else if (genres.contains("broadcasting")) {
            idb.type(CSLType.BROADCAST);
        } else if (genres.contains("picture")) {
            idb.type(CSLType.GRAPHIC);
        } else if (genres.contains("review")) {
            idb.type(CSLType.REVIEW);
            if (parentGenres.contains("book")) {
                idb.type(CSLType.REVIEW_BOOK);
            }
        } else if (genres.contains("thesis") || genres.contains("exam") || genres.contains("dissertation")
            || genres.contains("habilitation") || genres.contains("diploma_thesis") || genres.contains("master_thesis")
            || genres.contains("bachelor_thesis") || genres.contains("student_research_project")
            || genres.contains("magister_thesis")) {
            idb.type(CSLType.THESIS);
        } else if (genres.contains("report") || genres.contains("research_results") || genres.contains("in_house")
            || genres.contains("press_release") || genres.contains("declaration")) {
            idb.type(CSLType.REPORT);
            /* } else if (genres.contains("teaching_material") || genres.contains("lecture_resource")
               || genres.contains("course_resources")) {
               // TODO: find right mapping
              } else if (genres.contains("series")) {
               // TODO: find right mapping
              } else if (genres.contains("journal")) {
               // TODO: find right mapping
              } else if (genres.contains("newspaper")) {
               // TODO: find right mapping
              */
        } else {
            idb.type(CSLType.ARTICLE);
        }
    }

    protected String getGenreStringFromElement(Element genre) {
        if(genre.getAttributeValue("authorityURI")!=null){
            MCRCategoryID categoryID = MCRClassMapper.getCategoryID(genre);
            if(categoryID==null){
                return null;
            }
            return categoryID.getID();
        } else {
            return genre.getText();
        }
    }

    protected void processAbstract(CSLItemDataBuilder idb) {
        Optional.ofNullable(wrapper.getElement("mods:abstract[not(@altFormat)]")).ifPresent(abstr -> {
            idb.abstrct(abstr.getTextNormalize());
        });
    }

    protected void processPublicationData(CSLItemDataBuilder idb) {
        Optional.ofNullable(wrapper.getElement(MODS_ORIGIN_INFO_PUBLICATION + "/mods:place/mods:placeTerm"))
            .or(() -> Optional.ofNullable(wrapper.getElement(MODS_RELATED_ITEM_XPATH +
                    MODS_ORIGIN_INFO_PUBLICATION + "/mods:place/mods:placeTerm")))
            .ifPresent(el -> {
                idb.publisherPlace(el.getTextNormalize());
            });

        Optional.ofNullable(wrapper.getElement(MODS_ORIGIN_INFO_PUBLICATION + "/mods:publisher"))
            .or(() -> Optional.ofNullable(wrapper.getElement(MODS_RELATED_ITEM_XPATH +
                    MODS_ORIGIN_INFO_PUBLICATION + "/mods:publisher")))
            .ifPresent(el -> {
                idb.publisher(el.getTextNormalize());
            });

        Optional.ofNullable(wrapper.getElement(MODS_ORIGIN_INFO_PUBLICATION + "/mods:edition"))
            .or(() -> Optional.ofNullable(wrapper.getElement(MODS_RELATED_ITEM_XPATH +
                    MODS_ORIGIN_INFO_PUBLICATION + "/mods:edition")))
            .ifPresent(el -> {
                idb.edition(el.getTextNormalize());
            });

        Optional.ofNullable(wrapper.getElement(MODS_ORIGIN_INFO_PUBLICATION + "/mods:dateIssued"))
            .or(() -> Optional.ofNullable(wrapper.getElement(MODS_RELATED_ITEM_XPATH +
                    MODS_ORIGIN_INFO_PUBLICATION + "/mods:dateIssued")))
            .ifPresent(el -> {
                idb.issued(new CSLDateBuilder().raw(el.getTextNormalize()).build());
            });
    }

    protected void processIdentifier(CSLItemDataBuilder idb) {
        final List<Element> parentIdentifiers = wrapper.getElements("mods:relatedItem[@type='host']/mods:identifier");

        parentIdentifiers.forEach(parentIdentifier -> {
            applyIdentifier(idb, parentIdentifier, true);
        });

        final List<Element> identifiers = wrapper.getElements("mods:identifier");
        identifiers.forEach(identifierElement -> {
            applyIdentifier(idb, identifierElement, false);
        });

    }

    private void applyIdentifier(CSLItemDataBuilder idb, Element identifierElement, boolean parent) {
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
                if (!parent) {
                    idb.PMID(identifier);
                }
                break;
            case "pmcid":
                if (!parent) {
                    idb.PMCID(identifier);
                }
                break;
        }
    }

    protected void processTitles(CSLItemDataBuilder idb) {
        final Element titleInfoElement = wrapper.getElement(USABLE_TITLE_XPATH);
        if (titleInfoElement != null) {
            idb.titleShort(buildShortTitle(titleInfoElement));
            idb.title(buildTitle(titleInfoElement));
        }

        final Element titleInfoShortElement = wrapper.getElement(SHORT_TITLE_XPATH);
        if(titleInfoShortElement != null){
            idb.titleShort(buildShortTitle(titleInfoShortElement));
        }

        Optional.ofNullable(wrapper.getElement("mods:relatedItem[@type='host']/" + USABLE_TITLE_XPATH))
            .ifPresent((titleInfo) -> {
            idb.containerTitleShort(buildShortTitle(titleInfo));
            idb.containerTitle(buildTitle(titleInfo));
        });

        Optional.ofNullable(wrapper.getElement("mods:relatedItem[@type='host']/" + SHORT_TITLE_XPATH))
            .ifPresent((titleInfo) -> {
            idb.containerTitleShort(buildShortTitle(titleInfo));
        });

        wrapper.getElements(".//mods:relatedItem[@type='series']/" + USABLE_TITLE_XPATH).stream()
                .findFirst().ifPresent((relatedItem)-> {
            idb.collectionTitle(buildTitle(relatedItem));
        });
    }

    protected void processNames(CSLItemDataBuilder idb) {
        final List<Element> modsNameElements = wrapper.getElements("mods:name");
        HashMap<String, List<CSLName>> roleNameMap = new HashMap<>();
        for (Element modsName : modsNameElements) {
            final CSLName cslName = buildName(modsName);
            if(isNameEmpty(cslName)) {
                continue;
            }
            fillRoleMap(roleNameMap, modsName, cslName);
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
                case "ive":
                    idb.author(cslNames);
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
                default:
                    LOGGER.warn("Unknown person role " + role + " in " + this.id);
                    break;
            }
        });


        HashMap<String, List<CSLName>> parentRoleMap = new HashMap<>();
        final List<Element> parentModsNameElements = wrapper.getElements("mods:relatedItem/mods:name");

        for (Element modsName : parentModsNameElements) {
            final CSLName cslName = buildName(modsName);
            if(isNameEmpty(cslName)) {
                continue;
            }
            fillRoleMap(parentRoleMap, modsName, cslName);
        }
        parentRoleMap.forEach((role, list) -> {
            final CSLName[] cslNames = list.toArray(list.toArray(new CSLName[0]));
            switch (role) {
                case "aut":
                    idb.containerAuthor(cslNames);
                    break;
                case "edt":
                    idb.collectionEditor(cslNames);
                    break;
                default:
                    // we dont care
                    break;
            }
        });
    }

    private void fillRoleMap(HashMap<String, List<CSLName>> roleNameMap, Element modsName, CSLName cslName) {
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

    private CSLName buildName(Element modsName) {
        final CSLNameBuilder nameBuilder = new CSLNameBuilder();

        String nameType = modsName.getAttributeValue("type");
        final boolean isInstitution = "corporate".equals(nameType) || "conference".equals(nameType);
        nameBuilder.isInstitution(isInstitution);

        if (!isInstitution) {
            //todo: maybe better mapping here
            HashMap<String, List<String>> typeContentsMap = new HashMap<>();
            modsName.getChildren("namePart", MODS_NAMESPACE).forEach(namePart -> {
                final String type = namePart.getAttributeValue("type");
                final String content = namePart.getTextNormalize();
                if (("family".equals(type) || "given".equals(type)) && nonDroppingParticles.contains(content)) {
                    final List<String> contents = typeContentsMap.computeIfAbsent(NON_DROPPING_PARTICLE,
                        (t) -> new LinkedList<>());
                    contents.add(content);
                } else if(("family".equals(type) || "given".equals(type)) && droppingParticles.contains(content)) {
                    final List<String> contents = typeContentsMap.computeIfAbsent(NON_DROPPING_PARTICLE,
                            (t) -> new LinkedList<>());
                    contents.add(content);
                }else {
                    final List<String> contents = typeContentsMap.computeIfAbsent(Optional.ofNullable(type)
                            .orElse(NONE_TYPE), (t) -> new LinkedList<>());
                    contents.add(content);
                }
            });

            if (typeContentsMap.containsKey("family")) {
                nameBuilder.family(String.join(" ", typeContentsMap.get("family")));
            }

            if (typeContentsMap.containsKey("given")) {
                nameBuilder.given(String.join(" ", typeContentsMap.get("given")));
            }

            if (typeContentsMap.containsKey(NON_DROPPING_PARTICLE)) {
                nameBuilder.nonDroppingParticle(String.join(" ", typeContentsMap.get(NON_DROPPING_PARTICLE)));
            }

            if (typeContentsMap.containsKey(DROPPING_PARTICLE)) {
                nameBuilder.droppingParticle(String.join(" ", typeContentsMap.get(DROPPING_PARTICLE)));
            }

            if (typeContentsMap.containsKey(NONE_TYPE)) {
                nameBuilder.literal(String.join(" ", typeContentsMap.get(NONE_TYPE)));
            }

            Element displayForm = modsName.getChild("displayForm", MODS_NAMESPACE);
            if (typeContentsMap.isEmpty() && displayForm != null) {
                LOGGER.warn("The displayForm ({}) is used, because no mods name elements are present in doc {}!",
                    displayForm.getTextNormalize(), this.id);
                nameBuilder.literal(displayForm.getTextNormalize());
            }
        } else {
            String lit = Optional.ofNullable(modsName.getChildTextNormalize("displayForm", MODS_NAMESPACE))
                .orElse(modsName.getChildren("namePart", MODS_NAMESPACE).stream().map(Element::getTextNormalize)
                    .collect(Collectors.joining(" ")));

            if (!lit.isBlank()) {
                nameBuilder.literal(lit);
            }
        }

        return nameBuilder.build();
    }

    protected boolean isNameEmpty(CSLName cslName) {
        Predicate<String> isNullOrEmpty = (p) -> p == null || p.isEmpty();
        return isNullOrEmpty.test(cslName.getFamily()) &&
            isNullOrEmpty.test(cslName.getGiven()) &&
            isNullOrEmpty.test(cslName.getDroppingParticle()) &&
            isNullOrEmpty.test(cslName.getNonDroppingParticle()) &&
            isNullOrEmpty.test(cslName.getSuffix()) &&
            isNullOrEmpty.test(cslName.getLiteral());
    }

    protected String buildShortTitle(Element titleInfoElement) {
        return titleInfoElement.getChild("title", MODS_NAMESPACE).getText();
    }

    protected String buildTitle(Element titleInfoElement) {
        StringBuilder titleBuilder = new StringBuilder();

        titleBuilder.append(Stream.of("nonSort", "title")
                .flatMap(n -> getModsElementTextStream(titleInfoElement, n))
                .collect(Collectors.joining(" ")));

        final String subTitle = getModsElementTextStream(titleInfoElement, "subTitle").collect(Collectors.joining(" "));
        if (subTitle.length() > 0) {
            titleBuilder.append(": ").append(subTitle);
        }

        titleBuilder.append(Stream.of("partNumber", "partName")
            .flatMap(n -> getModsElementTextStream(titleInfoElement, n))
            .collect(Collectors.joining(",")));
        return titleBuilder.toString();
    }

    @Override
    public String[] getIds() {
        return new String[] { id };
    }

    @Override
    public void addContent(MCRContent content) throws IOException, JDOMException, SAXException {
        final Document document = content.asXML();
        addContent(document);
    }

    protected void addContent(Document document) {
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
