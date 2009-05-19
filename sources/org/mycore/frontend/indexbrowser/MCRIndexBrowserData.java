package org.mycore.frontend.indexbrowser;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.log4j.Logger;
import org.jdom.Attribute;
import org.jdom.Content;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.filter.ElementFilter;
import org.jdom.output.XMLOutputter;
import org.jdom.xpath.XPath;
import org.mycore.common.MCRCache;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRException;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.datamodel.common.MCRXMLTableManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.parsers.bool.MCRAndCondition;
import org.mycore.parsers.bool.MCROrCondition;
import org.mycore.services.fieldquery.MCRCachedQueryData;
import org.mycore.services.fieldquery.MCRFieldDef;
import org.mycore.services.fieldquery.MCRFieldValue;
import org.mycore.services.fieldquery.MCRHit;
import org.mycore.services.fieldquery.MCRQuery;
import org.mycore.services.fieldquery.MCRQueryCondition;
import org.mycore.services.fieldquery.MCRQueryManager;
import org.mycore.services.fieldquery.MCRResults;
import org.mycore.services.fieldquery.MCRSortBy;

public class MCRIndexBrowserData {
    protected static Logger LOGGER = Logger.getLogger(MCRIndexBrowserData.class);

    private static final long serialVersionUID = 1L;

    private List<String[]> hitList = new LinkedList<String[]>();

    private MyBrowseData browseData = new MyBrowseData();

    private MyIndexConfiguration indexConfig = new MyIndexConfiguration();

    private XMLOutputter out = new XMLOutputter(org.jdom.output.Format.getPrettyFormat());

    private Element page;

    private MCRResults mcrResult;

    private MCRQuery myQuery;

    private static final String defaultlang = MCRConfiguration.instance().getString("MCR.Metadata.DefaultLang", "de");

    private static Hashtable<String, MCRCache> TYPE_CACHE_TABLE = new Hashtable<String, MCRCache>();

    private static ReentrantReadWriteLock TYPE_CACHE_TABLE_LOCK = new ReentrantReadWriteLock();

    private static MCRConfiguration MCR_CONFIG = MCRConfiguration.instance();

    static void deleteIndexCacheOfObjectType(String objectType) {
        if (objectType == null)
            return;
        TYPE_CACHE_TABLE_LOCK.writeLock().lock();
        TYPE_CACHE_TABLE.remove(objectType);
        TYPE_CACHE_TABLE_LOCK.writeLock().unlock();
    }

    private void initIndexCacheForObjectType(String alias) {
        String objectType = getObjectType(alias);
        if (objectType == null)
            throw new MCRException("Could not determine object type for alias: " + alias);
        try {
            TYPE_CACHE_TABLE_LOCK.writeLock().lock();
            if (!TYPE_CACHE_TABLE.containsKey(objectType))
                TYPE_CACHE_TABLE.put(objectType, new MCRCache(1000, "IndexBrowser,objectType=" + objectType.replace(",", "_")));
        } finally {
            TYPE_CACHE_TABLE_LOCK.writeLock().unlock();
        }
    }

    private static String getObjectType(String alias) {
        // get object type belonging to alias
        String propKey = "MCR.IndexBrowser." + alias + ".Table";
        String objectType = MCR_CONFIG.getProperties("MCR.IndexBrowser.").getProperty(propKey);
        return objectType;
    }

    private MCRCache getIndexCache(String alias) {
        String objectType = getObjectType(alias);
        try {
            TYPE_CACHE_TABLE_LOCK.readLock().lock();
            return TYPE_CACHE_TABLE.get(objectType);
        } finally {
            TYPE_CACHE_TABLE_LOCK.readLock().unlock();
        }
    }

    public MCRIndexBrowserData(String search, String mode, String path, String fromTo, String mask) {

        browseData.set(search, mode, path, fromTo, mask);
        indexConfig.set(browseData.index);
        Element results = buildPageElement();
        int numRows = 0;
        String cacheKey = browseData.index + "#" + browseData.search + "#" + mode;

        if (cached(path, cacheKey)) {
            CacheEntry entry = ((CacheEntry) getIndexCache(path).get(cacheKey));
            hitList = entry.list;
            numRows = hitList.size();
            myQuery = entry.query;
        } else {
            initIndexCacheForObjectType(path);
            CacheEntry entry = new CacheEntry();
            numRows = createLinkedListfromSearch();
            entry.list = hitList;
            entry.query = myQuery;
            getIndexCache(path).put(cacheKey, entry);
            // for further search and research (by refine and other posibilities
            // the query must be in the Cache
            MCRCachedQueryData.cache(mcrResult, myQuery.buildXML(), myQuery.getCondition());
            results.setAttribute("resultid", mcrResult.getID());
        }
        int from = Math.max(0, browseData.from);
        int to = Math.min(numRows, browseData.to + 1);
        int numSelectedRows = to - from;

        results.setAttribute("numHits", String.valueOf(numRows));
        if (browseData.search != null) {
            results.setAttribute("search", browseData.search);
        }

        if (numSelectedRows <= indexConfig.maxPerPage) {
            // set the metadata for the details only for the shown objects
            fillHitListWithMetadata(from, to);

            // build the xml-result tree with detailed Informations
            for (int i = from; i < to; i++) {
                String myValues[] = hitList.get(i);

                Element v = new Element("value");
                v.setAttribute("pos", String.valueOf(i));

                // the first field from the lll-element must be the first
                // outputField
                v.addContent(new Element("sort").addContent(myValues[0]));

                // the second field from the lll-element must be se second
                // element in outputfields
                v.addContent(new Element("idx").addContent(myValues[1]));

                // the thirt field from the lll-element is the objectid
                v.addContent(new Element("id").addContent(myValues[2]));

                for (int j = 0; indexConfig.outputFields != null && j < indexConfig.outputFields.length; j++) {
                    String sField = indexConfig.outputFields[j];
                    Element col = new Element("col");
                    col.setAttribute("name", sField);
                    col.addContent(myValues[3 + j]);
                    v.addContent(col);
                }
                results.addContent(v);
            }
        } else {
            // to much results to show all details
            // build the xml-result tree with the range of the searchresults
            int stepSize = calculateStepSize(numSelectedRows, indexConfig.maxPerPage);
            List<MyRangeDelim> delims = new Vector<MyRangeDelim>();

            for (int i = from; i < to; i++) {
                String myValues[] = hitList.get(i);
                delims.add(new MyRangeDelim(i, myValues[1]));

                i = Math.min(i + stepSize, to - 1);

                String myVal[] = hitList.get(i);
                String value = myVal[1];

                if ((i < to) && ((to - i) < 3)) {
                    i = to - 1;
                    value = hitList.get(i)[1];
                }
                delims.add(new MyRangeDelim(i, value));
            }
            buildPrefixDifference(delims);
            buildXML(results, delims);
        }
    }

    public MCRResults getResultList() {
        return mcrResult;
    }

    public Document getQuery() {
        Document jQuery = myQuery.buildXML();
        if (browseData.mask != null) {
            jQuery.getRootElement().setAttribute("mask", browseData.mask);
        }
        return jQuery;
    }

    public Document getXMLContent() {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Results: \n" + out.outputString(page));
        }
        return new Document(page);
    }

    private Element buildPageElement() {
        // Build output index page
        page = new Element("indexpage");
        page.setAttribute("path", browseData.path.toString());

        Element eIndex = new Element("index");
        page.addContent(eIndex);

        eIndex.setAttribute("id", browseData.index);
        Element results = new Element("results");
        page.addContent(results);
        results.setAttribute("mode", browseData.mode);
        return results;
    }

    private void buildPrefixDifference(List<MyRangeDelim> delims) {
        for (int i = 0; i < delims.size(); i++) {
            MyRangeDelim curr = (delims.get(i));
            MyRangeDelim prev = (delims.get(Math.max(0, i - 1)));
            MyRangeDelim next = (delims.get(Math.min(i + 1, delims.size() - 1)));

            String vCurr = curr.value;
            String vPrev = (i > 0 ? prev.value : "");
            String vNext = (i < delims.size() - 1 ? next.value : "");

            String a = buildPrefixDifference(vCurr, vPrev);
            String b = buildPrefixDifference(vCurr, vNext);
            curr.diff = (a.length() > b.length() ? a : b);
        }
    }

    private void buildXML(Element results, List<MyRangeDelim> delims) {
        for (int i = 0; i < delims.size(); i += 2) {
            MyRangeDelim start = (delims.get(i));
            MyRangeDelim end = (delims.get(i + 1));

            Element range = new Element("range");
            results.addContent(range);

            Element eFrom = new Element("from");
            eFrom.setAttribute("pos", String.valueOf(start.pos));
            eFrom.setAttribute("short", start.diff);
            eFrom.addContent(start.value);
            range.addContent(eFrom);

            Element eTo = new Element("to");
            eTo.setAttribute("pos", String.valueOf(end.pos));
            eTo.setAttribute("short", end.diff);
            eTo.addContent(end.value);
            range.addContent(eTo);
        }
    }

    private String buildPrefixDifference(String a, String b) {
        if (a.equals(b))
            return a;

        StringBuffer pdiff = new StringBuffer();

        for (int i = 0; i < Math.min(a.length(), b.length()); i++) {
            pdiff.append(a.charAt(i));
            if (a.charAt(i) != b.charAt(i))
                break;
        }

        if ((a.length() > b.length()) && (b.equals(pdiff.toString())))
            pdiff.append(a.charAt(pdiff.length()));

        return pdiff.toString();
    }

    private MCRQuery buildQuery() {
        MCRAndCondition cAnd = new MCRAndCondition();

        MCRFieldDef fieldproject;
        MCRFieldDef fieldtype;
        if (indexConfig.table.indexOf(",") != -1) {
            MCROrCondition cOr = new MCROrCondition();
            StringTokenizer st = new StringTokenizer(indexConfig.table, ",");
            while (st.hasMoreTokens()) {
                String next = st.nextToken();
                int ilen = next.indexOf("_");
                if (ilen == -1) {
                    fieldtype = MCRFieldDef.getDef("objectType");
                    cOr.addChild(new MCRQueryCondition(fieldtype, "=", next));
                } else {
                    MCRAndCondition iAnd = new MCRAndCondition();
                    fieldtype = MCRFieldDef.getDef("objectType");
                    iAnd.addChild(new MCRQueryCondition(fieldtype, "=", next.substring(ilen + 1, next.length())));
                    fieldproject = MCRFieldDef.getDef("objectProject");
                    iAnd.addChild(new MCRQueryCondition(fieldproject, "=", next.substring(0, ilen)));
                    cOr.addChild(iAnd);
                }
            }
            cAnd.addChild(cOr);
        } else {
            int ilen = indexConfig.table.indexOf("_");
            if (ilen == -1) {
                fieldtype = MCRFieldDef.getDef("objectType");
                cAnd.addChild(new MCRQueryCondition(fieldtype, "=", indexConfig.table));
            } else {
                fieldtype = MCRFieldDef.getDef("objectType");
                cAnd.addChild(new MCRQueryCondition(fieldtype, "=", indexConfig.table.substring(ilen + 1, indexConfig.table.length())));
                fieldproject = MCRFieldDef.getDef("objectProject");
                cAnd.addChild(new MCRQueryCondition(fieldproject, "=", indexConfig.table.substring(0, ilen)));
            }
        }
        if (browseData.search != null && browseData.search.length() > 0) {
            MCRFieldDef field = MCRFieldDef.getDef(indexConfig.browseField);
            String value = browseData.search == null ? "*" : browseData.search;
            String operator = getOperator();

            cAnd.addChild(new MCRQueryCondition(field, operator, value));
        }

        boolean order = "ascending".equalsIgnoreCase(indexConfig.order);
        List<MCRSortBy> sortCriteria = new ArrayList<MCRSortBy>();

        for (int i = 0; indexConfig.sortFields != null && i < indexConfig.sortFields.length; i++) {
            MCRFieldDef field = MCRFieldDef.getDef(indexConfig.sortFields[i]);
            if (null != field)
                sortCriteria.add(new MCRSortBy(field, order));
            else
                LOGGER.error("MCRFieldDef not available: " + indexConfig.sortFields[i]);
        }

        myQuery = new MCRQuery(cAnd, sortCriteria, 0);
        LOGGER.debug("Query: \n" + out.outputString(myQuery.buildXML()));
        return myQuery;
    }

    /**
     * Returns The lucene search operator as String to be used doing a lucene
     * query. This will be taken from MyBrowseData.mode; If MyBrowseData.mode ==
     * "prefix" -> return "like", If MyBrowseData.mode == "equals" -> return
     * "=", Else return "like"
     * 
     * @return The lucene search operator as String
     * 
     */
    private String getOperator() {
        if (browseData != null && browseData.mode != null && browseData.mode.equalsIgnoreCase("equals"))
            return "=";
        else if (browseData != null && browseData.mode != null && browseData.mode.equalsIgnoreCase("prefix"))
            return "like";
        else
            return "like";
    }

    private int calculateStepSize(int numSelectedRows, int maxPerPage) {
        for (int i = 1;; i++) {
            double dNum = numSelectedRows;
            double dI = 1.0 / ((double) i);
            double root = Math.pow(dNum, dI);
            if (root <= maxPerPage)
                return (int) (Math.floor(dNum / root));
        }
    }

    private int createLinkedListfromSearch() {
        buildQuery();
        // at first we must create the full list with all results
        hitList = new LinkedList<String[]>();
        mcrResult = MCRQueryManager.search(myQuery);
        LOGGER.debug("Results found hits:" + mcrResult.getNumHits());

        int len = (indexConfig.outputFields != null) ? indexConfig.outputFields.length + 3 : 3;

        for (MCRHit hit : mcrResult) {
            String[] listelm = new String[len + 3];
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("\n" + out.outputString(hit.buildXML()));
            }
            List<MCRFieldValue> sortData = hit.getSortData();
            MCRFieldDef mainSortField = myQuery.getSortBy().get(0).getField();
            if (sortData.size() == 0 || !sortData.get(0).getField().equals(mainSortField)) {
                //main sortfield has no value for this hit
                MCRFieldValue value = new MCRFieldValue(mainSortField, "???undefined???");
                sortData.add(0, value);
            }
            //TODO this is kind of ugly
            listelm[0] = sortData.get(0).getValue();
            if (sortData.size() > 1) {
                listelm[1] = sortData.get(1).getValue();
            } else {
                listelm[1] = listelm[0];
            }
            listelm[2] = hit.getID();
            hitList.add(listelm);
        }
        return hitList.size();
    }

    private int fillHitListWithMetadata(int from, int to) {
        if (from == 0)
            from = 1;
        // now we take only the objects from metadata for the dataobjects that
        // we will shown in detail
        // this occurres if we have only a small count of results, or we show
        // only a part
        if (to - from <= indexConfig.maxPerPage && indexConfig.outputFields != null) {
            for (int i = from - 1; i < to; i++) {
                String[] listelm = hitList.get(i);
                String id = listelm[2];
                Document jdomDoc = MCRXMLTableManager.instance().readDocument(new MCRObjectID(id));
                // outputfields came only from metadataobject
                setListeElm(jdomDoc, indexConfig.outputFields, 3, listelm, false);
            }
        }
        return hitList.size();
    }

    // reads the values from the metadata of the document
    private void setListeElm(Document od, String[] Fields, int startindex, String[] listelm, boolean append) {
        if (Fields == null)
            return;
        MCRSession mcrSession = MCRSessionMgr.getCurrentSession();
        String currentlang = mcrSession.getCurrentLanguage();
        for (int j = 0; j < Fields.length; j++) {
            String value = "";
            String attribute = "";
            String sField = Fields[j];

            // Eintrag der ID
            if (sField.equalsIgnoreCase("id")) {
                value = od.getRootElement().getAttributeValue("ID");

            } else {
                if (sField.contains("/")) {
                    // evaluate an XPath-Expression
                    try {
                        XPath xpath = XPath.newInstance(sField);
                        List<?> xpathResults = xpath.selectNodes(od);
                        value = "";
                        for (int i = 0; i < xpathResults.size(); i++) {
                            if (i > 0) {
                                value += " - ";
                            }
                            if (xpathResults.get(i) instanceof Attribute) {
                                value += ((Attribute) xpathResults.get(i)).getValue();
                            } else {
                                value += ((Content) xpathResults.get(i)).getValue();
                            }
                        }

                    } catch (JDOMException jde) {
                        value = "";
                    }
                } else {
                    // get for current lang
                    Iterator<?> it = od.getDescendants(new ElementFilter(sField));
                    // only the atribute different texts are taken!!
                    int counter = 0;
                    boolean hasdefault = false;
                    while (it.hasNext()) {
                        Element el = (Element) it.next();
                        String lang = el.getAttributeValue("lang", org.jdom.Namespace.XML_NAMESPACE);
                        if ((lang != null) && (lang.equals(currentlang))) {
                            if (attribute != el.getAttributeValue("type")) {
                                if (value.length() > 0) {
                                    value += " - ";
                                }
                                value += el.getText();
                                attribute = el.getAttributeValue("type");
                            }
                            counter++;
                        }
                        if ((lang != null) && (lang.equals(currentlang))) {
                            hasdefault = true;
                        }
                    }
                    if (counter == 0) {
                        // get for current lang
                        it = od.getDescendants(new ElementFilter(sField));
                        // only the atribute different texts are taken!!
                        if (hasdefault) {
                            while (it.hasNext()) {
                                Element el = (Element) it.next();
                                String lang = el.getAttributeValue("lang", org.jdom.Namespace.XML_NAMESPACE);
                                if ((lang != null) && (lang.equals(defaultlang))) {
                                    if (attribute != el.getAttributeValue("type")) {
                                        if (value.length() > 0) {
                                            value += " - ";
                                        }
                                        value += el.getText();
                                        attribute = el.getAttributeValue("type");
                                    }
                                    break;
                                }
                            }
                        } else {
                            while (it.hasNext()) {
                                Element el = (Element) it.next();
                                if (attribute != el.getAttributeValue("type")) {
                                    if (value.length() > 0) {
                                        value += " - ";
                                    }
                                    value += el.getText();
                                    attribute = el.getAttributeValue("type");
                                }
                                break;
                            }
                        }
                    }

                }
            }
            if (append) {
                if (j == 0)
                    listelm[startindex] = value;
                else
                    listelm[startindex] += ", " + value;
            } else
                listelm[startindex + j] = value;
        }

    }

    private boolean cached(String alias, String key) {
        String objType = getObjectType(alias);
        try {
            TYPE_CACHE_TABLE_LOCK.readLock().lock();
            if (TYPE_CACHE_TABLE.get(objType) != null && TYPE_CACHE_TABLE.get(objType).get(key) != null)
                return true;
            return false;
        } finally {
            TYPE_CACHE_TABLE_LOCK.readLock().unlock();
        }
    }

    /*
     * indexCache is build like this:
     * 
     * --+ indexCache +---+ MCRObjectType +---+ Cache entries for configured
     * aliases
     */

    private static class MyRangeDelim {
        int pos;

        String value;

        String diff;

        MyRangeDelim(int pos, String value) {
            this.pos = pos;
            this.value = value;
        }
    }

    private static class MyBrowseData {
        String index;

        int from = 0;

        int to = Integer.MAX_VALUE - 10;

        StringBuffer path;

        String search;

        String mode;

        String mask;

        public void set(String search, String mode, String index, String fromTo, String mask) {
            this.search = search;
            this.mode = mode;
            this.index = index;
            this.path = new StringBuffer(this.index);
            this.path.append("/");
            if (fromTo != null && fromTo.length() > 0) {
                String from = fromTo.substring(0, fromTo.indexOf("-"));
                String to = fromTo.substring(fromTo.indexOf("-") + 1);
                this.addRange(from, to);
            }
            this.mask = mask;
        }

        void addRange(String from, String to) {
            this.from = Integer.parseInt(from);
            this.to = Integer.parseInt(to);
            path.append(this.from);
            path.append("-");
            path.append(this.to);
            path.append("/");
        }

    }

    private static class MyIndexConfiguration {
        String table;

        boolean distinct;

        String browseField;

        String fields;

        String[] outputFields;

        String fieldToSort;

        String[] sortFields;

        String order;

        String filter;

        String style;

        int maxPerPage;

        public void set(String ID) {

            MCRConfiguration config = MCRConfiguration.instance();
            String prefix = "MCR.IndexBrowser." + ID + ".";
            table = config.getString(prefix + "Table");
            distinct = config.getBoolean(prefix + "Distinct", true);
            browseField = config.getString(prefix + "Searchfield");
            filter = config.getString(prefix + "FilterCondition", null);
            maxPerPage = config.getInt(prefix + "MaxPerPage");
            style = config.getString(prefix + "Style");
            fields = config.getString(prefix + "ExtraOutputFields", null);
            fieldToSort = config.getString(prefix + "FieldsToSort", null);
            order = config.getString(prefix + "Order", "ascending");
            outputFields = buildFieldList(fields);
            sortFields = buildFieldList(fieldToSort);
        }

        private String[] buildFieldList(String myfields) {
            String[] ff;
            if ((myfields == null) || (myfields.trim().length() == 0)) {
                ff = null;
            } else {
                StringTokenizer st = new StringTokenizer(myfields, ",");
                ff = new String[st.countTokens()];
                for (int i = 0; i < ff.length; i++)
                    ff[i] = st.nextToken();
            }
            return ff;
        }
    }

    private static class CacheEntry {
        List<String[]> list;

        MCRQuery query;
    }
}
