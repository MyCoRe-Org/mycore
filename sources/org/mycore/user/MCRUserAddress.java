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

package mycore.user;

import java.io.*;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Instances of this class store address information of MyCoRe users. MCRUserAddress
 * is part of the MyCoRe user component and must not be used directly by other components.
 * MCRUserAddress is aggregated by MCRUser and all user objects are managed by the
 * user manager (the instance of the singleton MCRUserMgr), which is the only class of
 * the MyCoRe user component that other components should use.
 *
 * @see mycore.user.MCRUserMgr
 * @see mycore.user.MCRUser
 *
 * @author Detlev Degenhardt
 * @version $Revision$ $Date$
 */
public class MCRUserAddress
{
  private String salutation = "";
  private String firstname = "";
  private String lastname = "";
  private String street = "";
  private String city = "";
  private String postalcode = "";
  private String country = "";
  private String institution = "";
  private String telephone = "";
  private String fax = "";
  private String email = "";
  private String cellphone = "";

  /**
   * All address attributes of a user are taken from a DOM NodeList which
   * must be passed as a parameter.
   *
   * @param addressElements a DOM NodeList containing the address attributes of a user
   */
  public MCRUserAddress(NodeList addressElements)
  {
    salutation  = MCRXmlHelper.getElementText("salutation", addressElements);
    firstname   = MCRXmlHelper.getElementText("firstname", addressElements);
    lastname    = MCRXmlHelper.getElementText("lastname", addressElements);
    street      = MCRXmlHelper.getElementText("street", addressElements);
    city        = MCRXmlHelper.getElementText("city", addressElements);
    postalcode  = MCRXmlHelper.getElementText("postalcode", addressElements);
    country     = MCRXmlHelper.getElementText("country", addressElements);
    institution = MCRXmlHelper.getElementText("institution", addressElements);
    telephone   = MCRXmlHelper.getElementText("telephone", addressElements);
    fax         = MCRXmlHelper.getElementText("fax", addressElements);
    email       = MCRXmlHelper.getElementText("email", addressElements);
    cellphone   = MCRXmlHelper.getElementText("cellphone", addressElements);
  }

  /**
   * This method returns the user address information as an XML-Stream. This output
   * is used by the corresponding user object to create a full XML representation
   * of the user.
   *
   * @param NL   newline character, should be "\n" if you want newlines behind
   *             every tag or "" if you want the XML-stream as one line
   */
  public String getAddressAsXmlElement(String NL)
  {
    StringBuffer sb = new StringBuffer();
    sb.append("<address>").append(NL)
      .append("<salutation>" ).append(salutation ).append("</salutation>" ).append(NL)
      .append("<firstname>"  ).append(firstname  ).append("</firstname>"  ).append(NL)
      .append("<lastname>"   ).append(lastname   ).append("</lastname>"   ).append(NL)
      .append("<street>"     ).append(street     ).append("</street>"     ).append(NL)
      .append("<city>"       ).append(city       ).append("</city>"       ).append(NL)
      .append("<postalcode>" ).append(postalcode ).append("</postalcode>" ).append(NL)
      .append("<country>"    ).append(country    ).append("</country>"    ).append(NL)
      .append("<institution>").append(institution).append("</institution>").append(NL)
      .append("<telephone>"  ).append(telephone  ).append("</telephone>"  ).append(NL)
      .append("<fax>"        ).append(fax        ).append("</fax>"        ).append(NL)
      .append("<email>"      ).append(email      ).append("</email>"      ).append(NL)
      .append("<cellphone>"  ).append(cellphone  ).append("</cellphone>"  ).append(NL)
      .append("</address>");
    return sb.toString();
  }

  /**
   * This method overrides the <code>toString()</code> method inherited from
   * <code>java.lang.Object</code>. All address attributes of a user are returned
   * as a string (in table formatted output for "nice" listings).
   *
   * @return returns the address information of the user in a table formatted
   *         output, all in one string
   */
  public String toString()
  {
    StringBuffer sb = new StringBuffer();
    sb.append("salutation    : ").append(salutation ).append("\n");
    sb.append("firstname     : ").append(firstname  ).append("\n");
    sb.append("lastname      : ").append(lastname   ).append("\n");
    sb.append("street        : ").append(street     ).append("\n");
    sb.append("city          : ").append(city       ).append("\n");
    sb.append("postalcode    : ").append(postalcode ).append("\n");
    sb.append("country       : ").append(country    ).append("\n");
    sb.append("institution   : ").append(institution).append("\n");
    sb.append("telephone     : ").append(telephone  ).append("\n");
    sb.append("fax           : ").append(fax        ).append("\n");
    sb.append("email         : ").append(email      ).append("\n");
    sb.append("cellphone     : ").append(cellphone  );
    return sb.toString();
  }

  /**
   * This <code>toString()</code> method returns all address information in
   * one string. The various attributes are separated by a separator string, which
   * must be provided as a parameter.
   *
   * @param separator String sequence for separating the various address attributes
   */
  public String toString(String separator)
  {
    StringBuffer sb = new StringBuffer();
    sb.append(salutation ).append(separator)
      .append(firstname  ).append(separator)
      .append(lastname   ).append(separator)
      .append(street     ).append(separator)
      .append(city       ).append(separator)
      .append(postalcode ).append(separator)
      .append(country    ).append(separator)
      .append(institution).append(separator)
      .append(telephone  ).append(separator)
      .append(fax        ).append(separator)
      .append(email      ).append(separator)
      .append(cellphone  ).append(separator);
    return sb.toString();
  }
}

