/*
 * $RCSfile$
 * $Revision$ $Date$
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
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
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */

package org.mycore.backend.lucene;

import java.io.StringReader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenStream;
import org.mycore.common.MCRConfiguration;
import java.util.Map;
import java.util.HashMap;

/**
 * Use Lucene Analyzer to normalize strings
 * 
 * @author Harald Richter
 * 
 * @version $Revision$ $Date$
 * 
 */
public class MCRLuceneTools {
    MCRConfiguration config = MCRConfiguration.instance();
    private static Map<String,Analyzer> analyzerMap = new HashMap<String,Analyzer>();

    /**
     * Use Lucene Analyzer to normalize strings
     * 
     * @param value
     *            string to convert
     * @param ID
     *            The classes that do the normalization come from the lucene package
     *            and are configured by the property
     *            <tt>MCR.Lucene.Analyzer.<ID>.Class</tt> in mycore.properties.
     * 
     * @return the normalized string
     */
    public static String luceneNormalize(String value, String ID) throws Exception {
      Analyzer analyzer = analyzerMap.get( ID );
      if (null == analyzer)
      {
        analyzer = (Analyzer)MCRConfiguration.instance().getInstanceOf("MCR.Lucene.Analyzer." + ID + ".Class");
        analyzerMap.put(ID, analyzer);
      }
      
      StringBuffer sb = new StringBuffer();

      TokenStream ts = analyzer.tokenStream(null, new StringReader(value));
      Token to;

      while ((to = ts.next()) != null) 
      {
        if ( sb.length() > 0)
          sb.append(" ");
        sb.append(to.termText());
      }

      return sb.toString();
    }

}
