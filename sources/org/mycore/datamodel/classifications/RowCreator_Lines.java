/**
 * 
 */
package org.mycore.datamodel.classifications;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.xpath.XPath;
import org.mycore.common.MCRConfiguration;
import org.mycore.datamodel.classifications2.MCRCategLinkService;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryID;

class RowCreator_Lines implements RowCreator {
    private final Logger LOGGER = Logger.getLogger(RowCreator_Lines.class);
    private LinkedList<Element> lines;
    private MCRClassificationPool classificationPool;
    private MCRCategLinkService linkService;
    private MCRCategory classif;
    private int maxlevel = 0;
    private MCRConfiguration configuration;
    private boolean commented = false;
    private String browserClass;
    private String uri;
    private String view = null;
    private String emptyLeafs;
    
    public RowCreator_Lines(MCRClassificationPool classificationPool, MCRCategLinkService linkService, MCRConfiguration configuration) {
        this.classificationPool = classificationPool;
        this.linkService = linkService;
        this.configuration = configuration;
        String defaultEmptyLeafs = configuration.getString("MCR.ClassificationBrowser.default.EmptyLeafs");
        emptyLeafs = configuration.getString("MCR.ClassificationBrowser." + getBrowserClass() + ".EmptyLeafs", defaultEmptyLeafs);
    }
    
    @Override
    public void createRows(String lang, Element xNavtree) {
        try {
            MCRCategory classification = classificationPool.getClassificationAsPojo(classif.getId(), true);
            String rootID = classification.getId().getRootID();

            Map<MCRCategoryID, Boolean> linkMap = linkService.hasLinks(classification);
            /* the line element has following form
             *  <category ID="child_0" level="2" hasChildren=" ">
             *        <label xml:lang="de" text="text_de" description="descr_de" />
             *        <label xml:lang="en" text="text_en" description="descr_en" />
             *  </category>
             *  
             *  Attributes
             *  level: MCRCategory.getLevel()
             *  hasChildren: "T" = MCRCategory.hasChildren() otherwise " "
             */
            for (Element line : lines) {
                String catid = line.getAttributeValue("ID");
//                boolean hasLinks = linkService.hasLink(new MCRCategoryID(rootID, catid));
                boolean hasLinks = linkMap.get(new MCRCategoryID(rootID, catid));

                
                if (emptyLeafs.endsWith("no") && !hasLinks) {
                    LOGGER.debug(" empty Leaf continue - " + emptyLeafs);
                    continue;
                }

                Element xRow = new Element("row");
                xRow.addContent(createColumn1(line, catid, hasLinks, lines.indexOf(line)));
                xRow.addContent(createColumn2(lang, line, catid, hasLinks));
                xNavtree.addContent(xRow);
            }
        } catch (JDOMException e) {
            e.printStackTrace();
        }
        
        xNavtree.setAttribute("rowcount", "" + lines.size());
    }

    private Element createColumn1(Element line, String catid, boolean hasLinks, int pos) {
        Element xCol1 = new Element("col");
        int level = Integer.parseInt(line.getAttributeValue("level"));
        xCol1.setAttribute("lineLevel", String.valueOf(level));
        xCol1.setAttribute("childpos", getRowPosition(level, pos));

        xCol1.setAttribute("plusminusbase", catid);

        String status = line.getAttributeValue("hasChildren");
        String folder1_Sign = "folder_plain";
        if (status.equals("T")) {
            folder1_Sign = "folder_plus";
        } else if (status.equals("F")) {
            folder1_Sign = "folder_minus";
        }
        xCol1.setAttribute("folder1", folder1_Sign);
        xCol1.setAttribute("folder2", hasLinks ? "folder_closed_in_use" : "folder_closed_empty");
        return xCol1;
    }
    
    private Element createColumn2(String lang, Element line, String catid, boolean hasLinks) throws JDOMException {
        Element xCol2 = new Element("col");
        Element labelElement = createLabelForLang(lang, line);
        String text = labelElement.getAttributeValue("text");
        String description = labelElement.getAttributeValue("description");
        xCol2.setAttribute("searchbase", createSearchUri(catid));
        xCol2.setAttribute("lineID", catid);
        xCol2.setAttribute("hasLinks", String.valueOf(hasLinks));

        xCol2.addContent(text);

        if (isCommented() && (description != null)) {
            final Element comment = new Element("comment");
            xCol2.addContent(comment);
            comment.setText(description);
        }
        return xCol2;
    }
    
    private String getRowPosition(int level, int pos) {
        String position = "middle";
        Element treeline = getTreeline(pos+1);
        if (level > maxlevel) {
            position = "first";
            maxlevel = level;
            if (treeline == null) {
                // Spezialfall nur genau ein Element
                position = "firstlast";
            }
        } else if (treeline == null) {
            position = "last";
        }

        return position;
    }
    
    private void putCategoriesintoLines(final int startpos, final List<MCRCategory> children) {
        LOGGER.debug("Start Explore Arraylist of CategItems  ");
        int i = startpos;

        for (MCRCategory cat : children) {
            lines.add(++i, setTreeline(cat));
        }

        LOGGER.debug("End Explore - Arraylist of CategItems ");
    }
    
    public void setClassification(final MCRCategoryID classifID) {
        classif = classificationPool.getClassificationAsPojo(classifID, true);
        LOGGER.info("classif: " + classif);
        if (classif == null)
            return;
        lines = new LinkedList<Element>();
        putCategoriesintoLines(-1, classif.getChildren());
        LOGGER.debug("Arraylist of CategItems initialized - Size " + lines.size());
    }
    
    public void update(final String categID) throws Exception {
        int lastLevel = 0;
        boolean hideLevel = false;

        MCRCategory parent = classificationPool.getClassificationAsPojo(classif.getId(), true);
        MCRCategory cat = MCRCategoryTools.findCategory(parent, categID);

        Element line;
        for (int i = 0; i < lines.size(); i++) {
            line = getTreeline(i);
            final String catid = line.getAttributeValue("ID");
            final String status = line.getAttributeValue("hasChildren");
            final int level = Integer.parseInt(line.getAttributeValue("level"));

            hideLevel = hideLevel && (level > lastLevel);
            LOGGER.debug(" compare CategoryTree on " + i + "_" + catid + " to " + categID);
            if (getView().endsWith("tree")) {
                if (hideLevel) {
                    lines.remove(i--);
                } else if (categID.equals(catid)) {
                    if (status.equals("F")) // hide expanded category - //
                    // children
                    {
                        line.setAttribute("hasChildren", "T");
                        hideLevel = true;
                        lastLevel = level;
                    } else if (status.equals("T")) // expand category - //
                    // children
                    {
                        line.setAttribute("hasChildren", "F");
                        putCategoriesintoLines(i, cat.getChildren());
                    }
                }
            } else {
                if (categID.equalsIgnoreCase(catid)) {
                    line.setAttribute("level", "0");
                    line.setAttribute("hasChildren", "F");
                    putCategoriesintoLines(i, cat.getChildren());
                } else {
                    LOGGER.debug(" remove lines " + i + "_" + catid);
                    lines.remove(i--);
                }
            }

        }
    }

    private Element setTreeline(MCRCategory categ) {
        Element categElement = MCRCategoryTools.getCategoryElement(categ, false, 0);
        categElement.setAttribute("level", String.valueOf(categ.getLevel()));

        if (categ.hasChildren()) {
            categElement.setAttribute("hasChildren", "T");
        } else {
            categElement.setAttribute("hasChildren", " ");
        }
        return categElement;
    }
    
    private String createSearchUri(String catid) {
        String search = getUri();
        search += "/" + catid;

        if (search.indexOf("//") > 0)
            search = search.substring(0, search.indexOf("//")) + search.substring(search.indexOf("//") + 1);
        return search;
    }
    
    private Element createLabelForLang(String lang, Element line) throws JDOMException {
        Element label = (Element) XPath.selectSingleNode(line, "label[lang('" + lang + "')]");
        if (label == null) {
            label = (Element) XPath.selectSingleNode(line, "label[lang('"
                    + MCRConfiguration.instance().getString("MCR.Metadata.DefaultLang", "en") + "')]");
        }

        if (label == null) {
            label = (Element) XPath.selectSingleNode(line, "label");
        }
        return label;
    }
    
    private Element getTreeline(final int i) {
        if (i >= lines.size()) {
            return null;
        }
        return lines.get(i);
    }

    @Override
    public MCRCategory getClassification() {
        return classif;
    }

    public void setCommented(boolean commented) {
        this.commented = commented;
    }

    private boolean isCommented() {
        return commented;
    }

    public void setBrowserClass(String browserClass) {
        this.browserClass = browserClass;
    }

    private String getBrowserClass() {
        return browserClass;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    private String getUri() {
        return uri;
    }

    public void setView(String view) {
        this.view = view;
    }

    private String getView() {
        return view;
    }
}