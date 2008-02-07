package org.mycore.frontend.indexbrowser;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;

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
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.parsers.bool.MCRAndCondition;
import org.mycore.parsers.bool.MCROrCondition;
import org.mycore.services.fieldquery.MCRCachedQueryData;
import org.mycore.services.fieldquery.MCRFieldDef;
import org.mycore.services.fieldquery.MCRQuery;
import org.mycore.services.fieldquery.MCRQueryCondition;
import org.mycore.services.fieldquery.MCRQueryManager;
import org.mycore.services.fieldquery.MCRResults;
import org.mycore.services.fieldquery.MCRSortBy;

public class MCRIndexBrowserData {
    protected static Logger logger = Logger.getLogger(MCRIndexBrowserData.class);

    protected static final String PREFIX = "MCRIndexBrowserData.";

    protected static final String INDEX_KEY = PREFIX + "list";

    protected static final String QUERY_KEY = PREFIX + "query";

    private static final long serialVersionUID = 1L;

    private LinkedList<String[]> linkedList1 = new LinkedList<String[]>();

    private MyBrowseData browseData = new MyBrowseData();

    private MyIndexConfiguration indexConfig = new MyIndexConfiguration();

    private XMLOutputter out = new XMLOutputter(org.jdom.output.Format.getPrettyFormat());

    private Element page;

    private MCRResults mcrResult;

    private MCRQuery myQuery;

    private static final String defaultlang = MCRConfiguration.instance().getString("MCR.Metadata.DefaultLang", "de");

    private static MCRCache INDEX_CACHE = new MCRCache(100, "Cache for already created indexes");

    private static MCRConfiguration MCR_CONFIG = MCRConfiguration.instance();

    /*
     * indexCache is build like this:
     * 
     * --+ indexCache +---+ MCRObjectType +---+ Cache entries for configured
     * aliases
     */

    class MyRangeDelim {
        int pos;

        String value;

        String diff;

        MyRangeDelim(int pos, String value) {
            this.pos = pos;
            this.value = value;
        }
    }

    class MyBrowseData {
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

    class MyIndexConfiguration {
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

    private void initIndexCacheForObjectType(String alias) {
        String objectType = getObjectType(alias);
        if (INDEX_CACHE.get(objectType) == null)
            INDEX_CACHE.put(objectType, new MCRCache(1000, "Index cache for object type " + objectType));
    }

    private static String getObjectType(String alias) {
        // get object type belonging to alias
        String propKey = "MCR.IndexBrowser." + alias + ".Table";
        String objectType = MCR_CONFIG.getProperties("MCR.IndexBrowser.").getProperty(propKey);
        return objectType;
    }

    private MCRCache getIndexCache(String alias) {
        String objectType = getObjectType(alias);
        return ((MCRCache) INDEX_CACHE.get(objectType));
    }

    protected static void deleteIndexCacheOfObjectType(String objectType) {
    	for(Object o: INDEX_CACHE.keys()){
    		if(o.toString().contains(objectType)){
    			INDEX_CACHE.remove(o);
    		}
    	}
    }

    public MCRIndexBrowserData(String search, String mode, String path, String fromTo, String mask) {

        browseData.set(search, mode, path, fromTo, mask);
        indexConfig.set(browseData.index);
        Element results = buildPageElement();
        int numRows = 0;
        String cacheKey = browseData.index + "##" + browseData.search + "##" + mode;

        if (cached(path, cacheKey)) {
            MCRCache cache = ((MCRCache) getIndexCache(path).get(getCacheKey(cacheKey)));
            linkedList1 = (LinkedList<String[]>) (cache.get(INDEX_KEY));
            numRows = linkedList1.size();
            myQuery = (MCRQuery) cache.get(QUERY_KEY);
        } else {
            initIndexCacheForObjectType(path);
            MCRCache cacheNew = new MCRCache(5, "IndexBrowser Cache key(" + getCacheKey(cacheKey) + ")");
            getIndexCache(path).put(getCacheKey(cacheKey), cacheNew);
            // cache = getCache(cacheKey);
            numRows = createLinkedListfromSearch();
            ((MCRCache) getIndexCache(path).get(getCacheKey(cacheKey))).put(INDEX_KEY, linkedList1);
            ((MCRCache) getIndexCache(path).get(getCacheKey(cacheKey))).put(QUERY_KEY, myQuery);
            // for further search and research (by refine and other posibilities
            // the query must be in the Cache
            new MCRCachedQueryData(mcrResult, myQuery.buildXML(), myQuery.getCondition());
            results.setAttribute("resultid", mcrResult.getID());
        }
        // resort it for german ...
        // sortLinkedListForGerman();
        int from = Math.max(0, browseData.from);
        int to = Math.min(numRows, browseData.to + 1);
        int numSelectedRows = to - from;

        results.setAttribute("numHits", String.valueOf(numRows));
        if (browseData.search != null) {
            results.setAttribute("search", browseData.search);
        }

        if (numSelectedRows <= indexConfig.maxPerPage) {
            // set the metadata for the details only for the shown objects
            fillLinkedListWithMetadata(from, to);

            // build the xml-result tree with detailed Informations
            for (int i = from; i < to; i++) {
                String myValues[] = linkedList1.get(i);

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
                String myValues[] = linkedList1.get(i);
                delims.add(new MyRangeDelim(i, myValues[1]));

                i = Math.min(i + stepSize, to - 1);

                String myVal[] = linkedList1.get(i);
                String value = myVal[1];

                if ((i < to) && ((to - i) < 3)) {
                    i = to - 1;
                    value = linkedList1.get(i)[1];
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
        logger.debug("Results: \n" + out.outputString(page));
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

    // **************************************************************************

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

    // **************************************************************************

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

    // **************************************************************************

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

    // **************************************************************************

    private MCRQuery buildQuery() {
        MCRAndCondition cAnd = new MCRAndCondition();

        MCRFieldDef field = MCRFieldDef.getDef("objectType");

        if (indexConfig.table.indexOf(",") != -1) {
            MCROrCondition cOr = new MCROrCondition();
            StringTokenizer st = new StringTokenizer(indexConfig.table, ",");
            while (st.hasMoreTokens()) {
                cOr.addChild(new MCRQueryCondition(field, "=", st.nextToken()));
            }
            cAnd.addChild(cOr);
        } else {
            cAnd.addChild(new MCRQueryCondition(field, "=", indexConfig.table));
        }

        field = MCRFieldDef.getDef(indexConfig.browseField);
        // String value = br.search == null ? "*" : br.search + "*";
        String value = browseData.search == null ? "*" : browseData.search;
        String operator = getOperator();

        cAnd.addChild(new MCRQueryCondition(field, operator, value));

        boolean order = "ascending".equalsIgnoreCase(indexConfig.order);
        List<MCRSortBy> sortCriteria = new ArrayList<MCRSortBy>();

        for (int i = 0; indexConfig.sortFields != null && i < indexConfig.sortFields.length; i++) {
            field = MCRFieldDef.getDef(indexConfig.sortFields[i]);
            if (null != field)
                sortCriteria.add(new MCRSortBy(field, order));
            else
                logger.error("MCRFieldDef not available: " + indexConfig.sortFields[i]);
        }

        myQuery = new MCRQuery(cAnd, sortCriteria, 0);
        logger.debug("Query: \n" + out.outputString(myQuery.buildXML()));
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

    // **************************************************************************

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
        linkedList1 = new LinkedList<String[]>();
        mcrResult = MCRQueryManager.search(myQuery);
        logger.debug("Results found hits:" + mcrResult.getNumHits());
        Document jResult = new Document(mcrResult.buildXML());
        List<?> mcrHit = jResult.getRootElement().getChildren();

        if (mcrHit != null) {
            int numRows = mcrHit.size();
            for (int i = 0; i < numRows; i++) {
                // for faster results we take the first 2 fields from the mcrhit
                // they should be set with an searchfield with
                // type="identifier", to get the exact String
                int len = 0;
                if (indexConfig.outputFields != null)
                    len = indexConfig.outputFields.length;
                String[] listelm = new String[len + 3];
                Element child = (Element) (mcrHit.get(i));
                logger.debug("\n" + out.outputString(child));

                List<?> searchfields = child.getChild("sortData", MCRFieldDef.mcrns).getChildren("field", MCRFieldDef.mcrns);
                listelm[0] = ((Element) searchfields.get(0)).getValue();
                if (searchfields.size() > 1)
                    listelm[1] = ((Element) searchfields.get(1)).getValue();
                else
                    listelm[1] = listelm[0];

                listelm[2] = child.getAttributeValue("id");
                linkedList1.add(listelm);
            }
        }
        return linkedList1.size();
    }

    private int fillLinkedListWithMetadata(int from, int to) {
        if (from == 0)
            from = 1;
        // now we take only the objects from metadata for the dataobjects that
        // we will shown in detail
        // this occurres if we have only a small count of results, or we show
        // only a part
        if (to - from <= indexConfig.maxPerPage && indexConfig.outputFields != null) {
            for (int i = from - 1; i < to; i++) {
                String[] listelm = linkedList1.get(i);
                Document od = new Document();
                MCRObject oo = new MCRObject();
                try {
                    od = oo.receiveJDOMFromDatastore(listelm[2]);
                } catch (Exception yy) {
                    // object dos'nt exist in Database????
                    continue;
                }
                // outputfields came only from metadataobject
                setListeElm(od, indexConfig.outputFields, 3, listelm, false);
                linkedList1.set(i, listelm);
            }
        }
        return linkedList1.size();
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
        String cacheKey = getCacheKey(key);
        if ((((MCRCache) INDEX_CACHE.get(objType)) != null) && ((MCRCache) INDEX_CACHE.get(objType)).get(cacheKey) != null)
            return true;
        else
            return false;
    }

    public static void removeAllCachesStartsWithKey(String key) {
        String key2 = getCacheKey(key);
        Iterator iC = MCRSessionMgr.getCurrentSession().getObjectsKeyList();
        // indexCache.
        while (iC.hasNext()) {
            String nextKey = (String) iC.next();
            if (nextKey.startsWith(key2)) {
                logger.debug("Remove IndexBrowserCache with KEY" + nextKey);
                ((MCRCache) MCRSessionMgr.getCurrentSession().get(nextKey)).remove(INDEX_KEY);
                ((MCRCache) MCRSessionMgr.getCurrentSession().get(nextKey)).remove(QUERY_KEY);
            }
        }
    }

    private static String getCacheKey(String key) {
        return PREFIX + key;
    }
}
