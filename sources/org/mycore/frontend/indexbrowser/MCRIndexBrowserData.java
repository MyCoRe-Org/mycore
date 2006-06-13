package org.mycore.frontend.indexbrowser;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.filter.ElementFilter;
import org.jdom.output.XMLOutputter;

import org.mycore.access.MCRAccessManager;
import org.mycore.common.MCRConfiguration;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.parsers.bool.MCRAndCondition;
import org.mycore.services.fieldquery.MCRFieldDef;
import org.mycore.services.fieldquery.MCRQuery;
import org.mycore.services.fieldquery.MCRQueryCondition;
import org.mycore.services.fieldquery.MCRQueryManager;
import org.mycore.services.fieldquery.MCRResults;
import org.mycore.services.fieldquery.MCRSortBy;

public class MCRIndexBrowserData {
	protected static Logger logger = Logger.getLogger(MCRIndexBrowserData.class);
	private static final long serialVersionUID = 1L;
	private String selectedtype = "";
    private LinkedList ll1 = new LinkedList(); 
    private MyBrowseData br = new MyBrowseData();
    private MyIndexConfiguration ic = new MyIndexConfiguration(); 
	private XMLOutputter out = new XMLOutputter(org.jdom.output.Format.getPrettyFormat());
    private Element page;
    private Document jResult;
    private Document jQuery;
    private MCRResults mcrResult;
    
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

        public void set (  	String search, String mode, String index, String fromTo ) {            	
            	this.search=search;
            	this.mode = mode;            	
            	this.index = index;
            	this.path = new StringBuffer(this.index);
            	this.path.append("/");
            	if ( fromTo != null && fromTo.length() > 0 ){
            		String from = fromTo.substring(0,fromTo.indexOf("-"));
            		String to = fromTo.substring(fromTo.indexOf("-")+1);
                	this.addRange(from, to);
            	}
                
                

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
        String[] extraFields;
        String fieldToSort;
        String[] sortFields;
        String order;
        String filter;
        String style;
        int maxPerPage;
        
        public void set (String ID) {

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
            extraFields = buildFieldList(fields);
            sortFields  = buildFieldList(fieldToSort);
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

    
    public MCRIndexBrowserData(String search, String mode, String path, String fromTo) {
    	
    	br.set(search, mode, path, fromTo);
        ic.set(br.index);
        
        Element results = buildPageElement();
        
		int numRows = fillmyLinkedList( );
		results.setAttribute("numHits", String.valueOf(numRows) );
		
        if (br.search != null) {
            results.setAttribute("search", br.search);
        }
        int from = Math.max(0, br.from);
        int to = Math.min(numRows, br.to);
        int numSelectedRows = to - from;

        if (numSelectedRows <= ic.maxPerPage) {
           for (int i = from; i < to; i++) {
               String myValues[] = (String[])this.ll1.get(i);
                    
               Element v = new Element("value");
               v.setAttribute("pos", String.valueOf(i));

               Element es = new Element("sort");
               es.addContent(myValues[0]);                       
               v.addContent(es);

               Element ev = new Element("idx");
               ev.addContent(myValues[1]);                   
               v.addContent(ev);
			   
               for (int j = 0; ic.extraFields!= null && j < ic.extraFields.length; j++) {
                  String sField= ic.extraFields[j];
                  Element col = new Element("col");
                  col.setAttribute("name", sField);
                  col.addContent(myValues[2 + j]);
                  v.addContent(col);
               }
               results.addContent(v);
            }
	    }  else {
            int stepSize = calculateStepSize(numSelectedRows,ic.maxPerPage);                
            List delims = new Vector();
            
            for (int i = from; i < to; i++) {              
            	String myValues[] = (String[])this.ll1.get(i);
            	delims.add(new MyRangeDelim(i, myValues[1]));
            	
            	i = Math.min(i + stepSize, to-1);
            	
            	String myVal[] = (String[])this.ll1.get(i);
            	String value = myVal[1];
            	           	
            	if ((i < to) && ((to - i) < 3)) {
            		i = to-1;                        
            		value =((String[])this.ll1.get(i))[1];
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
    
    
    private void setListeElm(Element child, String[] Fields, int startindex, String[] listelm, boolean append){
        if ( Fields == null) return;
        for (int j = 0; j < Fields.length; j++) {
        	String   value = "";
    	    String   sField= Fields[j];
    	    
    	    /* mcrHit - 
	    	 * <mcrhit id="docportal_author_000000000441">
	    	 * ...
			 * </mcrhit>
	    	 */
    	    
    	    
    	    //* über die ID die darzustellenden Daten holen 
        	Element  child1 = (Element) child.clone();
    	    
            // Eintrag der ID
        	if ( sField.equals("id")) {
        		value = child1.getAttributeValue("id");
        		
        	} else {
        	    MCRObject oo = new MCRObject();
        	    Document od =  oo.receiveJDOMFromDatastore(child1.getAttributeValue("id"));
        	    Iterator it = od.getDescendants(new ElementFilter(sField));
        	    if ( it.hasNext() )  {
        	      Element el = (Element) it.next();
                  value = el.getText();
        	    }  	
        	}        		
       		if ( append) {
                if ( j==0)  listelm[startindex]  = value;
                else    	listelm[startindex] +=", "+ value;
       		}
            else listelm[startindex + j] = value;
        }

	}
    
    //**************************************************************************

    private void buildPrefixDifference(List delims) {
        for (int i = 0; i < delims.size(); i++) {
            MyRangeDelim curr = (MyRangeDelim) (delims.get(i));
            MyRangeDelim prev = (MyRangeDelim) (delims.get(Math.max(0, i - 1)));
            MyRangeDelim next = (MyRangeDelim) (delims.get(Math.min(i + 1,  delims.size() - 1)));

            String vCurr = curr.value;
            String vPrev = (i > 0 ? prev.value : "");
            String vNext = (i < delims.size() - 1 ? next.value : "");

            String a = buildPrefixDifference(vCurr, vPrev);
            String b = buildPrefixDifference(vCurr, vNext);
            curr.diff = (a.length() > b.length() ? a : b);
        }
    }

    //**************************************************************************

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
   

    //**************************************************************************

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

    //**************************************************************************

    private void  buildQuery() {
        MCRAndCondition cAnd = new MCRAndCondition();

        MCRFieldDef field = MCRFieldDef.getDef("objectType");
        cAnd.addChild(new MCRQueryCondition(field,"=",ic.table));

        field = MCRFieldDef.getDef(ic.browseField);
        String value = br.search == null ? "*" : br.search + "*";
        cAnd.addChild(new MCRQueryCondition(field,"like",value));
    	
        logger.debug("generated query: \n" + cAnd );            

        boolean order = "ascending".equalsIgnoreCase(ic.order); 
        List sortCriteria = new ArrayList();
        sortCriteria.add(new MCRSortBy(field,order));
        
    	for ( int i=0; ic.extraFields != null && i< ic.extraFields.length; i++){    	
	        field = MCRFieldDef.getDef(ic.extraFields[i]);
            if (null != field )
              sortCriteria.add(new MCRSortBy(field,order));
            else
              logger.error("MCRFieldDef not available: " + ic.extraFields[i]);
    	}
        
        MCRQuery query = new MCRQuery(cAnd,sortCriteria,0);
    	mcrResult = MCRQueryManager.search(query);
    	
    	/**
    	 * <mcrresults sorted="true">
			  <mcrhit mcrid="atlibri_professorum_000000000441">
			    <sortData>
			      <data name="title">Aepinus</data>
			    </sortData>
			  </mcrhit>
			  <mcrhit mcrid="atlibri_document_000000000001">
			    <sortData>
			      <data name="title">Haupttitel</data>
			    </sortData>
			  </mcrhit>
			  <mcrhit mcrid="atlibri_document_000000000002">
			    <sortData>
			      <data name="title">Artikel Test Editor Servlet</data>
			    </sortData>
			  </mcrhit>
			...  
    	 */
    	logger.debug("Results found hits:" + mcrResult.getNumHits());    	    	
    	jResult = new Document(mcrResult.buildXML());
    	
    }
    
    //**************************************************************************

    private int calculateStepSize(int numSelectedRows, int maxPerPage) {
        for (int i = 1;; i++) {
            double dNum = numSelectedRows;
            double dI = 1.0 / ((double) i);
            double root = Math.pow(dNum, dI);
            if (root <= maxPerPage)
                return (int) (Math.floor(dNum / root));
        }
    }
    
    private int fillmyLinkedList() {
	    
    	if (ic.table.equals(this.selectedtype))
    	    return this.ll1.size();
    	else this.ll1.clear();

    	buildQuery();
    	
	    List mcrHit = jResult.getRootElement().getChildren();
	    /**
    	 * <mcrresults sorted="true">
			  <mcrhit id="atlibri_document_000000000002">
			    <sortData>
			      <data name="title">Artikel Test Editor Servlet</data>
			    </sortData>
			  </mcrhit>
			...  
    	 */
	    if ( mcrHit != null ) {
	        int numRows = mcrHit.size();

	    	// List elemente[sortfield,browsefield,extrafields1, extrafield2, ... extrafieldn]
            for (int i = 0; i< numRows; i++) {
            	
                String[] listelm = new String[2 + ( (ic.extraFields !=null)?ic.extraFields.length:0 ) ];                
            	Element child = (Element)(mcrHit.get( i ));   
            	
            	if ( MCRAccessManager.checkPermission( child.getAttributeValue("id"),"read") ){
	            	String[] brFields = new String[1];
	            	brFields[0]=  ic.sortFields[0];
	            	           	
	            	if ( ic.sortFields != null ) 
	            			setListeElm(child, ic.sortFields ,0, listelm, true);
	            	
	           		setListeElm(child, brFields, 1, listelm, false);           		
	           		if (ic.extraFields !=null)
	           			setListeElm(child, ic.extraFields, 2, listelm, false);
	           		
	                this.ll1.add(listelm);
            	}
            }            
	    }
	    return this.ll1.size();
    }            
    
}
