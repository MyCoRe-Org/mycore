package org.mycore.frontend.indexbrowser;

import java.text.CollationKey;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.filter.ElementFilter;
import org.jdom.output.XMLOutputter;
import org.mycore.common.MCRConfiguration;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.parsers.bool.MCRAndCondition;
import org.mycore.parsers.bool.MCROrCondition;
import org.mycore.services.fieldquery.MCRFieldDef;
import org.mycore.services.fieldquery.MCRQuery;
import org.mycore.services.fieldquery.MCRQueryCondition;
import org.mycore.services.fieldquery.MCRQueryManager;
import org.mycore.services.fieldquery.MCRResults;
import org.mycore.services.fieldquery.MCRSearchServlet;
import org.mycore.services.fieldquery.MCRSortBy;

public class MCRIndexBrowserData {
    protected static Logger logger = Logger.getLogger(MCRIndexBrowserData.class);

    private static final long serialVersionUID = 1L;

    // private static MCRCache ll1Cache = new MCRCache(500);
    private static LinkedList ll1 = new LinkedList();

    private MyBrowseData br = new MyBrowseData();

    private MyIndexConfiguration ic = new MyIndexConfiguration();

    private XMLOutputter out = new XMLOutputter(org.jdom.output.Format.getPrettyFormat());

    private Element page;

    private MCRResults mcrResult;

    private MCRQuery myQuery;

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

        int to = Integer.MAX_VALUE;

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

    public MCRIndexBrowserData(String search, String mode, String path, String fromTo, String mask) {

        br.set(search, mode, path, fromTo, mask);
        ic.set(br.index);
        Element results = buildPageElement();

        // first crate all listitems
        int numRows = createLinkedListfromSearch();

        // resort it for german ...
        sortLinkedListForGerman();
        int from = Math.max(0, br.from);
        int to = Math.min(numRows, br.to);
        int numSelectedRows = to - from;

        results.setAttribute("numHits", String.valueOf(numRows));
        if (br.search != null) {
            results.setAttribute("search", br.search);
        }

        if (numSelectedRows <= ic.maxPerPage) {
            // set the metadata for the details only for the shown objects
            fillLinkedListWithMetadata(from, to);

            // for further search and research
            MCRSearchServlet.getCache(MCRSearchServlet.getResultsKey()).put(mcrResult.getID(), mcrResult);
            MCRSearchServlet.getCache(MCRSearchServlet.getQueriesKey()).put(mcrResult.getID(), getQuery());
            MCRSearchServlet.getCache(MCRSearchServlet.getConditionsKey()).put(mcrResult.getID(), myQuery.getCondition());
            results.setAttribute("resultid", mcrResult.getID());

            // build the xml-result tree with detailed Informations
            for (int i = from; i < to; i++) {
                String myValues[] = (String[]) ll1.get(i);

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

                for (int j = 0; j < ic.outputFields.length; j++) {
                    String sField = ic.outputFields[j];
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
            int stepSize = calculateStepSize(numSelectedRows, ic.maxPerPage);
            List delims = new Vector();

            for (int i = from; i < to; i++) {
                String myValues[] = (String[]) ll1.get(i);
                delims.add(new MyRangeDelim(i, myValues[1]));

                i = Math.min(i + stepSize, to - 1);

                String myVal[] = (String[]) ll1.get(i);
                String value = myVal[1];

                if ((i < to) && ((to - i) < 3)) {
                    i = to - 1;
                    value = ((String[]) ll1.get(i))[1];
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
        if (br.mask != null) {
            jQuery.getRootElement().setAttribute("mask", br.mask);
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
        page.setAttribute("path", br.path.toString());

        Element eIndex = new Element("index");
        page.addContent(eIndex);

        eIndex.setAttribute("id", br.index);
        Element results = new Element("results");
        page.addContent(results);
        return results;
    }

    // **************************************************************************

    private void buildPrefixDifference(List delims) {
        for (int i = 0; i < delims.size(); i++) {
            MyRangeDelim curr = (MyRangeDelim) (delims.get(i));
            MyRangeDelim prev = (MyRangeDelim) (delims.get(Math.max(0, i - 1)));
            MyRangeDelim next = (MyRangeDelim) (delims.get(Math.min(i + 1, delims.size() - 1)));

            String vCurr = curr.value;
            String vPrev = (i > 0 ? prev.value : "");
            String vNext = (i < delims.size() - 1 ? next.value : "");

            String a = buildPrefixDifference(vCurr, vPrev);
            String b = buildPrefixDifference(vCurr, vNext);
            curr.diff = (a.length() > b.length() ? a : b);
        }
    }

    // **************************************************************************

    private void buildXML(Element results, List delims) {
        for (int i = 0; i < delims.size(); i += 2) {
            MyRangeDelim start = (MyRangeDelim) (delims.get(i));
            MyRangeDelim end = (MyRangeDelim) (delims.get(i + 1));

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

        if (ic.table.indexOf(",") != -1) {
            MCROrCondition cOr = new MCROrCondition();
            StringTokenizer st = new StringTokenizer(ic.table, ",");
            while (st.hasMoreTokens()) {
                cOr.addChild(new MCRQueryCondition(field, "=", st.nextToken()));
            }
            cAnd.addChild(cOr);
        } else {
            cAnd.addChild(new MCRQueryCondition(field, "=", ic.table));
        }

        field = MCRFieldDef.getDef(ic.browseField);
        String value = br.search == null ? "*" : br.search + "*";
        cAnd.addChild(new MCRQueryCondition(field, "like", value));

        boolean order = "ascending".equalsIgnoreCase(ic.order);
        List sortCriteria = new ArrayList();

        for (int i = 0; ic.sortFields != null && i < ic.sortFields.length; i++) {
            field = MCRFieldDef.getDef(ic.sortFields[i]);
            if (null != field)
                sortCriteria.add(new MCRSortBy(field, order));
            else
                logger.error("MCRFieldDef not available: " + ic.sortFields[i]);
        }

        myQuery = new MCRQuery(cAnd, sortCriteria, 0);
        logger.debug("Query: \n" + out.outputString(myQuery.buildXML()));
        return myQuery;
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
        ll1 = new LinkedList();
        mcrResult = MCRQueryManager.search(myQuery);
        logger.debug("Results found hits:" + mcrResult.getNumHits());
        Document jResult = new Document(mcrResult.buildXML());
        List mcrHit = jResult.getRootElement().getChildren();

        if (mcrHit != null) {
            int numRows = mcrHit.size();
            for (int i = 0; i < numRows; i++) {
                // for faster results we take the first 2 fields from the mcrhit
                // they should be set with an searchfield with
                // type="identifier", to get the exact String
                String[] listelm = new String[ic.outputFields.length + 3];
                Element child = (Element) (mcrHit.get(i));
                logger.debug("\n" + out.outputString(child));

                Element searchfield = child.getChild("sortData", MCRFieldDef.mcrns).getChild("field", MCRFieldDef.mcrns);
                listelm[0] = searchfield.getValue();
                listelm[1] = searchfield.getValue();
                listelm[2] = child.getAttributeValue("id");
                ll1.add(listelm);
            }
        }
        return ll1.size();
    }

    private int fillLinkedListWithMetadata(int from, int to) {
        if (from == 0)
            from = 1;
        // now we take only the objects from metadata for the dataobjects that
        // we will shown in detail
        // this occurres if we have only a small count of results, or we show
        // only a part
        if (to - from <= ic.maxPerPage) {
            for (int i = from - 1; i < to; i++) {
                String[] listelm = (String[]) ll1.get(i);

                Document od = new Document();
                MCRObject oo = new MCRObject();
                try {
                    od = oo.receiveJDOMFromDatastore(listelm[2]);
                } catch (Exception yy) {
                    // object dos'nt exist in Database????
                    continue;
                }
                // outputfields came only from metadataobject
                if (ic.outputFields != null) {
                    setListeElm(od, ic.outputFields, 3, listelm, false);
                }
                ll1.set(i, listelm);
            }
        }
        return ll1.size();
    }

    // reads the values from the metadata of the document
    private void setListeElm(Document od, String[] Fields, int startindex, String[] listelm, boolean append) {
        if (Fields == null)
            return;
        for (int j = 0; j < Fields.length; j++) {
            String value = "";
            String attribute = "";
            String sField = Fields[j];

            // Eintrag der ID
            if (sField.equals("id")) {
                value = od.getRootElement().getAttributeValue("ID");

            } else {
                Iterator it = od.getDescendants(new ElementFilter(sField));
                // only the atribute different texts are taken!!
                while (it.hasNext()) {
                    Element el = (Element) it.next();
                    if (attribute != el.getAttributeValue("type")) {
                        if (value.length() > 0) {
                            value += " - ";
                        }
                        value += el.getText();
                        attribute = el.getAttributeValue("type");
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

    private void sortLinkedListForGerman() {
        /***********************************************************************
         * lucene has no no correct sort, use this lines for a german sort
         **********************************************************************/
        Collections.sort(ll1, new Comparator() {
            public int compare(Object o1, Object o2) {
                Collator germanCc = Collator.getInstance(Locale.GERMAN);

                String[] name1 = (String[]) o1;
                String[] name2 = (String[]) o2;

                // int cc = name1[0].compareTo(name2[0]);
                // System.out.println("NAME1:" + name2[0] + "NAME2:" + name1[0]+
                // "ERG: " + cc);

                CollationKey key1 = germanCc.getCollationKey(name1[0]);
                CollationKey key2 = germanCc.getCollationKey(name2[0]);

                int comp = key1.compareTo(key2);
                // System.out.println("NAME1:" + name2[0] + "NAME2:" + name1[0]+
                // "ERG: " + comp);
                // System.out.println("___________________________");
                return comp;
            }
        });

        return;
    }

}
