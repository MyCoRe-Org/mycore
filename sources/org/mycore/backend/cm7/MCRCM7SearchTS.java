/**
 * $RCSfile$
 * $Revision$ $Date$
 *
 * This file is part of ** M y C o R e **
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
 *
 **/
 
package org.mycore.backend.cm7;

import com.ibm.mm.sdk.server.*;
import com.ibm.mm.sdk.common.*;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.xml.MCRXMLContainer;

/**
 * This is the search class for the IBM Content Manager 7 Text Search Engine.
 *
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 **/
public class MCRCM7SearchTS implements DKConstant
{
// defaults
public final int MAX_RESULTS = 1000;
public final int SEARCH_LANG = DK_LANG_DEU;
public final int CCSID_LATIN_1 = 819; 

// variables
private int mcr_maxresults;
private int mcr_searchlang;
private int mcr_ccsid;
private String mcr_indexclass;
private String mcr_tsserver;
private String mcr_tsindex;
private String mcr_fieldid;
private int mcr_partid;
private MCRXMLContainer mcr_result;

/**
 * The constructor.<br>
 * The defaults was set to:
 * <ul>
 * <li>MAX_RESULTS for the maximum of results.
 * <li>The configuration value of <em>MCR.persistence_cm7_textsearch_lang</em>
 * for the language of the search.
 * <li>The configuration value of <em>MCR.persistence_cm7_textsearch_ccsid</em>
 * for the enconding type.
 * <li>The configuration value of <em>MCR.persistence_cm7_textsearch_server</em>
 * for the text search server.
 * <li>The configuration value of <em>MCR.persistence_cm7_field_id</em> for
 * the index class field name of the MCRObjectId.
 * <li> An empty string for the index class and the text search index name.
 * This must be set befor you can search!
 * </ul>
 **/
public MCRCM7SearchTS()
  {
  MCRConfiguration conf = MCRConfiguration.instance();
  mcr_maxresults = conf.getInt("MCR.persistence_cm7_textsearch_maxresult",
    MAX_RESULTS);
  setSearchLang(conf.getString("MCR.persistence_cm7_textsearch_lang","DEU"));
  mcr_ccsid = conf.getInt("MCR.persistence_cm7_textsearch_ccsid",
    CCSID_LATIN_1);
  mcr_indexclass = "";
  mcr_tsserver = conf.getString("MCR.persistence_cm7_textsearch_server");
  mcr_tsindex = "";
  mcr_fieldid = conf.getString("MCR.persistence_cm7_field_id");
  mcr_partid = conf.getInt("MCR.persistence_cm7_part_xml",2);
  mcr_result = new MCRXMLContainer();
  }

/**
 * This methode return the answer list of a query.
 *
 * @return the MCRXMLContainer
 **/
public final MCRXMLContainer getResult()
  { return mcr_result; }

/**
 * This method sets the maximum of results for the query.
 *
 * @param maxresults            the maximum of results
 **/
public final void setMaxResults(int maxresults) 
  {
  if (maxresults<0) { return; }
  if (maxresults>MAX_RESULTS) { mcr_maxresults = MAX_RESULTS; return; }
  mcr_maxresults = maxresults;
  }

/**
 * This method sets the search language.
 *
 * @param lang                  the CM7 language string
 **/
public final void setSearchLang(String lang) 
  {
  if ((lang == null) || ((lang = lang.trim()).length() ==0)) { 
    mcr_searchlang = DK_LANG_DEU; return; }
  lang.toUpperCase();
  if (lang.equals("DEU")) { mcr_searchlang = DK_LANG_DEU; return; }
  if (lang.equals("ENG")) { mcr_searchlang = DK_LANG_ENG; return; }
  if (lang.equals("FRA")) { mcr_searchlang = DK_LANG_FRA; return; }
  mcr_searchlang = DK_LANG_DEU;
  }

/**
 * This method sets the index class.
 *
 * @param index                  the index class
 **/
public final void setIndexClass(String index) 
  {
  if ((index == null) || ((index = index.trim()).length() ==0)) {
    throw new MCRPersistenceException("The index class name is empty.");
    }
  mcr_indexclass = index;
  }

/**
 * This method sets the text search index.
 *
 * @param index                 the text search index
 **/
public final void setIndexTS(String index) 
  {
  if ((index == null) || ((index = index.trim()).length() ==0)) {
    throw new MCRPersistenceException("The text search index name is empty.");
    }
  mcr_tsindex = index;
  }

/**
 * This methode starts the query and store the result in a MCRXMLContainer.
 *
 * @param cond                 the query string for CM7 text search
 **/
public final void search(String cond) throws Exception, DKException
  {
  // if parameters not set 
  if (mcr_tsindex == null) { return ; }
  if (mcr_indexclass == null) { return ; }
  DKDatastoreDL connection = null;
  DKDatastoreTS dsTS = null;
  try {
    // search
    dsTS = new DKDatastoreTS();
    dsTS.connect(mcr_tsserver,"",' ');
    dsTS.setOption(DK_OPT_TS_LANG,(Object) new Integer(mcr_searchlang));
    dsTS.setOption(DK_OPT_TS_CCSID,(Object) new Integer(mcr_ccsid));
    StringBuffer sb = new StringBuffer(1024);
    sb.append("SEARCH=(COND=").append(cond).append(");");
    sb.append("OPTION=(SEARCH_INDEX=").append(mcr_tsindex).append(';');
    sb.append("MAX_RESULTS=").append(mcr_maxresults).append(')');
    DKNVPair parms[] = null;
    dkQuery pQry = dsTS.createQuery(sb.toString(),DK_TEXT_QL_TYPE,parms);
    pQry.execute(parms);
    DKResults pResult = (DKResults)pQry.result();
    dkIterator iter = ((dkCollection)pResult).createIterator();
    // prepare the vector
    connection = MCRCM7ConnectionPool.instance().getConnection();
    String id = "";
    byte[] xml = null;
    String itemId = "";
    while (iter.more()) {
      DKDDO mcr_item = (DKDDO)iter.next();
      if (mcr_item != null) {
        itemId = (String)mcr_item.getDataByName("DKDLItemId");
//      Integer rank = (Integer)mcr_item.getDataByName("DKRank") ;
        Integer rank = new Integer(1);
        MCRCM7Item my_item = new MCRCM7Item(connection,mcr_indexclass,itemId);
        my_item.retrieve();
        id = my_item.getKeyfieldToString(mcr_fieldid);
        xml = my_item.getPartToBytes(mcr_partid);
        mcr_result.add("local",id,rank.intValue(),xml);
        }
      }
    }
  finally { 
    MCRCM7ConnectionPool.instance().releaseConnection( connection );
    if (dsTS != null) { dsTS.disconnect(); } }
  }

}

