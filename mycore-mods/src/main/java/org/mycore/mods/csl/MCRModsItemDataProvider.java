/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

import de.undercouch.citeproc.csl.CSLDateBuilder;
import de.undercouch.citeproc.csl.CSLItemData;
import de.undercouch.citeproc.csl.CSLItemDataBuilder;
import de.undercouch.citeproc.csl.CSLName;
import de.undercouch.citeproc.csl.CSLNameBuilder;
import de.undercouch.citeproc.csl.CSLType;
import de.undercouch.citeproc.helper.json.JsonBuilder;
import de.undercouch.citeproc.helper.json.StringJsonBuilderFactory;

public class MCRModsItemDataProvider extends MCRItemDataProvider {

    public static final String USABLE_TITLE_XPATH = "mods:titleInfo[not(@altFormat) and (not(@xlink:type)" +
        " or @xlink:type='simple')]";
    public static final String SHORT_TITLE_XPATH = "mods:titleInfo[not(@altFormat) and (not(@xlink:type)" +
        " or @xlink:type='simple') and @type='abbreviated']";
    public static final String MODS_RELATED_ITEM_XPATH = "mods:relatedItem/";
    public static final String MODS_ORIGIN_INFO_PUBLICATION = "mods:originInfo[@eventType='publication' or not" +
        "(@eventType)]";
    public static final String NONE_TYPE = "none";
    public static final String URN_RESOLVER_LINK = "https://nbn-resolving.org/";
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String NON_DROPPING_PARTICLE = "nonDroppingParticle";
    private static final String DROPPING_PARTICLE = "droppingParticle";
    private static final Set<String> KNOWN_UNMAPPED_PERSON_ROLES = MCRConfiguration2
        .getString("MCR.CSL.KnownUnmappedPersonRoles")
        .stream()
        .flatMap(str -> Stream.of(str.split(",")))
        .collect(Collectors.toUnmodifiableSet());
    public static final String ATTRIBUTE_TYPE = "type";
    private final Set<String> nonDroppingParticles = MCRConfiguration2.getString("MCR.CSL.NonDroppingParticles")
        .stream()
        .flatMap(str -> Stream.of(str.split(",")))
        .collect(Collectors.toSet());
    private final Set<String> droppingParticles = MCRConfiguration2.getString("MCR.CSL.DroppingParticles")
        .stream()
        .flatMap(str -> Stream.of(str.split(",")))
        .collect(Collectors.toSet());
    private MCRMODSWrapper wrapper;
    private String id;

    public enum ModsGenre {
        ARTICLE("article"),
        CONFERENCE("conference"),
        BOOK("book"),
        INTERVIEW("interview"),
        RESEARCH_DATA("research_data"),
        PATENT("patent"),
        CHAPTER("chapter"),
        ENTRY("entry"),
        PREFACE("preface"),
        SPEECH("speech"),
        VIDEO("video"),
        BROADCASTING("broadcasting"),
        PICTURE("picture"),
        REVIEW("review"),
        THESIS("thesis"),
        REPORT("report");

        private final String value;

        ModsGenre(String value) {
            this.value = value;
        }

        public static ModsGenre fromString(String value) {
            for (ModsGenre genre : values()) {
                if (genre.value.equalsIgnoreCase(value)) {
                    return genre;
                }
            }
            return null;
        }
    }

    private static Stream<String> getModsElementTextStream(Element element, String elementName) {
        return element.getChildren(elementName, MODS_NAMESPACE)
            .stream()
            .map(Element::getTextNormalize);
    }

    @Override
    public CSLItemData retrieveItem(String id) {
        final CSLItemDataBuilder idb = new CSLItemDataBuilder().id(id);

        processMyCoReId(id, idb);
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
        if (LOGGER.isDebugEnabled()) {
            JsonBuilder jsonBuilder = new StringJsonBuilderFactory().createJsonBuilder();
            String str = (String) build.toJson(jsonBuilder);
            LOGGER.debug("Created json object: {}", str);
        }

        return build;
    }

    private void processMyCoReId(String id, CSLItemDataBuilder idb) {
        idb.citationKey(id);
    }

    private void processSubject(CSLItemDataBuilder idb) {
        final String keyword = wrapper.getElements("mods:subject/mods:topic")
            .stream()
            .map(Element::getTextNormalize)
            .collect(Collectors.joining(", "));
        if (!keyword.isEmpty()) {
            idb.keyword(keyword);
        }
    }

    protected void processLanguage(CSLItemDataBuilder idb) {
        Optional.ofNullable(
            wrapper.getElement("mods:language/mods:languageTerm[@authority='rfc5646' or @authority='rfc4646']"))
            .or(() -> Optional.ofNullable(wrapper.getElement(MODS_RELATED_ITEM_XPATH +
                "mods:language/mods:languageTerm[@authority='rfc5646' or @authority='rfc4646']")))
            .ifPresent(el -> idb.language(el.getTextNormalize()));
    }

    protected void processURL(String id, CSLItemDataBuilder idb) {
        // use 1. urn 2. mods:location/mods:url  3. receive if there is a fulltext  4. url of parent
        Optional.ofNullable(wrapper.getElement("mods:identifier[@type='urn']"))
            .map(Element::getTextNormalize)
            .map((urn) -> URN_RESOLVER_LINK + urn)
            .or(() -> Optional.ofNullable(wrapper.getElement("mods:location/mods:url"))
                .map(Element::getTextNormalize))
            .or(() -> Optional.of(MCRFrontendUtil.getBaseURL() + "receive/" + id)
                .filter(url -> !this.wrapper.getMCRObject().getStructure().getDerivates().isEmpty()))
            .or(() -> Optional.ofNullable(wrapper.getElement("mods:relatedItem[@type='host']/mods:location/mods:url"))
                .map(Element::getTextNormalize))
            .ifPresent(idb::URL);
    }

    protected void processModsPart(CSLItemDataBuilder idb) {
        String issueVolumeXP = "mods:relatedItem/mods:part[count(mods:detail[@type='issue' or @type='volume'"
            + " or @type='article_number'])>0]";
        final Optional<Element> parentPartOpt = Optional.ofNullable(wrapper.getElement(issueVolumeXP))
            .or(() -> Optional.ofNullable(wrapper.getElement(".//" + issueVolumeXP)));
        parentPartOpt.ifPresent((modsPartElement) -> {
            final List<Element> detailElements = modsPartElement.getChildren("detail", MODS_NAMESPACE);
            for (Element detailElement : detailElements) {
                processModsPartDetail(idb, detailElement);
            }
        });

        final Element modsExtentElement = wrapper
            .getElement("mods:relatedItem[@type='host']/mods:part/mods:extent[@unit='pages']");
        if (modsExtentElement != null) {
            processModsPartExtent(idb, modsExtentElement);
        }
    }

    private void processModsPartDetail(CSLItemDataBuilder idb, Element detailElement) {
        final String type = detailElement.getAttributeValue(ATTRIBUTE_TYPE);
        final Element num = detailElement.getChild("number", MODS_NAMESPACE);
        if (num != null) {
            Consumer<String> strFN = null;
            Consumer<Integer> intFn = null;
            switch (type) {
                case "issue" -> {
                    strFN = idb::issue;
                    intFn = idb::issue;
                }
                case "volume" -> {
                    strFN = idb::volume;
                    intFn = idb::volume;
                }
                case "article_number" -> {
                    intFn = idb::number;
                    strFN = idb::number;
                }
                default -> LOGGER.warn("Unknown type " + type + " in mods:detail in " + this.id);
            }
            try {
                if (intFn != null) {
                    intFn.accept(Integer.parseInt(num.getTextNormalize()));
                }
            } catch (NumberFormatException nfe) {
                strFN.accept(num.getTextNormalize());
            }
        }
    }

    private static void processModsPartExtent(CSLItemDataBuilder idb, Element modsExtentElement) {
        final String start = modsExtentElement.getChildTextNormalize("start", MODS_NAMESPACE);
        final String end = modsExtentElement.getChildTextNormalize("end", MODS_NAMESPACE);
        final String list = modsExtentElement.getChildTextNormalize("list", MODS_NAMESPACE);
        final String total = modsExtentElement.getChildTextNormalize("total", MODS_NAMESPACE);

        if (list != null) {
            idb.page(list);
        } else if (start != null && end != null && start.matches("\\d+") && end.matches("\\d+")) {
            idb.page(Integer.parseInt(start), Integer.parseInt(end));
        } else if (start != null && end != null) {
            idb.page(start + "-" + end);
        } else if (start != null && total != null) {
            idb.page(start);
            try {
                final int startI = Integer.parseInt(start);
                final int totalI = Integer.parseInt(total);
                idb.page(startI, (totalI - startI));
            } catch (NumberFormatException e) {
                idb.page(start);
            }
            idb.numberOfPages(total);
        } else if (start != null) {
            idb.page(start);
        } else if (end != null) {
            idb.page(end);
        }
    }

    protected void processGenre(CSLItemDataBuilder idb) {
        final List<Element> elements = wrapper.getElements("mods:genre");
        final Set<String> genres = getStrings(elements);
        final List<Element> parentElements = wrapper.getElements("mods:relatedItem[@type='host']/mods:genre");
        final Set<String> parentGenres = getStrings(parentElements);
        ModsGenre modsGenre = getModsGenre(genres);

        switch (modsGenre) {
            case ARTICLE -> idb.type(handleArticleType(parentGenres));
            case CONFERENCE -> idb.type(CSLType.PAPER_CONFERENCE);
            case BOOK -> idb.type(CSLType.BOOK);
            case INTERVIEW -> idb.type(CSLType.INTERVIEW);
            case RESEARCH_DATA -> idb.type(CSLType.DATASET);
            case PATENT -> idb.type(CSLType.PATENT);
            case CHAPTER -> idb.type(CSLType.CHAPTER);
            case ENTRY -> idb.type(CSLType.ENTRY_ENCYCLOPEDIA);
            case SPEECH -> idb.type(CSLType.SPEECH);
            case VIDEO -> idb.type(CSLType.MOTION_PICTURE);
            case BROADCASTING -> idb.type(CSLType.BROADCAST);
            case PICTURE -> idb.type(CSLType.GRAPHIC);
            case REVIEW -> idb.type(parentGenres.contains("book") ? CSLType.REVIEW_BOOK : CSLType.REVIEW);
            case THESIS -> idb.type(CSLType.THESIS);
            case REPORT -> idb.type(CSLType.REPORT);
            case null, default -> idb.type(CSLType.ARTICLE);
        }
    }

    private Set<String> getStrings(List<Element> parentElements) {
        return parentElements.stream()
            .map(this::getGenreStringFromElement)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    }

    private CSLType handleArticleType(Set<String> parentGenres) {
        if (parentGenres.contains("journal")) {
            return CSLType.ARTICLE_JOURNAL;
        } else if (parentGenres.contains("newspaper")) {
            return CSLType.ARTICLE_NEWSPAPER;
        } else {
            return CSLType.ARTICLE;
        }
    }

    private ModsGenre getModsGenre(Set<String> genres) {
        if (genres.contains("article") || genres.contains("review_article")) {
            return ModsGenre.ARTICLE;
        } else if (genres.contains("conference_essay") || genres.contains("abstract")) {
            return ModsGenre.CONFERENCE;
        } else if (genres.contains("book") || genres.contains("proceedings") || genres.contains("collection")
            || genres.contains("festschrift") || genres.contains("lexicon") || genres.contains("monograph")
            || genres.contains("lecture")) {
            return ModsGenre.BOOK;
        } else if (genres.contains("interview")) {
            return ModsGenre.INTERVIEW;
        } else if (genres.contains("research_data")) {
            return ModsGenre.RESEARCH_DATA;
        } else if (genres.contains("patent")) {
            return ModsGenre.PATENT;
        } else if (genres.contains("chapter") || genres.contains("contribution")) {
            return ModsGenre.CHAPTER;
        } else if (genres.contains("entry")) {
            return ModsGenre.ENTRY;
        } else if (genres.contains("preface")) {
            return ModsGenre.PREFACE;
        } else if (genres.contains("speech") || genres.contains("poster")) {
            return ModsGenre.SPEECH;
        } else if (genres.contains("video") || genres.contains("video_contribution")) {
            return ModsGenre.VIDEO;
        } else if (genres.contains("broadcasting")) {
            return ModsGenre.BROADCASTING;
        } else if (genres.contains("picture")) {
            return ModsGenre.PICTURE;
        } else if (genres.contains("review")) {
            return ModsGenre.RESEARCH_DATA;
        } else if (genres.contains("thesis") || genres.contains("exam") || genres.contains("dissertation")
            || genres.contains("habilitation") || genres.contains("diploma_thesis") || genres.contains("master_thesis")
            || genres.contains("bachelor_thesis") || genres.contains("student_research_project")
            || genres.contains("magister_thesis")) {
            return ModsGenre.THESIS;
        } else if (genres.contains("report") || genres.contains("research_results") || genres.contains("in_house")
            || genres.contains("press_release") || genres.contains("declaration") || genres.contains("researchpaper")) {
            return ModsGenre.REPORT;
        }
        return null;
    }

    protected String getGenreStringFromElement(Element genre) {
        if (genre.getAttributeValue("authorityURI") != null) {
            MCRCategoryID categoryID = MCRClassMapper.getCategoryID(genre);
            if (categoryID == null) {
                return null;
            }
            return categoryID.getId();
        } else {
            return genre.getText();
        }
    }

    protected void processAbstract(CSLItemDataBuilder idb) {
        Optional.ofNullable(wrapper.getElement("mods:abstract[not(@altFormat)]"))
            .map(Element::getTextNormalize)
            .ifPresent(idb::abstrct);
    }

    protected void processPublicationData(CSLItemDataBuilder idb) {
        Optional.ofNullable(wrapper.getElement(MODS_ORIGIN_INFO_PUBLICATION + "/mods:place/mods:placeTerm"))
            .or(() -> Optional.ofNullable(wrapper.getElement(MODS_RELATED_ITEM_XPATH +
                MODS_ORIGIN_INFO_PUBLICATION + "/mods:place/mods:placeTerm")))
            .ifPresent(el -> idb.publisherPlace(el.getTextNormalize()));

        Optional.ofNullable(wrapper.getElement(MODS_ORIGIN_INFO_PUBLICATION + "/mods:publisher"))
            .or(() -> Optional.ofNullable(wrapper.getElement(MODS_RELATED_ITEM_XPATH +
                MODS_ORIGIN_INFO_PUBLICATION + "/mods:publisher")))
            .ifPresent(el -> idb.publisher(el.getTextNormalize()));

        Optional.ofNullable(wrapper.getElement(MODS_ORIGIN_INFO_PUBLICATION + "/mods:edition"))
            .or(() -> Optional.ofNullable(wrapper.getElement(MODS_RELATED_ITEM_XPATH +
                MODS_ORIGIN_INFO_PUBLICATION + "/mods:edition")))
            .ifPresent(el -> idb.edition(el.getTextNormalize()));

        Optional.ofNullable(wrapper.getElement(MODS_ORIGIN_INFO_PUBLICATION + "/mods:dateIssued"))
            .or(() -> Optional.ofNullable(wrapper.getElement(MODS_RELATED_ITEM_XPATH +
                MODS_ORIGIN_INFO_PUBLICATION + "/mods:dateIssued")))
            .ifPresent(el -> idb.issued(new CSLDateBuilder().raw(el.getTextNormalize()).build()));
    }

    protected void processIdentifier(CSLItemDataBuilder idb) {
        final List<Element> parentIdentifiers = wrapper.getElements("mods:relatedItem[@type='host']/mods:identifier");

        parentIdentifiers.forEach(parentIdentifier -> applyIdentifier(idb, parentIdentifier, true));

        final List<Element> identifiers = wrapper.getElements("mods:identifier");
        identifiers.forEach(identifierElement -> applyIdentifier(idb, identifierElement, false));

    }

    private void applyIdentifier(CSLItemDataBuilder idb, Element identifierElement, boolean parent) {
        final String type = identifierElement.getAttributeValue(ATTRIBUTE_TYPE);
        final String identifier = identifierElement.getTextNormalize();

        if (type == null) {
            LOGGER.info("Type is null for identifier {}", identifier);
            return;
        }

        switch (type) {
            case "doi" -> idb.DOI(identifier);
            case "isbn" -> idb.ISBN(identifier);
            case "issn" -> idb.ISSN(identifier);
            case "pmid" -> {
                if (!parent) {
                    idb.PMID(identifier);
                }
            }
            case "pmcid" -> {
                if (!parent) {
                    idb.PMCID(identifier);
                }
            }
            default -> LOGGER.info("Unknown identifier type {}", identifier);

        }
    }

    protected void processTitles(CSLItemDataBuilder idb) {
        final Element titleInfoElement = wrapper.getElement(USABLE_TITLE_XPATH);
        if (titleInfoElement != null) {
            idb.titleShort(buildShortTitle(titleInfoElement));
            idb.title(buildTitle(titleInfoElement));
        }

        final Element titleInfoShortElement = wrapper.getElement(SHORT_TITLE_XPATH);
        if (titleInfoShortElement != null) {
            idb.titleShort(buildShortTitle(titleInfoShortElement));
        }

        Optional.ofNullable(wrapper.getElement("mods:relatedItem[@type='host']/" + USABLE_TITLE_XPATH))
            .ifPresent((titleInfo) -> {
                idb.containerTitleShort(buildShortTitle(titleInfo));
                idb.containerTitle(buildTitle(titleInfo));
            });

        Optional.ofNullable(wrapper.getElement("mods:relatedItem[@type='host']/" + SHORT_TITLE_XPATH))
            .ifPresent((titleInfo) -> idb.containerTitleShort(buildShortTitle(titleInfo)));

        wrapper.getElements(".//mods:relatedItem[@type='series' or (@type='host' and "
            + "substring-after(mods:genre[@type='intern']/@valueURI,'#') = 'series')]/" + USABLE_TITLE_XPATH).stream()
            .findFirst().ifPresent((relatedItem) -> idb.collectionTitle(buildTitle(relatedItem)));
    }

    protected void processNames(CSLItemDataBuilder idb) {
        final List<Element> modsNameElements = wrapper.getElements("mods:name");
        Map<String, List<CSLName>> roleNameMap = new HashMap<>();
        for (Element modsName : modsNameElements) {
            final CSLName cslName = buildName(modsName);
            if (isNameEmpty(cslName)) {
                continue;
            }
            fillRoleMap(roleNameMap, modsName, cslName);
        }

        mapRolesToCSLNames(idb, roleNameMap);

        Map<String, List<CSLName>> parentRoleMap = new HashMap<>();
        final List<Element> parentModsNameElements = wrapper.getElements("mods:relatedItem/mods:name");

        for (Element modsName : parentModsNameElements) {
            final CSLName cslName = buildName(modsName);
            if (isNameEmpty(cslName)) {
                continue;
            }
            fillRoleMap(parentRoleMap, modsName, cslName);
        }
        parentRoleMap.forEach((role, list) -> {
            final CSLName[] cslNames = list.toArray(list.toArray(new CSLName[0]));
            switch (role) {
                case "aut" -> idb.containerAuthor(cslNames);
                case "edt" -> idb.collectionEditor(cslNames);
                default -> {
                }
                // we dont care
            }
        });
    }

    private void mapRolesToCSLNames(CSLItemDataBuilder idb, Map<String, List<CSLName>> roleNameMap) {
        roleNameMap.forEach((role, list) -> {
            final CSLName[] cslNames = list.toArray(list.toArray(new CSLName[0]));
            switch (role) {
                case "aut", "inv" -> idb.author(cslNames);
                case "col" -> idb.collectionEditor(cslNames);
                case "edt" -> idb.editor(cslNames);
                case "fmd" -> idb.director(cslNames);
                case "ivr" -> idb.interviewer(cslNames);
                case "ive" -> idb.author(cslNames);
                case "ill" -> idb.illustrator(cslNames);
                case "trl" -> idb.translator(cslNames);
                case "cmp" -> idb.composer(cslNames);
                case "conference-name", "pup" -> idb
                    .event(Stream.of(cslNames).map(CSLName::getLiteral).collect(Collectors.joining(", ")));
                default -> {
                    if (KNOWN_UNMAPPED_PERSON_ROLES.contains(role)) {
                        LOGGER.trace(() -> "Unmapped person role " + role + " in " + this.id);
                    } else {
                        LOGGER.warn(() -> "Unknown person role " + role + " in " + this.id);
                    }
                }
            }
        });
    }

    private void fillRoleMap(Map<String, List<CSLName>> roleNameMap, Element modsName, CSLName cslName) {
        final Element roleElement = modsName.getChild("role", MODS_NAMESPACE);
        if (roleElement != null) {
            final List<Element> roleTerms = roleElement.getChildren("roleTerm", MODS_NAMESPACE);
            for (Element roleTermElement : roleTerms) {
                final String role = roleTermElement.getTextNormalize();
                roleNameMap.computeIfAbsent(role, s -> new ArrayList<>()).add(cslName);
            }
        } else {
            String nameType = modsName.getAttributeValue(ATTRIBUTE_TYPE);
            if (Objects.equals(nameType, "conference")) {
                roleNameMap.computeIfAbsent("conference-name", s -> new ArrayList<>()).add(cslName);
            }
        }
    }

    private CSLName buildName(Element modsName) {
        final CSLNameBuilder nameBuilder = new CSLNameBuilder();

        String nameType = modsName.getAttributeValue(ATTRIBUTE_TYPE);
        final boolean isInstitution = Objects.equals(nameType, "corporate") || Objects.equals(nameType,
            "conference");
        nameBuilder.isInstitution(isInstitution);

        if (!isInstitution) {
            Map<String, List<String>> typeContentsMap = new HashMap<>();
            modsName.getChildren("namePart", MODS_NAMESPACE).forEach(namePart -> {
                final String type = namePart.getAttributeValue(ATTRIBUTE_TYPE);
                final String content = namePart.getTextNormalize();

                if ((Objects.equals(type, "family") || Objects.equals(type, "given"))
                    && nonDroppingParticles.contains(content)) {
                    typeContentsMap.computeIfAbsent(NON_DROPPING_PARTICLE, t -> new ArrayList<>()).add(content);
                } else if ((Objects.equals(type, "family") || Objects.equals(type, "given"))
                    && droppingParticles.contains(content)) {
                    typeContentsMap.computeIfAbsent(DROPPING_PARTICLE, t -> new ArrayList<>()).add(content);
                } else {
                    typeContentsMap.computeIfAbsent(Optional.ofNullable(type).orElse(NONE_TYPE), t -> new ArrayList<>())
                        .add(content);
                }
            });

            typeContentsMap.forEach((key, value) -> {
                String joinedValue = String.join(" ", value);
                switch (key) {
                    case "family" -> nameBuilder.family(joinedValue);
                    case "given" -> nameBuilder.given(joinedValue);
                    case NON_DROPPING_PARTICLE -> nameBuilder.nonDroppingParticle(joinedValue);
                    case DROPPING_PARTICLE -> nameBuilder.droppingParticle(joinedValue);
                    case NONE_TYPE -> nameBuilder.literal(joinedValue);
                    default -> LOGGER.warn("Unexpected key '{}' encountered with value '{}'", key, joinedValue);
                }
            });

            Element displayForm = modsName.getChild("displayForm", MODS_NAMESPACE);
            if (typeContentsMap.isEmpty() && displayForm != null) {
                LOGGER.warn("The displayForm ({}) is used, because no mods name elements are present in doc {}!",
                    displayForm::getTextNormalize, () -> this.id);
                nameBuilder.literal(displayForm.getTextNormalize());
            }
        } else {
            String lit = Optional.ofNullable(modsName.getChildTextNormalize("displayForm", MODS_NAMESPACE))
                .orElse(modsName.getChildren("namePart", MODS_NAMESPACE).stream()
                    .map(Element::getTextNormalize)
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
        if (!subTitle.isEmpty()) {
            titleBuilder.append(": ").append(subTitle);
        }

        titleBuilder.append(Stream.of("partNumber", "partName")
            .flatMap(n -> getModsElementTextStream(titleInfoElement, n))
            .collect(Collectors.joining(",")));
        return titleBuilder.toString();
    }

    @Override
    public Collection<String> getIds() {
        return List.of(id);
    }

    @Override
    public void addContent(MCRContent content) throws IOException, JDOMException {
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
