/**
 * * This file is part of ** M y C o R e **
 * Visit our homepage at http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, normally in the file license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */

package org.mycore.backend.lucene;

import java.io.File;
import java.io.StringReader;
import java.util.List;
import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.mycore.services.fieldquery.MCRHit;
import org.mycore.services.fieldquery.MCRResults;
import org.mycore.common.MCRConfiguration;
import org.mycore.parsers.bool.MCRCondition;
import org.mycore.parsers.bool.MCRConditionVisitor;
import org.mycore.services.fieldquery.MCRQueryParser;
import org.xml.sax.InputSource;

/**
 * Helper class for generating lucene query from mycore query
 * 
 * @author Harald Richter
 *
 */
public class MCRLuceneQuery implements MCRConditionVisitor{

    /** The logger */
    public static Logger LOGGER = Logger.getLogger(MCRLuceneQuery.class.getName());

  	private String IndexDir = "";
  	
    private Query luceneQuery;

    private MCRCondition cond;                //query-condition

    private int maxResults = 200;
   
    /**
     * initialise query with xml-document containing complete query
     * @param document xml query docuement
     */
    public MCRLuceneQuery(Element document){
        try{
            init(document);
        }catch(Exception e){
            LOGGER.error(e);
        }
    }
    
    
    /**
     * initialise query with xml-string containing complete query
     * @param xmlString
     */
    public MCRLuceneQuery(String xmlString){
        try{
            SAXBuilder builder = new SAXBuilder();
            init(builder.build(new InputSource(new StringReader(xmlString))).getRootElement());
        }catch(Exception e){
            LOGGER.error(e);
        }
    }
    
    public MCRLuceneQuery( MCRCondition cond, int maxResults, String IndexDir )
    {
      try
      {
        this.cond = cond;
        this.maxResults = maxResults;
        this.IndexDir = IndexDir;
      
        List f = new ArrayList();
        f.add( cond.toXML() );
      
        boolean reqf = true;    // required flag Term with AND (true) or OR (false) combined
        luceneQuery = MCRBuildLuceneQuery.buildLuceneQuery(null, reqf, f);
        LOGGER.debug("Lucene Query: " + luceneQuery.toString() );
      }
      catch(Exception e)
      {
        e.printStackTrace();
      }
    }
    
    /**
     * fill internal fields with query and build lucene query
     * @param doc document with xml query
     */
    private void init(Element root){
        try{
            IndexDir = MCRConfiguration.instance().getString("MCR.Searcher.lucenem.IndexDir");
            LOGGER.info("MCR.Searcher.lucenem.IndexDir: " + IndexDir);
//            org.jdom.Element root = querydoc.getRootElement();
            cond = new MCRQueryParser().parse( (Element)root.getChild("conditions").getChildren().get(0) );
//            cond.accept(this);
            maxResults   = Integer.parseInt(root.getAttributeValue("maxResults", "200"));
            LOGGER.debug("maxResults " + maxResults);
            Element x = cond.toXML();
            org.jdom.output.XMLOutputter outputter = new org.jdom.output.XMLOutputter();
            LOGGER.debug( outputter.outputString( x ) );
            LOGGER.debug( "input" + outputter.outputString( root ) );
            
            List f = root.getChild("conditions").getChildren();
            boolean reqf = true;    // required flag Term with AND (true) or OR (false) combined
            luceneQuery = MCRBuildLuceneQuery.buildLuceneQuery(null, reqf, f);
            LOGGER.debug("Lucene Query: " + luceneQuery.toString() );
       
        }catch(Exception e){
          e.printStackTrace();
            LOGGER.error(e);
        }
    }
    
    /**
     * method does lucene query 
     * @return result set
     */
    public MCRResults getLuceneHits() throws Exception {
      IndexSearcher searcher = new IndexSearcher(IndexDir);
//    Hits hits = searcher.search( luceneQuery );
//    int found = hits.length();
      TopDocs hits = searcher.search( luceneQuery, null, maxResults );
      int found = hits.scoreDocs.length;

      LOGGER.info("Number of Objects found : " + found);
      
      MCRResults result = new MCRResults();
      for (int i=0; i<found;i++)
      {
//        org.apache.lucene.document.Document doc    = hits.doc(i);
        org.apache.lucene.document.Document doc    = searcher.doc(hits.scoreDocs[i].doc); 
        String mcrtype  = doc.get("mcrtype");
        String id;
        if ( "f".equals(mcrtype) )     // MCRFile found
        {
          id = doc.get("FileID");
          LOGGER.debug("ID of MCRFile found: " + id );
          MCRHit hit = new MCRHit(id);
          hit.addSortData("type", "MCRFile");

          String key = "OwnnerID";
          String value = doc.get("OwnnerID");
          hit.addSortData(key, value);
          result.addHit(hit);
        }
        else                           // MCRObject found
        {
          id = doc.get("mcrid");
          LOGGER.debug("ID of MCRObject found: " + id );
  /*TODO        if (MCRAccessManager.checkReadAccess( id, MCRSessionMgr.getCurrentSession()))*/{
            MCRHit hit = new MCRHit( id );
            hit.addSortData("type", "MCRObject");
            
            // fill hit meta
//            for (int j=0; j<order.size(); j++){
                String key = "author";
                String value = doc.get("author");
                hit.addSortData(key,value);
//            }
            result.addHit(hit);
        } // MCRAccessManager
        }
      }
      searcher.close();
      return result;
    }
    
    /**
     * interface implementation (visitor pattern) for condition types:
     * on each new type a xml-element will be added to an internal stack which
     * holds the number of children to process
     */
    public void visitType(Element element) {   
    }
    
    /**
     * interface implementation (visitor pattern) for field type
     */
    public void visitQuery(MCRCondition entry) {
      }
    
    /**
     * method returns the string representation of given query
     */
    public String toString(){
      return  luceneQuery.toString();
    }

}
