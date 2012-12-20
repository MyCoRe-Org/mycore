/**
 * 
 */
package org.mycore.datamodel.classifications;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.time.StopWatch;
import org.apache.log4j.Logger;
import org.jdom.Element;
import org.mycore.common.MCRConfiguration;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.classifications2.MCRLabel;

class RowCreator_NoLines implements RowCreator {
    int maxlevel = 0;

    private boolean commented = false;

    private MCRCategory classif;

    private MCRClassificationPool classificationPool;

    private String emptyLeafs;

    private String browserClass;

    private String uri;

    private String view = null;

    private Map<MCRCategoryID, String> folderStatus = new HashMap<MCRCategoryID, String>();

    private final Logger LOGGER = Logger.getLogger(RowCreator_NoLines.class);

    public RowCreator_NoLines() {
        this(MCRClassificationPoolFactory.getInstance(), MCRConfiguration.instance());
    }

    public RowCreator_NoLines(MCRClassificationPool classificationPool, MCRConfiguration configuration) {
        this.classificationPool = classificationPool;
        MCRConfiguration configuration1 = configuration;
        String defaultEmptyLeafs = configuration.getString("MCR.ClassificationBrowser.default.EmptyLeafs");
        emptyLeafs = configuration.getString("MCR.ClassificationBrowser." + getBrowserClass() + ".EmptyLeafs", defaultEmptyLeafs);
    }

    private String getBrowserClass() {
        return browserClass;
    }

    /* (non-Javadoc)
     * @see org.mycore.datamodel.classifications.RowCreator#createRows(java.lang.String, org.jdom.Element)
     */
    public void createRows(String lang, Element xNavtree) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        Map<MCRCategoryID, Boolean> linkMap = classificationPool.hasLinks(classif);
        stopWatch.stop();
        LOGGER.info("haslinks time for " + classif.getId() + ": " + stopWatch.getTime());
        createRowsForCategory(lang, xNavtree, classif, linkMap);
    }

    private void createRowsForCategory(String lang, Element xNavtree, MCRCategory category, Map<MCRCategoryID, Boolean> linkMap) {
        List<MCRCategory> children = category.getChildren();
        for (MCRCategory mcrCategory : children) {
            MCRCategoryID categoryID = mcrCategory.getId();
            Boolean hasLinksValue = linkMap.get(categoryID);
            boolean hasLinks = hasLinksValue == null ? false : hasLinksValue;
            boolean hasChildren = mcrCategory.hasChildren();

            if (emptyLeafs.endsWith("no") && !hasLinks) {
                LOGGER.debug(" empty Leaf continue - " + emptyLeafs);
                continue;
            }

            Element row = new Element("row");
            String position = getRowPosition(mcrCategory, children);
            row.addContent(createColumn1(mcrCategory, hasLinks, position, hasChildren));
            row.addContent(createColumn2(lang, mcrCategory, hasLinks));
            xNavtree.addContent(row);

            if (hasChildren) {
                if ("F".equals(folderStatus.get(categoryID))) {
                    createRowsForCategory(lang, xNavtree, mcrCategory, linkMap);
                } else {
                    folderStatus.put(categoryID, "T");
                }
            } else {
                folderStatus.put(categoryID, " ");
            }
        }

        xNavtree.setAttribute("rowcount", "" + xNavtree.getChildren("row").size());
    }

    private Element createColumn1(MCRCategory mcrCategory, boolean hasLinks, String position, boolean hasChildren) {
        Element xCol1 = new Element("col");
        int level = mcrCategory.getLevel();
        xCol1.setAttribute("lineLevel", String.valueOf(level));
        xCol1.setAttribute("childpos", position);

        MCRCategoryID categoryID = mcrCategory.getId();

        if (hasChildren) {
            xCol1.setAttribute("plusminusbase", categoryID.getID());
        }
        String status = folderStatus.get(categoryID);
        if (status == null) {
            status = hasChildren ? "T" : " ";
            folderStatus.put(categoryID, status);
        }

        String folder1_Sign = "folder_plain";
        if ("T".equals(status) || status == null) {
            folder1_Sign = "folder_plus";
        } else if ("F".equals(status)) {
            folder1_Sign = "folder_minus";
        }
        xCol1.setAttribute("folder1", folder1_Sign);
        xCol1.setAttribute("folder2", hasLinks ? "folder_closed_in_use" : "folder_closed_empty");
        return xCol1;
    }

    private String getRowPosition(MCRCategory mcrCategory, List<MCRCategory> children) {
        String position = "middle";
        int childrenSize = children.size();
        int indexOf = children.indexOf(mcrCategory);
        if (indexOf == 0) {
            position = childrenSize == 1 ? "firstlast" : "first";
        } else if (indexOf == (childrenSize - 1)) {
            position = "last";
        }

        return position;
    }

    private Element createColumn2(String lang, MCRCategory mcrCategory, boolean hasLinks) {
        Element xCol2 = new Element("col");
        MCRLabel label = mcrCategory.getLabel(lang);
        if (label == null) {
            label = mcrCategory.getCurrentLabel();
        }

        String catid = mcrCategory.getId().getID();

        xCol2.setAttribute("searchbase", createSearchUri(catid));
        xCol2.setAttribute("lineID", catid);
        xCol2.setAttribute("hasLinks", String.valueOf(hasLinks));

        xCol2.addContent(label.getText());

        String description = label.getDescription();
        if (isCommented() && (description != null)) {
            final Element comment = new Element("comment");
            xCol2.addContent(comment);
            comment.setText(description);
        }
        return xCol2;
    }

    public void setClassification(MCRCategoryID classifID) {
        classif = classificationPool.getClassificationAsPojo(classifID, true);
        if (classif == null)
            return;
    }

    public void update(final String categID) throws Exception {

        MCRCategory parent = classificationPool.getClassificationAsPojo(classif.getId(), true);
        MCRCategory cat = MCRCategoryTools.findCategory(parent, categID);
        if (cat == null) {
            return;
        }

        MCRCategoryID categoryID = cat.getId();
        String status = folderStatus.get(categoryID);
        if (status == null) {
            status = cat.hasChildren() ? "T" : " ";
            folderStatus.put(categoryID, status);
        }

        if (view.endsWith("tree")) {
            if ("F".equals(status)) // hide expanded category - //
            // children
            {
                folderStatus.put(categoryID, "T");
            } else if ("T".equals(status)) // expand category - //
            // children
            {
                folderStatus.put(categoryID, "F");
                //                        putCategoriesintoLines(cat.getChildren());
            }
        } else {
            folderStatus.put(categoryID, "F");
        }
    }

    private String createSearchUri(String catid) {
        String search = uri;
        search += "/" + catid;

        if (search.indexOf("//") > 0)
            search = search.substring(0, search.indexOf("//")) + search.substring(search.indexOf("//") + 1);
        return search;
    }

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

    public void setUri(String uri) {
        this.uri = uri;
    }

    public void setView(String view) {
        this.view = view;
    }
}