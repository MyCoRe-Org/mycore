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

package mycore.datamodel;

import java.io.*;
import java.util.*;
import javax.servlet.http.*;
import javax.servlet.*;
import javax.xml.transform.*;
import javax.xml.transform.stream.*;
import org.jdom.transform.*;
import org.jdom.*;
import mycore.common.*;
import mycore.classifications.*;
import mycore.xml.MCRLayoutServlet;

/**
 * This servlet provides a web interface to query
 * the datastore using XQueries and deliver the result list
 *
 * @author Frank Lützenkirchen
 * @author Jens Kupferschmidt
 * @author Mathias Hegner
 * @version $Revision$ $Date$
*/
public class MCRQueryServlet extends HttpServlet 
{
// The configuration
private MCRConfiguration conf = null;

// Default Language (as UpperCase)
private String defaultLang = "";

 /**
  * The initialization method for this servlet. This read the default
  * language from the configuration.
  **/
  public void init() throws MCRConfigurationException
    {
    conf = MCRConfiguration.instance();
    String defaultLang = conf
      .getString( "MCR.metadata_default_lang", "en" ).toUpperCase();
    }

 /**
  * This method handles HTTP GET requests and resolves them to output.
  *
  * @param request the HTTP request instance
  * @param response the HTTP response instance
  * @exception IOException for java I/O errors.
  * @exception ServletException for errors from the servlet engine.
  **/
  public void doGet( HttpServletRequest  request, 
                     HttpServletResponse response )
    throws IOException, ServletException
  {  

    boolean cachedFlag = false;
    HttpSession session = null;
    org.jdom.Document jdom = null;

    String mode  = request.getParameter( "mode"  );
    String query = request.getParameter( "query" );
    String type  = request.getParameter( "type"  );
    String host  = request.getParameter( "hosts" );
    String lang  = request.getParameter( "lang"  );
    String sort  = request.getParameter( "sort"  );

    String att_mode  = (String) request.getAttribute( "mode"  );
    if (att_mode!=null) { mode = att_mode; }
    String att_query = (String) request.getAttribute( "query" );
    if (att_query!=null) { query = att_query; }
    String att_type  = (String) request.getAttribute( "type"  );
    if (att_type!=null) { type = att_type; }
    String att_host  = (String) request.getAttribute( "hosts" );
    if (att_host!=null) { host = att_host; }
    String att_lang  = (String) request.getAttribute( "lang"  );
    if (att_lang!=null) { lang = att_lang; }
    String att_sort  = (String) request.getAttribute( "sort"  );
    if (att_sort != null) { sort = att_sort; }

    if( mode  == null ) mode  = "ResultList";
    if( host  == null ) host  = "local";
    if( query == null ) query = "";
    if( type  == null ) return;
    if (!conf.getBoolean("MCR.type_"+type.toLowerCase(),false)) { return; }
    if( lang  == null ) { lang  = defaultLang; }
    if (lang.equals("")) { lang = defaultLang; }
    lang = lang.toUpperCase();
    if (sort == null) sort = "";
    sort = sort.trim();

    System.out.println("MCRQueryServlet : mode = "+mode);
    System.out.println("MCRQueryServlet : type = "+type);
    System.out.println("MCRQueryServlet : hosts = "+host);
    System.out.println("MCRQueryServlet : lang = "+lang);
    System.out.println("MCRQueryServlet : sort = " + sort);
    System.out.println("MCRQueryServlet : query = "+query);

    // query for classifications
    if (type.toLowerCase().equals("class")) {
      Properties parameters = MCRLayoutServlet.buildXSLParameters( request );
      String style = parameters.getProperty("Style",mode+"-class-"+lang);
      System.out.println("Style = "+style);
      MCRClassification cl = new MCRClassification();
      jdom = cl.search(query);
      if (jdom==null) {
        throw new MCRException( 
          "No classification or category exists" ); }
//System.out.println(new String(MCRUtils.getByteArray(jdom)));
      try {
        if (style.equals("xml")) {
          response.setContentType( "text/xml" );
          OutputStream out = response.getOutputStream();
          new org.jdom.output.XMLOutputter( "  ", true ).output( jdom, out );
          out.close();
          }
        else {
          request.setAttribute( "MCRLayoutServlet.Input.JDOM",  jdom  );
          request.setAttribute( "XSL.Style", style );
          RequestDispatcher rd = getServletContext()
            .getNamedDispatcher( "MCRLayoutServlet" );
          rd.forward( request, response );
          }
        }
      catch( Exception ex ) {
        System.out.println( ex.getClass().getName() );
        System.out.println( ex ); 
        }
      return;
      }

    // all other document types
    if (mode.equals("CachedResultList"))
    {
      cachedFlag = true;
      mode = "ResultList";
    }

    if (mode.equals("ResultList"))
      session = request.getSession(false);

    if (cachedFlag)
    {
      // retrieve result list from session cache
      try
      {
        if (session != null)
        {
          jdom = sortList((org.jdom.Document) session.getAttribute( "CachedList" ), sort);
          type = (String)            session.getAttribute( "CachedType" );
        }
        else
          System.out.println("session for getAttribute is null");
        if (jdom == null)
          System.out.println("jdom could not be retrieved from session cache");
        if (type == null)
          System.out.println("type could not be retrieved from session cache");
      }
      catch (Exception exc)
      {
        System.out.println(exc.getClass().getName());
        System.out.println(exc);
      }
    }

    // prepare the stylesheet name
    Properties parameters = MCRLayoutServlet.buildXSLParameters( request );
    String style = parameters.getProperty("Style",mode+"-"+type+"-"+lang);
    System.out.println("Style = "+style);

    if (! cachedFlag)
    {
      MCRQueryResult result = new MCRQueryResult();
      MCRQueryResultArray resarray = result.setFromQuery(host, type, query );

      jdom = resarray.exportAllToDocument();

      // create a new session if not already alive and encache result list
      if (mode.equals("ResultList"))
      {
        jdom = sortList(jdom, sort);

        if (session == null)
          session = request.getSession(true);
        if (session != null)
        {
          session.setAttribute( "CachedList", jdom );
          session.setAttribute( "CachedType", type );
        }
        else
          System.out.println("session for setAttribute is null");
      }
    }

    // append previous and next documents in list if they exist :
    if (mode.equals("ObjectMetadata"))
      if (session != null)
    {
      org.jdom.Document jdom_list = (org.jdom.Document) session.getAttribute("CachedList");
      jdom_list = sortList(jdom_list, sort);
      String xml_list = (new org.jdom.output.XMLOutputter()) .outputString(jdom_list);
      String xml_doc = (new org.jdom.output.XMLOutputter()) .outputString(jdom);
      int xml_doc_start = xml_doc.indexOf("<mcr_result ");
      int xml_doc_end = xml_doc.indexOf("</mcr_results>");
      String xml_doc_res = xml_doc.substring(xml_doc_start, xml_doc_end);
      int pos_xml_doc = xml_list.indexOf(xml_doc_res);
      if (pos_xml_doc > 0)
      {
        int prev_start = xml_list.lastIndexOf("<mcr_result ", pos_xml_doc - 3);
        int next_end = xml_list.indexOf("</mcr_result>", pos_xml_doc + xml_doc_res.length()) + 13;
        String xml_pn = xml_doc.substring(0, xml_doc_end);
        if (prev_start > 0) xml_pn += xml_list.substring(prev_start, pos_xml_doc);
        else xml_pn += "<mcr_result/>";
        if (next_end > 0) xml_pn += xml_list.substring(pos_xml_doc + xml_doc_res.length(), next_end);
        else xml_pn += "<mcr_result/>";
        xml_pn += "</mcr_results>";
System.out.println("-------------------------- xml_pn start ----------------------------------");
System.out.println(xml_pn);
System.out.println("-------------------------- xml_pn end ------------------------------------");
        try { jdom = (new org.jdom.input.SAXBuilder()) .build(new ByteArrayInputStream(xml_pn.getBytes())); }
        catch (org.jdom.JDOMException exc) { System.out.println(exc); }
      }
    }

    try {
      if (style.equals("xml")) {
        response.setContentType( "text/xml" );
        OutputStream out = response.getOutputStream();
        new org.jdom.output.XMLOutputter( "  ", true ).output( jdom, out );
        out.close();
        }
      else {
        request.setAttribute( "MCRLayoutServlet.Input.JDOM",  jdom  );
        request.setAttribute( "XSL.Style", style );
        RequestDispatcher rd = getServletContext()
          .getNamedDispatcher( "MCRLayoutServlet" );
        rd.forward( request, response );
        }
      }
    catch( Exception ex ) {
      System.out.println( ex.getClass().getName() );
      System.out.println( ex );
    }
  }

  /**
   * <em>transform</em> transforms a JDOM via XSLT giving a result JDOM.
   *
   * @param inp                              the input list as JDOM
   * @param stylesheet                       the transforming stylesheet
   * @return org.jdom.Document               the result
   * @exception MCRException                 thrown if XSLT transformation fails
   */
  private static org.jdom.Document transform (org.jdom.Document inp, String stylesheet)
    throws MCRException
  {
    try
    {
      Transformer transformer = TransformerFactory.newInstance().
        newTransformer(new StreamSource(stylesheet));
      JDOMResult out = new JDOMResult();
      transformer.transform(new JDOMSource(inp), out);
      return out.getDocument();
    }
    catch (TransformerException exc)
    {
      throw new MCRException("Could not transform JDOM via XSLT", exc);
    }
  }

  /**
   * <em>buildSortingStylesheet</em> creates an XSL stylesheet which can sort a list
   * of documents according to a given attribute in ascending order.
   *
   * @param attr                          the name of the attribute for which the list
   *                                      should be sorted
   * @return String                       the resulting stylesheet
   */
  private static String buildSortingStylesheet (String attr)
  {
    String stylesheet = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n\n";

    stylesheet += "<!-- This file is machine generated and can be safely removed -->\n\n";

    stylesheet += "<xsl:stylesheet version=\"1.0\"\n" +
      "  xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\">\n\n";

    stylesheet += "<xsl:output\n" +
      "  method=\"xml\"\n" +
  //    "  encoding=\"ISO-8859-1\"\n" +
  //    "  media-type=\"text/xml\"\n" +
  //    "  doctype-public=\"\"\n" +
      "/>\n\n";

    stylesheet += "<xsl:template match=\"/mcr_results\">\n\n";

    stylesheet += "<xsl:element name=\"mcr_results\">\n\n";

    stylesheet += "<xsl:for-each select=\"//mcr_result\">\n" +
      "  <xsl:sort order=\"ascending\" select=\"mycoreobject/metadata/" +
      attr + "s/" + attr + "/text()\"/>\n" +
      "  <xsl:copy-of select=\".\"/>\n" +
      "</xsl:for-each>\n\n";

    stylesheet += "</xsl:element>\n\n";

    stylesheet += "</xsl:template>\n\n";

    stylesheet += "</xsl:stylesheet>\n";

    return stylesheet;
  }

  /**
   * <em>sortList</em> takes a list of documents and sorts them according to a given attribute.
   *
   * @param inp                           the input list as a JDOM
   * @param attr                          the sorting attribute
   * @return org.jdom.Document            the sorted list as a JDOM
   * @exception MCRException              thrown if JDOM could not be transformed via XSLT
   */
  private static synchronized org.jdom.Document sortList (org.jdom.Document inp, String attr)
  {
    if (attr.equals("")) return inp; // no sort if no attribute is specified
    String stylesheet = buildSortingStylesheet(attr);
    try
    {
      BufferedWriter wr = new BufferedWriter(new FileWriter("machine-generated-sort.xsl"));
      wr.write(stylesheet);
      wr.close();
      return transform(inp, "machine-generated-sort.xsl");
    }
    catch (Exception exc)
    {
      System.out.println("Exception during sort list in MCRQueryServlet : " + exc.getMessage());
      return inp;
    }
  }

  /**
   * <em>getLinkFolder</em> retrieves all links outgoing from a single input document
   * and belonging to a given link folder and gives the result as a JDOM.
   *
   * @param doc                              the single input object as JDOM
   * @param folder                           the name of the link folder without ending "s"
   * @return org.jdom.Document               the result JDOM
   */
  private static org.jdom.Document getLinkFolder (org.jdom.Document doc, String folder)
  {
    return doc;
  }
}

