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

package mycore.xml;

import java.io.*;
import java.util.Vector;
import org.w3c.dom.*;
import mycore.common.*;

/**
 * This class provides some static utility methods to deal with XML/DOM
 * elements, nodes etc. The class *must* be considered as "work in progress"!
 * There is plenty left to do - and finally it may make its way from the
 * package mycore.user to the package mycore.common.
 *
 * @author Detlev Degenhardt
 * @author Frank Lützenkirchen
 * @version $Revision$ $Date$
 */
public class MCRXMLHelper
{
  private static MCRParserInterface parser;

  /** Returns the XML Parser as configured in mycore.properties */
  private static MCRParserInterface getParser()
    throws MCRException
  {
    if( parser == null )
    {
      Object o = MCRConfiguration.instance().getInstanceOf( "MCR.parser_class_name" );
      parser = (MCRParserInterface)o;
    }
    return parser;
  }

 /**
  * Parses an XML file from a URI and returns it as DOM.
  * Use the validation value from mycore.properties.
  *
  * @param uri           the URI of the XML file
  * @throws MCRException if XML could not be parsed
  * @return              the XML file as a DOM object
  **/
  public static Document parseURI( String uri ) throws MCRException
  { return getParser().parseURI( uri ); }

 /**
  * Parses an XML file from a URI and returns it as DOM.
  * Use the given validation flag.
  *
  * @param uri           the URI of the XML file
  * @param valid         the validation flag
  * @throws MCRException if XML could not be parsed
  * @return              the XML file as a DOM object
  **/
  public static Document parseURI( String uri, boolean valid ) 
    throws MCRException
  { return getParser().parseURI( uri, valid ); }

 /**
  * Parses an XML String and returns it as DOM.
  * Use the validation value from mycore.properties.
  *
  * @param xml           the XML String to be parsed
  * @throws MCRException if XML could not be parsed
  * @return              the XML file as a DOM object
  **/
  public static Document parseXML( String xml ) throws MCRException
  { return getParser().parseXML( xml ); }

 /**
  * Parses an XML String and returns it as DOM.
  * Use the given validation flag.
  *
  * @param xml           the XML String to be parsed
  * @param valid         the validation flag
  * @throws MCRException if XML could not be parsed
  * @return              the XML file as a DOM object
  **/
  public static Document parseXML( String xml, boolean valid ) 
    throws MCRException
  { return getParser().parseXML( xml, valid ); }

 /**
  * Parses an Byte Array and returns it as DOM.
  * Use the validation value from mycore.properties.
  *
  * @param xml           the XML Byte Array to be parsed
  * @throws MCRException if XML could not be parsed
  * @return              the XML file as a DOM object
  **/
  public static Document parseXML( byte [] xml ) throws MCRException
  { return getParser().parseXML( xml ); }

 /**
  * Parses an Byte Array and returns it as DOM.
  * Use the given validation flag.
  *
  * @param xml           the XML Byte Array to be parsed
  * @param valid         the validation flag
  * @throws MCRException if XML could not be parsed
  * @return              the XML file as a DOM object
  **/
  public static Document parseXML( byte [] xml, boolean valid ) 
    throws MCRException
  { return getParser().parseXML( xml, valid ); }

  /**
   * This method prints out a node. It is meant only for debugging purposes
   * during the software development .
   *
   * @param node        the DOM-node to be printed
   * @param indent      indentation string for formatted output, e.g. " " (one space)
   */
  static public void printNode (Node node, String indent)
  {
    // Parts of this method are taken from Brett McLaughlin's Book "Java and XML" (O'Reilly).
    switch (node.getNodeType())
    {
      case Node.DOCUMENT_NODE:
           System.out.println("<xml version=\"1.0\">\n");
           Document doc = (Document)node;
           printNode(doc.getDocumentElement(), "");
           break;

      case Node.ELEMENT_NODE:
           String name = node.getNodeName();
           System.out.print("\n" + indent + "<" + name);
           NamedNodeMap attributes = node.getAttributes();
           for (int i=0; i<attributes.getLength(); i++)
           {
             Node current = attributes.item(i);
             System.out.print(" " + current.getNodeName() + "=\""
                                  + current.getNodeValue() + "\"");
           }
           System.out.print(">");
           NodeList children = node.getChildNodes();
           if (children != null)
           {
             for (int i=0; i<children.getLength(); i++)
               printNode(children.item(i), indent + "  ");
           }
           System.out.print("\n" + indent + "</" + name + ">");
           break;

      case Node.TEXT_NODE:
           if (node.getNodeValue().trim() != null)
             System.out.print(node.getNodeValue().trim());
           break;
     }
  }

  /**
   * This method returns the text string of a text element. A NodeList must be
   * given as a parameter and the nodes in this list *must* be leaf nodes, i.e.
   * no further child nodes will be considered.
   *
   * @param seekElementName  the name of the element for which the text value will be returned
   * @param elementList      we are looking for seekElementname in this NodeList
   * @return the text value of seekElementname or ""
   */
  static public final String getElementText(String seekElementName, NodeList elementList)
  {
    for (int i=0; i<elementList.getLength(); i++)
    {
      if (elementList.item(i).getNodeName().trim().equals(seekElementName))
        if (elementList.item(i).hasChildNodes())
          return elementList.item(i).getFirstChild().getNodeValue();
        else  // ok, there is no text data available
          return "";
    }
    return "";
  }

  /**
   * This method returns the text string of a text element. An Element must be
   * given as a parameter. No childs will be considered!
   *
   * @param seekElementName  the name of the element for which the text value will be returned
   * @param element          we are looking for seekElementname in this Element
   * @return the text value of seekElementname or ""
   */
  static public final String getElementText(String seekElementName, Element element)
  {
    NodeList list = element.getElementsByTagName(seekElementName);
    return getElementText(seekElementName, list);
  }

  /**
   * This method returns a Vector of strings containing the text elements of a NodeList,
   * respectively. The NodeList must be provided as a parameter and the nodes in this list
   * *must* be leaf nodes, i.e. no further child nodes will be considered. This method is
   * useful if your XML-file contains elements like the following example:
   * <pre>
   *   &lt;group&gt;admins&lt;/group&gt;
   *   &lt;group&gt;users&lt;/group&gt;
   *   &lt;user&gt;whoever&lt;/user&gt;
   *    ...
   * </pre>
   * If you need all the text values for the &lt;group&gt;-elements in a Vector of strings, this
   * method will give it to you.
   *
   * @param seekElementName  the name of the element for which the text elements will be returned
   * @param elementList      we are looking for seekElementname in this NodeList
   * @return returns a Vector of strings containing the text elements of a given NodeList
   */
  static public final Vector getAllElementTexts(String seekElementName, NodeList elementList)
  {
    Vector textVector = new Vector();
    for (int i=0; i<elementList.getLength(); i++)
    {
      if (elementList.item(i).getNodeName().trim().equals(seekElementName))
        if (elementList.item(i).hasChildNodes()) // is there a text value?
          textVector.add(elementList.item(i).getFirstChild().getNodeValue().trim());
    }
    return textVector;
  }

  /**
   * This method fills a StringBuffer with node information as XML string. It is
   * used e.g. for saving users or groups as XML-file
   *
   * @param node        the DOM-node to be printed
   * @param indent      indentation string for formatted output, e.g. " " (one space)
   * @param sb          StringBuffer to be filled by recursive calls
   */
  static public void getNodeAsString (Node node, String indent, StringBuffer sb)
  {
    switch (node.getNodeType())
    {
      case Node.DOCUMENT_NODE:
           sb.append("<xml version=\"1.0\">\n").append("\n");
           Document doc = (Document)node;
           getNodeAsString(doc.getDocumentElement(), "", sb);
           break;

      case Node.ELEMENT_NODE:
           String name = node.getNodeName();
           sb.append("\n" + indent + "<" + name);
           NamedNodeMap attributes = node.getAttributes();
           for (int i=0; i<attributes.getLength(); i++)
           {
             Node current = attributes.item(i);
             sb.append(" " + current.getNodeName() + "=\""
                           + current.getNodeValue() + "\"");
           }
           sb.append(">");
           NodeList children = node.getChildNodes();
           if (children != null)
           {
             for (int i=0; i<children.getLength(); i++)
               getNodeAsString(children.item(i), indent + "  ", sb);
           }
           sb.append("\n" + indent + "</" + name + ">");
           break;

      case Node.TEXT_NODE:
           if (node.getNodeValue().trim() != null)
             sb.append(node.getNodeValue().trim());
           break;
    }
  }

  /**
   * Convenience function to create a new JDOM-Element with some content
   */
  public static org.jdom.Element newJDOMElementWithContent( 
    String             elementName, 
    org.jdom.Namespace namespace, 
    String             content )
  {
    org.jdom.Element element = new org.jdom.Element( elementName, namespace );
    element.addContent( content );
    return element;
  }
}

